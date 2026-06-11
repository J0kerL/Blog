package com.blog.service.impl;

import com.blog.common.BusinessException;
import com.blog.common.ResultCode;
import com.blog.dto.TagDTO;
import com.blog.entity.Tag;
import com.blog.mapper.TagMapper;
import com.blog.service.TagService;
import com.blog.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    @Override
    public List<TagVO> listAll() {
        return tagMapper.findAll().stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public TagVO create(TagDTO dto) {
        Tag tag = new Tag();
        BeanUtils.copyProperties(dto, tag);
        tagMapper.insert(tag);
        return toVO(tag);
    }

    @Override
    public TagVO update(Long id, TagDTO dto) {
        if (tagMapper.findById(id) == null) {
            throw new BusinessException(ResultCode.TAG_NOT_FOUND);
        }
        // 动态更新：仅 DTO 中非 null 的字段会被写入数据库
        Tag updateParam = new Tag();
        updateParam.setId(id);
        updateParam.setName(dto.getName());
        updateParam.setSlug(dto.getSlug());
        tagMapper.updateSelective(updateParam);
        return toVO(tagMapper.findById(id));
    }

    @Override
    public void delete(Long id) {
        if (tagMapper.findById(id) == null) {
            throw new BusinessException(ResultCode.TAG_NOT_FOUND);
        }
        tagMapper.deleteById(id);
    }

    private TagVO toVO(Tag tag) {
        TagVO vo = new TagVO();
        BeanUtils.copyProperties(tag, vo);
        vo.setPostCount(tagMapper.countPostsByTagId(tag.getId()));
        return vo;
    }
}
