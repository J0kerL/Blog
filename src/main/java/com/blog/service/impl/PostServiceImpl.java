package com.blog.service.impl;

import com.blog.common.BusinessException;
import com.blog.common.PageResult;
import com.blog.common.ResultCode;
import com.blog.dto.PostCreateDTO;
import com.blog.entity.Post;
import com.blog.mapper.PostMapper;
import com.blog.service.*;
import com.blog.util.SlugUtil;
import com.blog.vo.PostListVO;
import com.blog.vo.PostVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章服务实现类
 *
 * <p>负责文章的 CRUD 操作和业务逻辑处理。通过注入其他服务来处理复杂的业务逻辑：</p>
 * <ul>
 *   <li>{@link PostCategoryService} - 分类标签关联处理</li>
 *   <li>{@link PostEnrichmentService} - 文章数据丰富化</li>
 *   <li>{@link AiSummaryService} - AI 摘要生成</li>
 *   <li>{@link ViewCountService} - 视图计数管理</li>
 * </ul>
 *
 * @author Diamond
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final PostCategoryService postCategoryService;
    private final PostEnrichmentService postEnrichmentService;
    private final AiSummaryService aiSummaryService;
    private final ViewCountService viewCountService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** 最大分页大小 */
    private static final int MAX_PAGE_SIZE = 50;
    
    /** 文章详情缓存 Key 前缀 */
    private static final String CACHE_KEY_PREFIX = "cache:post:detail:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);

    // ========== Admin 文章操作 ==========

    /**
     * 创建文章（Admin）
     */
    @Override
    @Transactional
    public PostVO create(Long userId, PostCreateDTO dto) {
        Post post = buildPostFromDTO(dto, userId);
        postMapper.insert(post);

        // 关联分类和标签
        associateCategoriesAndTags(post.getId(), dto);

        return getById(post.getId());
    }

    /**
     * 更新文章（Admin）
     */
    @Override
    @Transactional
    public PostVO update(Long postId, PostCreateDTO dto) {
        Post post = postMapper.findById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }

        Post updateParam = buildUpdateParam(postId, dto, post.getStatus());
        postMapper.update(updateParam);

        // 重新关联分类和标签
        postCategoryService.deleteCategoryAssociations(postId);
        postCategoryService.deleteTagAssociations(postId);
        associateCategoriesAndTags(postId, dto);

        // 清除文章缓存
        clearPostCache(postId);
        
        return getById(postId);
    }

    /**
     * 更新文章状态（Admin）
     */
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
        
        // 清除文章缓存
        clearPostCache(postId);
        
        return getById(postId);
    }

    /**
     * 删除文章（Admin）
     */
    @Override
    @Transactional
    public void delete(Long postId) {
        Post post = postMapper.findById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        postCategoryService.deleteCategoryAssociations(postId);
        postCategoryService.deleteTagAssociations(postId);
        postMapper.deleteById(postId);
        
        // 清除文章缓存
        clearPostCache(postId);
    }

    /**
     * 获取文章详情（Admin）
     */
    @Override
    @Transactional(readOnly = true)
    public PostVO getById(Long id) {
        // 尝试从缓存获取
        String cacheKey = CACHE_KEY_PREFIX + id;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, PostVO.class);
            } catch (JsonProcessingException e) {
                log.warn("反序列化文章缓存失败: id={}", id, e);
            }
        }
        
        // 从数据库查询
        Post post = postMapper.findById(id);
        if (post == null) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        PostVO result = postEnrichmentService.enrichPostVO(post);
        
        // 写入缓存
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
        } catch (JsonProcessingException e) {
            log.warn("序列化文章缓存失败: id={}", id, e);
        }
        
        return result;
    }

    /**
     * 获取文章详情（前台浏览，增加阅读量）
     */
    @Override
    @Transactional
    public PostVO getByIdForView(Long id) {
        Post post = postMapper.findById(id);
        if (post == null) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        // 使用 Redis 缓存增加阅读量
        viewCountService.incrementViewCount(id);
        post.setViewCount(viewCountService.getViewCount(id, post.getViewCount()));
        PostVO result = postEnrichmentService.enrichPostVO(post);
        
        // 更新缓存中的阅读量
        updateCachedViewCount(id, result.getViewCount());
        
        return result;
    }

    /**
     * 通过 Slug 获取文章详情（前台浏览，增加阅读量）
     */
    @Override
    @Transactional
    public PostVO getBySlug(String slug) {
        Post post = postMapper.findBySlug(slug);
        if (post == null) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        // 使用 Redis 缓存增加阅读量
        viewCountService.incrementViewCount(post.getId());
        post.setViewCount(viewCountService.getViewCount(post.getId(), post.getViewCount()));
        PostVO result = postEnrichmentService.enrichPostVO(post);
        
        // 更新缓存中的阅读量
        updateCachedViewCount(post.getId(), result.getViewCount());
        
        return result;
    }

    /**
     * 前台文章列表（分页、搜索、分类/标签过滤）
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<PostListVO> listPublished(int pageNum, int pageSize, String keyword, Long categoryId, Long tagId) {
        pageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        PageHelper.startPage(pageNum, pageSize);
        List<Post> posts = postMapper.findPublishedList(keyword, categoryId, tagId);
        PageInfo<Post> pageInfo = new PageInfo<>(posts);
        List<PostListVO> voList = postEnrichmentService.enrichPostListVO(posts);
        return PageResult.of(pageInfo, voList);
    }

    /**
     * Admin 文章列表（含全部状态）
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<PostListVO> listAdmin(int pageNum, int pageSize, String keyword, Integer status, Long categoryId) {
        pageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        PageHelper.startPage(pageNum, pageSize);
        List<Post> posts = postMapper.findAdminList(keyword, status, categoryId);
        PageInfo<Post> pageInfo = new PageInfo<>(posts);
        List<PostListVO> voList = postEnrichmentService.enrichPostListVO(posts);
        return PageResult.of(pageInfo, voList);
    }

    // ========== 用户文章操作 ==========

    /**
     * 用户创建文章
     */
    @Override
    @Transactional
    public PostVO createForUser(Long userId, PostCreateDTO dto) {
        return create(userId, dto);
    }

    /**
     * 用户更新文章
     */
    @Override
    @Transactional
    public PostVO updateForUser(Long userId, Long postId, PostCreateDTO dto) {
        checkOwnership(userId, postId);
        return update(postId, dto);
    }

    /**
     * 用户删除文章
     */
    @Override
    @Transactional
    public void deleteForUser(Long userId, Long postId) {
        checkOwnership(userId, postId);
        delete(postId);
    }

    /**
     * 用户更新文章状态
     */
    @Override
    @Transactional
    public PostVO updateStatusForUser(Long userId, Long postId, Integer status) {
        checkOwnership(userId, postId);
        return updateStatus(postId, status);
    }

    /**
     * 用户获取文章详情
     */
    @Override
    @Transactional(readOnly = true)
    public PostVO getByIdForUser(Long userId, Long postId) {
        checkOwnership(userId, postId);
        return getById(postId);
    }

    /**
     * 用户文章列表
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<PostListVO> listByUser(Long userId, int pageNum, int pageSize, String keyword, Integer status) {
        pageSize = Math.min(pageSize, MAX_PAGE_SIZE);
        PageHelper.startPage(pageNum, pageSize);
        List<Post> posts = postMapper.findByUserId(userId, keyword, status);
        PageInfo<Post> pageInfo = new PageInfo<>(posts);
        List<PostListVO> voList = postEnrichmentService.enrichPostListVO(posts);
        return PageResult.of(pageInfo, voList);
    }

    // ========== 私有辅助方法 ==========

    /**
     * 从 DTO 构建文章实体
     */
    private Post buildPostFromDTO(PostCreateDTO dto, Long userId) {
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
        if ((post.getSummary() == null || post.getSummary().isBlank())
                && dto.getContent() != null && !dto.getContent().isBlank()) {
            post.setSummary(aiSummaryService.generateSummary(dto.getTitle(), dto.getContent()));
        }

        // 自动生成 Slug
        if (post.getSlug() == null || post.getSlug().isBlank()) {
            post.setSlug(SlugUtil.generateSlug(dto.getTitle()));
        }

        // 设置发布时间
        if (dto.getStatus() != null && dto.getStatus() == 1) {
            post.setPublishedAt(LocalDateTime.now());
        }

        // 设置默认值
        if (post.getIsTop() == null) {
            post.setIsTop(0);
        }
        if (post.getAllowComment() == null) {
            post.setAllowComment(1);
        }

        return post;
    }

    /**
     * 构建更新参数
     */
    private Post buildUpdateParam(Long postId, PostCreateDTO dto, Integer originalStatus) {
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

        // AI 自动生成摘要
        if ((dto.getSummary() == null || dto.getSummary().isBlank())
                && dto.getContent() != null && !dto.getContent().isBlank()) {
            updateParam.setSummary(aiSummaryService.generateSummary(dto.getTitle(), dto.getContent()));
        }

        // 设置发布时间
        if (dto.getStatus() != null && originalStatus != 1 && dto.getStatus() == 1) {
            updateParam.setPublishedAt(LocalDateTime.now());
        }

        return updateParam;
    }

    /**
     * 关联分类和标签
     */
    private void associateCategoriesAndTags(Long postId, PostCreateDTO dto) {
        List<Long> categoryIds = postCategoryService.resolveCategoryIds(
                dto.getCategoryIds(), dto.getNewCategoryNames());
        List<Long> tagIds = postCategoryService.resolveTagIds(
                dto.getTagIds(), dto.getNewTagNames());

        postCategoryService.associateCategories(postId, categoryIds);
        postCategoryService.associateTags(postId, tagIds);
    }

    /**
     * 检查文章所有权
     */
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
     * 清除文章缓存
     */
    private void clearPostCache(Long postId) {
        String cacheKey = CACHE_KEY_PREFIX + postId;
        redisTemplate.delete(cacheKey);
        log.debug("文章缓存已清除: id={}", postId);
    }
    
    /**
     * 更新缓存中的阅读量
     */
    private void updateCachedViewCount(Long postId, Long viewCount) {
        String cacheKey = CACHE_KEY_PREFIX + postId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                PostVO cachedPost = objectMapper.readValue(cached, PostVO.class);
                cachedPost.setViewCount(viewCount);
                String json = objectMapper.writeValueAsString(cachedPost);
                redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
            } catch (Exception e) {
                log.warn("更新缓存阅读量失败: id={}", postId, e);
            }
        }
    }
}
