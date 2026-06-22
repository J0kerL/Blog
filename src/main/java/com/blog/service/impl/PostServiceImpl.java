package com.blog.service.impl;

import com.blog.common.BusinessException;
import com.blog.common.PageResult;
import com.blog.common.ResultCode;
import com.blog.dto.PostCreateDTO;
import com.blog.entity.Category;
import com.blog.entity.Post;
import com.blog.entity.Tag;
import com.blog.entity.User;
import com.blog.mapper.*;
import com.blog.converter.EntityConverter;
import com.blog.service.PostService;
import com.blog.util.SlugUtil;
import com.blog.vo.*;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final PostCategoryMapper postCategoryMapper;
    private final PostTagMapper postTagMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final EntityConverter entityConverter;
    private final ChatClient chatClient;

    private static final int MAX_PAGE_SIZE = 50;

    @Override
    @Transactional
    public PostVO create(Long userId, PostCreateDTO dto) {
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(dto.getTitle());
        post.setSlug(dto.getSlug());
        post.setSummary(dto.getSummary());
        post.setContent(dto.getContent());
        post.setCoverImage(dto.getCoverImage());
        post.setStatus(dto.getStatus());
        post.setIsTop(dto.getIsTop());
        post.setAllowComment(dto.getAllowComment());

        // AI 自动生成摘要
        if ((post.getSummary() == null || post.getSummary().isBlank()) && dto.getContent() != null && !dto.getContent().isBlank()) {
            post.setSummary(generateSummaryByAi(dto.getTitle(), dto.getContent()));
        }

        if (post.getSlug() == null || post.getSlug().isBlank()) {
            post.setSlug(SlugUtil.generateSlug(dto.getTitle()));
        }

        if (dto.getStatus() != null && dto.getStatus() == 1) {
            post.setPublishedAt(LocalDateTime.now());
        }

        if (post.getIsTop() == null) {
            post.setIsTop(0);
        }
        if (post.getAllowComment() == null) {
            post.setAllowComment(1);
        }

        postMapper.insert(post);

        // 解析新建分类/标签名称，自动创建并合并到 ID 列表
        List<Long> allCategoryIds = resolveCategoryIds(dto.getCategoryIds(), dto.getNewCategoryNames());
        List<Long> allTagIds = resolveTagIds(dto.getTagIds(), dto.getNewTagNames());

        if (!allCategoryIds.isEmpty()) {
            for (Long catId : allCategoryIds) {
                postCategoryMapper.insert(post.getId(), catId);
            }
        }

        if (!allTagIds.isEmpty()) {
            for (Long tagId : allTagIds) {
                postTagMapper.insert(post.getId(), tagId);
            }
        }

        return getById(post.getId());
    }

    @Override
    @Transactional
    public PostVO update(Long postId, PostCreateDTO dto) {
        Post post = postMapper.findById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }

        LocalDateTime publishedAt = null;
        if (dto.getStatus() != null && post.getStatus() != 1 && dto.getStatus() == 1) {
            publishedAt = LocalDateTime.now();
        }

        Post updateParam = new Post();
        updateParam.setId(postId);
        updateParam.setTitle(dto.getTitle());
        updateParam.setSlug(dto.getSlug());
        updateParam.setSummary(dto.getSummary());
        updateParam.setContent(dto.getContent());
        updateParam.setCoverImage(dto.getCoverImage());
        updateParam.setStatus(dto.getStatus());
        updateParam.setIsTop(dto.getIsTop());
        updateParam.setAllowComment(dto.getAllowComment());
        updateParam.setPublishedAt(publishedAt);

        // AI 自动生成摘要
        if ((dto.getSummary() == null || dto.getSummary().isBlank()) && dto.getContent() != null && !dto.getContent().isBlank()) {
            updateParam.setSummary(generateSummaryByAi(dto.getTitle(), dto.getContent()));
        }

        postMapper.update(updateParam);

        // 解析新建分类/标签名称，自动创建并合并到 ID 列表
        List<Long> allCategoryIds = resolveCategoryIds(dto.getCategoryIds(), dto.getNewCategoryNames());
        List<Long> allTagIds = resolveTagIds(dto.getTagIds(), dto.getNewTagNames());

        postCategoryMapper.deleteByPostId(postId);
        for (Long catId : allCategoryIds) {
            postCategoryMapper.insert(postId, catId);
        }

        postTagMapper.deleteByPostId(postId);
        for (Long tagId : allTagIds) {
            postTagMapper.insert(postId, tagId);
        }

        return getById(postId);
    }

    @Override
    @Transactional
    public PostVO updateStatus(Long postId, Integer status) {
        Post post = postMapper.findById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }

        Post updateParam = new Post();
        updateParam.setId(postId);
        updateParam.setStatus(status);

        // 从非发布状态变为已发布时，设置发布时间
        if (post.getStatus() != 1 && status == 1) {
            updateParam.setPublishedAt(LocalDateTime.now());
        }

        postMapper.update(updateParam);

        return getById(postId);
    }

    @Override
    @Transactional
    public void delete(Long postId) {
        Post post = postMapper.findById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        postCategoryMapper.deleteByPostId(postId);
        postTagMapper.deleteByPostId(postId);
        postMapper.deleteById(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public PostVO getById(Long id) {
        Post post = postMapper.findById(id);
        if (post == null) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        return enrichPostVO(post);
    }

    @Override
    @Transactional
    public PostVO getByIdForView(Long id) {
        Post post = postMapper.findById(id);
        if (post == null) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        postMapper.incrementViewCount(id);
        post.setViewCount(post.getViewCount() == null ? 1 : post.getViewCount() + 1);
        return enrichPostVO(post);
    }

    @Override
    @Transactional
    public PostVO getBySlug(String slug) {
        Post post = postMapper.findBySlug(slug);
        if (post == null) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        postMapper.incrementViewCount(post.getId());
        post.setViewCount(post.getViewCount() == null ? 1 : post.getViewCount() + 1);
        return enrichPostVO(post);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PostListVO> listPublished(int pageNum, int pageSize, String keyword, Long categoryId, Long tagId) {
        pageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        PageHelper.startPage(pageNum, pageSize);
        List<Post> posts = postMapper.findPublishedList(keyword, categoryId, tagId);
        PageInfo<Post> pageInfo = new PageInfo<>(posts);
        List<PostListVO> voList = enrichPostListVO(posts);
        return PageResult.of(pageInfo, voList);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PostListVO> listAdmin(int pageNum, int pageSize, String keyword, Integer status, Long categoryId) {
        pageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        PageHelper.startPage(pageNum, pageSize);
        List<Post> posts = postMapper.findAdminList(keyword, status, categoryId);
        PageInfo<Post> pageInfo = new PageInfo<>(posts);
        List<PostListVO> voList = enrichPostListVO(posts);
        return PageResult.of(pageInfo, voList);
    }

    /**
     * 单个 Post → PostVO（详情页，N 次查询可接受）
     */
    private PostVO enrichPostVO(Post post) {
        PostVO vo = entityConverter.toPostVO(post);
        vo.setAuthor(loadAuthor(post.getUserId()));
        vo.setCategories(loadCategories(post.getId()));
        vo.setTags(loadTags(post.getId()));
        return vo;
    }

    /**
     * 批量 Post → PostListVO（列表页，消除 N+1）
     * 将 N×3 次查询优化为 3 次批量查询 + 内存组装
     */
    private List<PostListVO> enrichPostListVO(List<Post> posts) {
        if (posts.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集所有需要查询的 ID
        Set<Long> userIds = posts.stream().map(Post::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toSet());

        // 批量查询作者（1 次 SQL）
        Map<Long, User> userMap = userMapper.findByIds(new ArrayList<>(userIds)).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 批量查询分类（1 次 SQL）
        Map<Long, List<Category>> categoryMap = postCategoryMapper.findCategoriesByPostIds(new ArrayList<>(postIds)).stream()
                .collect(Collectors.groupingBy(Category::getPostId));

        // 批量查询标签（1 次 SQL）
        Map<Long, List<Tag>> tagMap = tagMapper.findByPostIds(new ArrayList<>(postIds)).stream()
                .collect(Collectors.groupingBy(Tag::getPostId));

        // 内存组装
        return posts.stream().map(post -> {
            PostListVO vo = entityConverter.toPostListVO(post);

            User user = userMap.get(post.getUserId());
            if (user != null) {
                vo.setAuthor(entityConverter.toUserVO(user));
            }

            List<Category> categories = categoryMap.getOrDefault(post.getId(), Collections.emptyList());
            vo.setCategories(categories.stream().map(entityConverter::toCategoryVO).collect(Collectors.toList()));

            List<Tag> tags = tagMap.getOrDefault(post.getId(), Collections.emptyList());
            vo.setTags(tags.stream().map(entityConverter::toTagVO).collect(Collectors.toList()));

            return vo;
        }).collect(Collectors.toList());
    }

    private UserVO loadAuthor(Long userId) {
        if (userId == null) return null;
        User user = userMapper.findById(userId);
        return user != null ? entityConverter.toUserVO(user) : null;
    }

    private List<CategoryVO> loadCategories(Long postId) {
        return postCategoryMapper.findCategoriesByPostId(postId).stream()
                .map(entityConverter::toCategoryVO)
                .collect(Collectors.toList());
    }

    private List<TagVO> loadTags(Long postId) {
        return tagMapper.findByPostId(postId).stream()
                .map(entityConverter::toTagVO)
                .collect(Collectors.toList());
    }

    // ========== 用户文章操作 ==========

    @Override
    @Transactional
    public PostVO createForUser(Long userId, PostCreateDTO dto) {
        return create(userId, dto);
    }

    @Override
    @Transactional
    public PostVO updateForUser(Long userId, Long postId, PostCreateDTO dto) {
        checkOwnership(userId, postId);
        return update(postId, dto);
    }

    @Override
    @Transactional
    public void deleteForUser(Long userId, Long postId) {
        checkOwnership(userId, postId);
        delete(postId);
    }

    @Override
    @Transactional
    public PostVO updateStatusForUser(Long userId, Long postId, Integer status) {
        checkOwnership(userId, postId);
        return updateStatus(postId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public PostVO getByIdForUser(Long userId, Long postId) {
        checkOwnership(userId, postId);
        return getById(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PostListVO> listByUser(Long userId, int pageNum, int pageSize, String keyword, Integer status) {
        pageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        PageHelper.startPage(pageNum, pageSize);
        List<Post> posts = postMapper.findByUserId(userId, keyword, status);
        PageInfo<Post> pageInfo = new PageInfo<>(posts);
        List<PostListVO> voList = enrichPostListVO(posts);
        return PageResult.of(pageInfo, voList);
    }

    /**
     * 将已有的分类 ID 列表和新建分类名称列表合并，自动创建不存在的分类
     */
    private List<Long> resolveCategoryIds(List<Long> existingIds, List<String> newNames) {
        List<Long> result = new ArrayList<>();
        if (existingIds != null) {
            result.addAll(existingIds);
        }
        if (newNames != null) {
            for (String name : newNames) {
                String trimmed = name.trim();
                if (trimmed.isEmpty()) continue;
                Category existing = categoryMapper.findByName(trimmed);
                if (existing != null) {
                    if (!result.contains(existing.getId())) {
                        result.add(existing.getId());
                    }
                } else {
                    Category cat = new Category();
                    cat.setName(trimmed);
                    cat.setSlug(SlugUtil.generateSlug(trimmed));
                    cat.setSortOrder(0);
                    categoryMapper.insert(cat);
                    result.add(cat.getId());
                }
            }
        }
        return result;
    }

    /**
     * 将已有的标签 ID 列表和新建标签名称列表合并，自动创建不存在的标签
     */
    private List<Long> resolveTagIds(List<Long> existingIds, List<String> newNames) {
        List<Long> result = new ArrayList<>();
        if (existingIds != null) {
            result.addAll(existingIds);
        }
        if (newNames != null) {
            for (String name : newNames) {
                String trimmed = name.trim();
                if (trimmed.isEmpty()) continue;
                Tag existing = tagMapper.findByName(trimmed);
                if (existing != null) {
                    if (!result.contains(existing.getId())) {
                        result.add(existing.getId());
                    }
                } else {
                    Tag tag = new Tag();
                    tag.setName(trimmed);
                    tag.setSlug(SlugUtil.generateSlug(trimmed));
                    tagMapper.insert(tag);
                    result.add(tag.getId());
                }
            }
        }
        return result;
    }

    private void checkOwnership(Long userId, Long postId) {
        Post post = postMapper.findById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        if (!userId.equals(post.getUserId())) {
            throw new BusinessException(ResultCode.POST_NOT_OWNER);
        }
    }

    /**
     * 调用 AI 根据标题和内容自动生成摘要，失败时返回空字符串
     */
    private String generateSummaryByAi(String title, String content) {
        try {
            String truncatedContent = content.length() > 1000 ? content.substring(0, 1000) : content;
            String prompt = String.format(
                    "请根据以下博客文章信息，生成一段 120 字以内的中文摘要，简洁概括文章核心内容，不要包含开头“本文”字样：\n\n标题：%s\n内容片段：%s",
                    title != null ? title : "未提供",
                    truncatedContent
            );
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("AI 自动生成摘要失败，文章将使用空摘要: {}", e.getMessage());
            return "";
        }
    }
}
