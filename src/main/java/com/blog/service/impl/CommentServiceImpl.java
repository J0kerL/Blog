package com.blog.service.impl;

import com.blog.common.BusinessException;
import com.blog.common.PageResult;
import com.blog.common.ResultCode;
import com.blog.dto.CommentCreateDTO;
import com.blog.entity.Comment;
import com.blog.entity.User;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.UserMapper;
import com.blog.mapper.convert.EntityConverter;
import com.blog.service.CommentService;
import com.blog.vo.CommentVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final EntityConverter entityConverter;

    private static final int MAX_PAGE_SIZE = 100;

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
            comment.setEmail(dto.getEmail());
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

        // 6. 内存组装
        return topLevel.stream().map(topComment -> {
            CommentVO topVO = enrichCommentVO(topComment, userMap);
            List<Comment> replies = replyMap.getOrDefault(topComment.getId(), Collections.emptyList());
            topVO.setReplies(replies.stream()
                    .map(reply -> enrichCommentVO(reply, userMap))
                    .collect(Collectors.toList()));
            return topVO;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CommentVO> listAdmin(int pageNum, int pageSize, Long postId, Integer status) {
        pageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        PageHelper.startPage(pageNum, pageSize);
        List<Comment> comments = commentMapper.findAdminList(postId, status);
        PageInfo<Comment> pageInfo = new PageInfo<>(comments);
        List<CommentVO> voList = comments.stream().map(this::enrichCommentVO).collect(Collectors.toList());
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
    public void delete(Long id) {
        if (commentMapper.findById(id) == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        commentMapper.deleteById(id);
    }

    /**
     * 单个评论转换（无批量上下文时使用）
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
}
