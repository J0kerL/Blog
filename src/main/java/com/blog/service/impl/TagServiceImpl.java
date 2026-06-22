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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final EntityConverter entityConverter;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** 标签列表缓存 Key */
    private static final String CACHE_KEY_ALL = "cache:tags:all";
    private static final String CACHE_KEY_PUBLISHED = "cache:tags:published";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Override
    @Transactional(readOnly = true)
    public List<TagVO> listAll() {
        // 尝试从缓存获取
        String cached = redisTemplate.opsForValue().get(CACHE_KEY_ALL);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<TagVO>>() {});
            } catch (Exception e) {
                log.warn("反序列化标签缓存失败", e);
            }
        }

        // 从数据库查询
        List<TagVO> result = tagMapper.findAll().stream()
                .map(this::toVOWithPostCount)
                .collect(Collectors.toList());

        // 写入缓存
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(CACHE_KEY_ALL, json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("序列化标签缓存失败", e);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagVO> listPublished() {
        // 尝试从缓存获取
        String cached = redisTemplate.opsForValue().get(CACHE_KEY_PUBLISHED);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<TagVO>>() {});
            } catch (Exception e) {
                log.warn("反序列化标签缓存失败", e);
            }
        }

        // 从数据库查询
        List<TagVO> result = tagMapper.findAll().stream()
                .map(this::toVOWithPublishedCount)
                .collect(Collectors.toList());

        // 写入缓存
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(CACHE_KEY_PUBLISHED, json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("序列化标签缓存失败", e);
        }

        return result;
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
        
        // 清除缓存
        clearCache();
        
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
        
        // 清除缓存
        clearCache();
        
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
        
        // 清除缓存
        clearCache();
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

    /**
     * 清除标签缓存
     */
    private void clearCache() {
        redisTemplate.delete(CACHE_KEY_ALL);
        redisTemplate.delete(CACHE_KEY_PUBLISHED);
        log.debug("标签缓存已清除");
    }
}
