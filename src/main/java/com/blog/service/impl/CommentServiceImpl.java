package com.blog.service.impl;

import com.blog.common.BusinessException;
import com.blog.common.PageResult;
import com.blog.common.ResultCode;
import com.blog.dto.CommentCreateDTO;
import com.blog.entity.Comment;
import com.blog.entity.User;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.UserMapper;
import com.blog.service.CommentService;
import com.blog.vo.CommentVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final UserMapper userMapper;

    @Override
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
        return toVO(comment);
    }

    @Override
    public List<CommentVO> listByPostId(Long postId) {
        List<Comment> topLevel = commentMapper.findTopLevelByPostId(postId);
        return topLevel.stream().map(c -> {
            CommentVO vo = toVO(c);
            List<Comment> replies = commentMapper.findRepliesByParentId(c.getId());
            vo.setReplies(replies.stream().map(this::toVO).collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public PageResult<CommentVO> listAdmin(int pageNum, int pageSize, Long postId, Integer status) {
        PageHelper.startPage(pageNum, pageSize);
        List<Comment> comments = commentMapper.findAdminList(postId, status);
        PageInfo<Comment> pageInfo = new PageInfo<>(comments);
        List<CommentVO> voList = comments.stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(pageInfo, voList);
    }

    @Override
    public void approve(Long id) {
        if (commentMapper.findById(id) == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        commentMapper.updateStatus(id, 1);
    }

    @Override
    public void reject(Long id) {
        if (commentMapper.findById(id) == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        commentMapper.updateStatus(id, 2);
    }

    @Override
    public void delete(Long id) {
        if (commentMapper.findById(id) == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        commentMapper.deleteById(id);
    }

    private CommentVO toVO(Comment comment) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comment, vo);

        if (comment.getUserId() != null) {
            User user = userMapper.findById(comment.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }
        }
        return vo;
    }
}
