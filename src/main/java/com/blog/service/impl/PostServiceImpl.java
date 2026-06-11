package com.blog.service.impl;

import com.blog.common.BusinessException;
import com.blog.common.PageResult;
import com.blog.common.ResultCode;
import com.blog.dto.PostCreateDTO;
import com.blog.entity.Category;
import com.blog.entity.Post;
import com.blog.entity.Tag;
import com.blog.mapper.*;
import com.blog.service.PostService;
import com.blog.util.SlugUtil;
import com.blog.vo.*;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final PostCategoryMapper postCategoryMapper;
    private final PostTagMapper postTagMapper;
    private final UserMapper userMapper;
    private final TagMapper tagMapper;

    @Override
    @Transactional
    public PostVO create(Long userId, PostCreateDTO dto) {
        Post post = new Post();
        BeanUtils.copyProperties(dto, post);
        post.setUserId(userId);

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

        // 关联分类
        if (dto.getCategoryIds() != null) {
            for (Long catId : dto.getCategoryIds()) {
                postCategoryMapper.insert(post.getId(), catId);
            }
        }

        // 关联标签
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

        // 状态变更：从非发布变为发布时，设置发布时间
        LocalDateTime publishedAt = null;
        if (dto.getStatus() != null && post.getStatus() != 1 && dto.getStatus() == 1) {
            publishedAt = LocalDateTime.now();
        }

        // 动态更新：从 DTO 中非 null 的字段会被写入数据库（XML <if> 判断）
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

        // 重新关联分类
        if (dto.getCategoryIds() != null) {
            postCategoryMapper.deleteByPostId(postId);
            for (Long catId : dto.getCategoryIds()) {
                postCategoryMapper.insert(postId, catId);
            }
        }

        // 重新关联标签
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
    public PostVO getById(Long id) {
        Post post = postMapper.findById(id);
        if (post == null) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        return toPostVO(post);
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
        return toPostVO(post);
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
        return toPostVO(post);
    }

    @Override
    public PageResult<PostListVO> listPublished(int pageNum, int pageSize, String keyword, Long categoryId, Long tagId) {
        PageHelper.startPage(pageNum, pageSize);
        List<Post> posts = postMapper.findPublishedList(keyword, categoryId, tagId);
        PageInfo<Post> pageInfo = new PageInfo<>(posts);
        List<PostListVO> voList = posts.stream().map(this::toPostListVO).collect(Collectors.toList());
        return PageResult.of(pageInfo, voList);
    }

    @Override
    public PageResult<PostListVO> listAdmin(int pageNum, int pageSize, String keyword, Integer status, Long categoryId) {
        PageHelper.startPage(pageNum, pageSize);
        List<Post> posts = postMapper.findAdminList(keyword, status, categoryId);
        PageInfo<Post> pageInfo = new PageInfo<>(posts);
        List<PostListVO> voList = posts.stream().map(this::toPostListVO).collect(Collectors.toList());
        return PageResult.of(pageInfo, voList);
    }

    private PostVO toPostVO(Post post) {
        PostVO vo = new PostVO();
        BeanUtils.copyProperties(post, vo);

        // 作者
        UserVO author = new UserVO();
        var user = userMapper.findById(post.getUserId());
        if (user != null) {
            BeanUtils.copyProperties(user, author);
            vo.setAuthor(author);
        }

        // 分类
        List<Category> categories = postCategoryMapper.findCategoriesByPostId(post.getId());
        vo.setCategories(categories.stream().map(c -> {
            CategoryVO cv = new CategoryVO();
            BeanUtils.copyProperties(c, cv);
            return cv;
        }).collect(Collectors.toList()));

        // 标签
        List<Tag> tags = tagMapper.findByPostId(post.getId());
        vo.setTags(tags.stream().map(t -> {
            TagVO tv = new TagVO();
            BeanUtils.copyProperties(t, tv);
            return tv;
        }).collect(Collectors.toList()));

        return vo;
    }

    private PostListVO toPostListVO(Post post) {
        PostListVO vo = new PostListVO();
        BeanUtils.copyProperties(post, vo);

        UserVO author = new UserVO();
        var user = userMapper.findById(post.getUserId());
        if (user != null) {
            BeanUtils.copyProperties(user, author);
            vo.setAuthor(author);
        }

        List<Category> categories = postCategoryMapper.findCategoriesByPostId(post.getId());
        vo.setCategories(categories.stream().map(c -> {
            CategoryVO cv = new CategoryVO();
            BeanUtils.copyProperties(c, cv);
            return cv;
        }).collect(Collectors.toList()));

        List<Tag> tags = tagMapper.findByPostId(post.getId());
        vo.setTags(tags.stream().map(t -> {
            TagVO tv = new TagVO();
            BeanUtils.copyProperties(t, tv);
            return tv;
        }).collect(Collectors.toList()));

        return vo;
    }
}
