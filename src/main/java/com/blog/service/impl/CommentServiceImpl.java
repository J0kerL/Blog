package com.blog.service.impl;

import com.blog.common.BusinessException;
import com.blog.common.PageResult;
import com.blog.common.ResultCode;
import com.blog.dto.CommentCreateDTO;
import com.blog.entity.Comment;
import com.blog.entity.User;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.PostMapper;
import com.blog.mapper.UserMapper;
import com.blog.converter.EntityConverter;
import com.blog.service.CommentService;
import com.blog.vo.CommentVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final EntityConverter entityConverter;
    private final ChatClient chatClient;

    private static final int MAX_PAGE_SIZE = 100;
    private static final String AI_REVIEW_PROMPT = """
            你是一个内容审核助手。请判断以下评论是否包含脏话、恶俗语言、人身攻击、违法内容或垃圾广告。
            请只回答 PASS 或 REJECT，不要解释原因。
            - PASS：内容正常，无违规内容
            - REJECT：包含违规内容

            评论内容：
            %s
            """;

    @Override
    @Transactional
    public CommentVO create(Long userId, CommentCreateDTO dto) {
        Comment comment = new Comment();
        comment.setPostId(dto.getPostId());
        comment.setParentId(dto.getParentId());
        comment.setContent(dto.getContent());
        comment.setStatus(0); // 默认待审核

        if (userId != null) {
            User user = userMapper.findById(userId);
            comment.setUserId(userId);
            comment.setNickname(user != null ? user.getNickname() : "匿名用户");
        } else {
            comment.setNickname(dto.getNickname() != null ? dto.getNickname() : "匿名访客");
        }

        commentMapper.insert(comment);
        return enrichCommentVO(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentVO> listByPostId(Long postId) {
        // 1. 查询顶级评论（1 次 SQL）
        List<Comment> topLevel = commentMapper.findTopLevelByPostId(postId);
        if (topLevel.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 批量查询所有回复（1 次 SQL，替代 N 次）
        Set<Long> topLevelIds = topLevel.stream().map(Comment::getId).collect(Collectors.toSet());
        List<Comment> allReplies = commentMapper.findRepliesByPostId(postId);

        // 3. 收集需要查询用户信息的 ID
        Set<Long> userIds = new HashSet<>();
        topLevel.stream().map(Comment::getUserId).filter(Objects::nonNull).forEach(userIds::add);
        allReplies.stream().map(Comment::getUserId).filter(Objects::nonNull).forEach(userIds::add);

        // 4. 批量查询用户信息（1 次 SQL）
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.findByIds(new ArrayList<>(userIds)).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));

        // 5. 按 parentId 分组回复
        Map<Long, List<Comment>> replyMap = allReplies.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(Comment::getParentId));

        // 6. 先将所有回复转换并填充各自的 replies 字段（递归组装）
        Map<Long, CommentVO> voMap = new LinkedHashMap<>();
        for (Comment reply : allReplies) {
            CommentVO vo = enrichCommentVO(reply, userMap);
            vo.setReplies(replyMap.getOrDefault(reply.getId(), Collections.emptyList())
                    .stream()
                    .map(r -> enrichCommentVO(r, userMap))
                    .collect(Collectors.toList()));
            voMap.put(reply.getId(), vo);
        }
        // 重新给每个回复 VO 填充其子回复（二次遍历，使用 voMap 里的对象）
        for (Comment reply : allReplies) {
            CommentVO vo = voMap.get(reply.getId());
            List<Comment> children = replyMap.getOrDefault(reply.getId(), Collections.emptyList());
            vo.setReplies(children.stream()
                    .map(child -> voMap.get(child.getId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        }

        // 7. 顶级评论组装
        return topLevel.stream().map(topComment -> {
            CommentVO topVO = enrichCommentVO(topComment, userMap);
            List<Comment> replies = replyMap.getOrDefault(topComment.getId(), Collections.emptyList());
            topVO.setReplies(replies.stream()
                    .map(r -> voMap.get(r.getId()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
            return topVO;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CommentVO> listAdmin(int pageNum, int pageSize, Long postId, Integer status, String keyword) {
        pageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        PageHelper.startPage(pageNum, pageSize);
        List<Comment> comments = commentMapper.findAdminList(postId, status, keyword);
        PageInfo<Comment> pageInfo = new PageInfo<>(comments);
        List<CommentVO> voList = comments.stream().map(this::enrichAdminCommentVO).collect(Collectors.toList());
        return PageResult.of(pageInfo, voList);
    }

    @Override
    @Transactional
    public void approve(Long id) {
        if (commentMapper.findById(id) == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        commentMapper.updateStatus(id, 1);
    }

    @Override
    @Transactional
    public void reject(Long id) {
        if (commentMapper.findById(id) == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        commentMapper.updateStatus(id, 2);
    }

    @Override
    @Transactional
    public void aiReview(Long id) {
        Comment comment = commentMapper.findById(id);
        if (comment == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        boolean passed = doAiModeration(comment.getContent());
        commentMapper.updateStatus(id, passed ? 1 : 2);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (commentMapper.findById(id) == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        deleteRecursive(id, 0);
    }

    /**
     * 递归删除评论及其所有子评论，最大深度 5 层防止循环引用
     */
    private void deleteRecursive(Long id, int depth) {
        if (depth > 5) return;
        List<Long> childIds = commentMapper.findIdsByParentId(id);
        for (Long childId : childIds) {
            deleteRecursive(childId, depth + 1);
        }
        commentMapper.deleteById(id);
    }

    /**
     * 单个评论转换（无批量上下文时使用），前台展示用
     */
    private CommentVO enrichCommentVO(Comment comment) {
        CommentVO vo = entityConverter.toCommentVO(comment);
        if (comment.getUserId() != null) {
            User user = userMapper.findById(comment.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }
        }
        return vo;
    }

    /**
     * Admin 列表用：包含 status、postTitle
     */
    private CommentVO enrichAdminCommentVO(Comment comment) {
        CommentVO vo = entityConverter.toCommentVO(comment);
        vo.setStatus(comment.getStatus());
        if (comment.getUserId() != null) {
            User user = userMapper.findById(comment.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }
        }
        var post = postMapper.findById(comment.getPostId());
        if (post != null) {
            vo.setPostTitle(post.getTitle());
        }
        return vo;
    }

    /**
     * 批量上下文中的评论转换（使用预加载的用户 Map）
     */
    private CommentVO enrichCommentVO(Comment comment, Map<Long, User> userMap) {
        CommentVO vo = entityConverter.toCommentVO(comment);
        if (comment.getUserId() != null) {
            User user = userMap.get(comment.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }
        }
        return vo;
    }

    /**
     * AI 内容审核：调用 LLM 判断是否包含恶意内容
     */
    private boolean doAiModeration(String content) {
        try {
            String result = CompletableFuture.supplyAsync(() ->
                    chatClient.prompt()
                            .system("你是一个严格的内容审核助手，只回答PASS或REJECT。")
                            .user(String.format(AI_REVIEW_PROMPT, content))
                            .call()
                            .content()
            ).orTimeout(30, TimeUnit.SECONDS).join();
            log.info("AI 审核结果：{} -> {}", content.length() > 30 ? content.substring(0, 30) + "..." : content, result);
            return result != null && result.trim().toUpperCase().startsWith("PASS");
        } catch (Exception e) {
            log.error("AI 评论审核异常，默认通过", e);
            return true; // AI 异常时默认通过，避免误杀
        }
    }
}
