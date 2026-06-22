package com.blog.service.impl;

import com.blog.converter.EntityConverter;
import com.blog.entity.Category;
import com.blog.entity.Post;
import com.blog.entity.Tag;
import com.blog.entity.User;
import com.blog.mapper.PostCategoryMapper;
import com.blog.mapper.TagMapper;
import com.blog.mapper.UserMapper;
import com.blog.service.PostEnrichmentService;
import com.blog.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 文章数据丰富化服务实现类
 *
 * <p>负责将文章实体转换为视图对象，并填充关联数据。</p>
 *
 * @author Diamond
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PostEnrichmentServiceImpl implements PostEnrichmentService {

    private final UserMapper userMapper;
    private final PostCategoryMapper postCategoryMapper;
    private final TagMapper tagMapper;
    private final EntityConverter entityConverter;

    /**
     * 将单个文章实体转换为详情视图对象
     */
    @Override
    public PostVO enrichPostVO(Post post) {
        PostVO vo = entityConverter.toPostVO(post);
        vo.setAuthor(loadAuthor(post.getUserId()));
        vo.setCategories(loadCategories(post.getId()));
        vo.setTags(loadTags(post.getId()));
        return vo;
    }

    /**
     * 批量将文章实体转换为列表视图对象
     *
     * <p>优化 N+1 查询问题，通过批量查询加载关联数据。</p>
     */
    @Override
    public List<PostListVO> enrichPostListVO(List<Post> posts) {
        if (posts.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集所有需要查询的 ID
        Set<Long> userIds = posts.stream()
                .map(Post::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> postIds = posts.stream()
                .map(Post::getId)
                .collect(Collectors.toSet());

        // 批量查询作者（1 次 SQL）
        Map<Long, User> userMap = userMapper.findByIds(new ArrayList<>(userIds)).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 批量查询分类（1 次 SQL）
        Map<Long, List<Category>> categoryMap = postCategoryMapper
                .findCategoriesByPostIds(new ArrayList<>(postIds)).stream()
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
            vo.setCategories(categories.stream()
                    .map(entityConverter::toCategoryVO)
                    .collect(Collectors.toList()));

            List<Tag> tags = tagMap.getOrDefault(post.getId(), Collections.emptyList());
            vo.setTags(tags.stream()
                    .map(entityConverter::toTagVO)
                    .collect(Collectors.toList()));

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 加载作者信息
     */
    private UserVO loadAuthor(Long userId) {
        if (userId == null) return null;
        User user = userMapper.findById(userId);
        return user != null ? entityConverter.toUserVO(user) : null;
    }

    /**
     * 加载文章的分类列表
     */
    private List<CategoryVO> loadCategories(Long postId) {
        return postCategoryMapper.findCategoriesByPostId(postId).stream()
                .map(entityConverter::toCategoryVO)
                .collect(Collectors.toList());
    }

    /**
     * 加载文章的标签列表
     */
    private List<TagVO> loadTags(Long postId) {
        return tagMapper.findByPostId(postId).stream()
                .map(entityConverter::toTagVO)
                .collect(Collectors.toList());
    }
}
