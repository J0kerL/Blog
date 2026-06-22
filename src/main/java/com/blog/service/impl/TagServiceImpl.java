package com.blog.service.impl;

import com.blog.common.BusinessException;
import com.blog.common.ResultCode;
import com.blog.dto.TagDTO;
import com.blog.entity.Tag;
import com.blog.mapper.TagMapper;
import com.blog.converter.EntityConverter;
import com.blog.service.TagService;
import com.blog.util.SlugUtil;
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
    @Transactional(readOnly = true)
    public List<TagVO> listPublished() {
        return tagMapper.findAll().stream().map(this::toVOWithPublishedCount).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagVO> search(String keyword) {
        return tagMapper.search(keyword).stream().map(this::toVOWithPostCount).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TagVO create(TagDTO dto) {
        Tag tag = new Tag();
        tag.setName(dto.getName());
        String slug = (dto.getSlug() != null && !dto.getSlug().isBlank())
                ? dto.getSlug()
                : SlugUtil.generateSlug(dto.getName());
        tag.setSlug(slug);
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
        if (dto.getSlug() != null && !dto.getSlug().isBlank()) {
            updateParam.setSlug(dto.getSlug());
        }
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
        if (tagMapper.countPostsByTagId(id) > 0) {
            throw new BusinessException("该标签下仍有文章，无法删除");
        }
        tagMapper.deleteById(id);
    }

    private TagVO toVOWithPostCount(Tag tag) {
        TagVO vo = entityConverter.toTagVO(tag);
        vo.setPostCount(tagMapper.countPostsByTagId(tag.getId()));
        return vo;
    }

    private TagVO toVOWithPublishedCount(Tag tag) {
        TagVO vo = entityConverter.toTagVO(tag);
        vo.setPostCount(tagMapper.countPublishedPostsByTagId(tag.getId()));
        return vo;
    }
}
