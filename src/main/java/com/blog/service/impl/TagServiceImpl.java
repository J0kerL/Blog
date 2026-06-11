package com.blog.service.impl;

import com.blog.common.BusinessException;
import com.blog.common.ResultCode;
import com.blog.dto.TagDTO;
import com.blog.entity.Tag;
import com.blog.mapper.TagMapper;
import com.blog.mapper.convert.EntityConverter;
import com.blog.service.TagService;
import com.blog.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final EntityConverter entityConverter;

    @Override
    @Transactional(readOnly = true)
    public List<TagVO> listAll() {
        return tagMapper.findAll().stream().map(this::toVOWithPostCount).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TagVO create(TagDTO dto) {
        Tag tag = new Tag();
        tag.setName(dto.getName());
        tag.setSlug(dto.getSlug());
        tagMapper.insert(tag);
        return entityConverter.toTagVO(tag);
    }

    @Override
    @Transactional
    public TagVO update(Long id, TagDTO dto) {
        if (tagMapper.findById(id) == null) {
            throw new BusinessException(ResultCode.TAG_NOT_FOUND);
        }
        Tag updateParam = new Tag();
        updateParam.setId(id);
        updateParam.setName(dto.getName());
        updateParam.setSlug(dto.getSlug());
        tagMapper.updateSelective(updateParam);

        Tag updated = tagMapper.findById(id);
        return entityConverter.toTagVO(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (tagMapper.findById(id) == null) {
            throw new BusinessException(ResultCode.TAG_NOT_FOUND);
        }
        tagMapper.deleteById(id);
    }

    private TagVO toVOWithPostCount(Tag tag) {
        TagVO vo = entityConverter.toTagVO(tag);
        vo.setPostCount(tagMapper.countPostsByTagId(tag.getId()));
        return vo;
    }
}
