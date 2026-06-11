package com.blog.service;

import com.blog.dto.TagDTO;
import com.blog.vo.TagVO;

import java.util.List;

public interface TagService {

    List<TagVO> listAll();

    TagVO create(TagDTO dto);

    TagVO update(Long id, TagDTO dto);

    void delete(Long id);
}
