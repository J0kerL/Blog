package com.blog.mapper.convert;

import com.blog.entity.*;
import com.blog.vo.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EntityConverter {

    UserVO toUserVO(User user);

    AdminUserVO toAdminUserVO(User user);

    CategoryVO toCategoryVO(Category category);

    TagVO toTagVO(Tag tag);

    @Mapping(target = "author", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "tags", ignore = true)
    PostVO toPostVO(Post post);

    @Mapping(target = "author", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "tags", ignore = true)
    PostListVO toPostListVO(Post post);

    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "replies", ignore = true)
    CommentVO toCommentVO(Comment comment);
}
