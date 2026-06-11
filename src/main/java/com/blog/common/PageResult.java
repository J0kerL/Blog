package com.blog.common;

import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "分页结果")
public class PageResult<T> implements Serializable {

    @Schema(description = "当前页码")
    private int pageNum;

    @Schema(description = "每页大小")
    private int pageSize;

    @Schema(description = "总记录数")
    private long total;

    @Schema(description = "总页数")
    private int pages;

    @Schema(description = "数据列表")
    private List<T> list;

    public static <T> PageResult<T> of(PageInfo<T> pageInfo) {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(pageInfo.getPageNum());
        result.setPageSize(pageInfo.getSize());
        result.setTotal(pageInfo.getTotal());
        result.setPages(pageInfo.getPages());
        result.setList(pageInfo.getList());
        return result;
    }

    /**
     * 从 PageInfo 提取分页元数据，配合已转换的 VO 列表使用
     * 解决 PageInfo<Post> 无法直接转为 PageResult<PostListVO> 的泛型问题
     */
    public static <T> PageResult<T> of(PageInfo<?> pageInfo, List<T> voList) {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(pageInfo.getPageNum());
        result.setPageSize(pageInfo.getSize());
        result.setTotal(pageInfo.getTotal());
        result.setPages(pageInfo.getPages());
        result.setList(voList);
        return result;
    }
}
