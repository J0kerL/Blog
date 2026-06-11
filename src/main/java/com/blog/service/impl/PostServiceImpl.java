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
import com.blog.mapper.convert.EntityConverter;
import com.blog.service.PostService;
import com.blog.util.SlugUtil;
import com.blog.vo.*;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final PostCategoryMapper postCategoryMapper;
    private final PostTagMapper postTagMapper;
    private final UserMapper userMapper;
    private final TagMapper tagMapper;
    private final EntityConverter entityConverter;

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

        if (dto.getCategoryIds() != null) {
            for (Long catId : dto.getCategoryIds()) {
                postCategoryMapper.insert(post.getId(), catId);
            }
        }

        if (dto.getTagIds() != null) {
            for (Long tagId : dto.getTagIds()) {
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
        postMapper.update(updateParam);

        if (dto.getCategoryIds() != null) {
            postCategoryMapper.deleteByPostId(postId);
            for (Long catId : dto.getCategoryIds()) {
                postCategoryMapper.insert(postId, catId);
            }
        }

        if (dto.getTagIds() != null) {
            postTagMapper.deleteByPostId(postId);
            for (Long tagId : dto.getTagIds()) {
                postTagMapper.insert(postId, tagId);
            }
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
}
