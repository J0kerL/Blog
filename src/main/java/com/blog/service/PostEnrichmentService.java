package com.blog.service;

import com.blog.entity.Post;
import com.blog.vo.PostListVO;
import com.blog.vo.PostVO;

import java.util.List;

/**
 * 文章数据丰富化服务接口
 *
 * <p>负责将文章实体转换为视图对象，并填充关联数据（作者、分类、标签等）。</p>
 *
 * @author Diamond
 * @since 1.0.0
 */
public interface PostEnrichmentService {

    /**
     * 将单个文章实体转换为详情视图对象
     *
     * <p>会额外加载作者信息、分类列表和标签列表。</p>
     *
     * @param post 文章实体
     * @return 文章详情视图对象
     */
    PostVO enrichPostVO(Post post);

    /**
     * 批量将文章实体转换为列表视图对象
     *
     * <p>优化 N+1 查询问题，通过批量查询加载关联数据。</p>
     *
     * @param posts 文章实体列表
     * @return 文章列表视图对象列表
     */
    List<PostListVO> enrichPostListVO(List<Post> posts);
}
