package com.theoyu.framework.common.response;

import lombok.Data;

import java.util.List;

/**
 * 游标分页响应工具类
 * @param <T> 数据类型
 * @author theoyu
 */
@Data
public class CursorPageResponse<T> extends Response<List<T>> {

    /**
     * 游标值（下一页的起始位置）
     */
    private String cursor;

    /**
     * 是否有下一页
     */
    private boolean hasMore;

    /**
     * 每页数据量
     */
    private long pageSize;

    /**
     * 本次返回的数据量
     */
    private int count;

    // =================================== 成功响应 ===================================

    /**
     * 成功响应（有更多数据）
     * 
     * @param data     数据列表
     * @param cursor   下一页游标
     * @param pageSize 每页数量
     * @param <T>      数据类型
     * @return 游标分页响应
     */
    public static <T> CursorPageResponse<T> success(List<T> data, String cursor, long pageSize) {
        CursorPageResponse<T> response = new CursorPageResponse<>();
        response.setSuccess(true);
        response.setData(data);
        response.setCursor(cursor);
        response.setPageSize(pageSize);
        response.setCount(data != null ? data.size() : 0);
        response.setHasMore(data != null && data.size() >= pageSize);
        return response;
    }

    /**
     * 成功响应（明确指定是否有更多数据）
     * 
     * @param data     数据列表
     * @param cursor   下一页游标（如果没有更多数据，可传 null）
     * @param hasMore  是否有下一页
     * @param pageSize 每页数量
     * @param <T>      数据类型
     * @return 游标分页响应
     */
    public static <T> CursorPageResponse<T> success(List<T> data, String cursor, boolean hasMore, long pageSize) {
        CursorPageResponse<T> response = new CursorPageResponse<>();
        response.setSuccess(true);
        response.setData(data);
        response.setCursor(hasMore ? cursor : null);
        response.setPageSize(pageSize);
        response.setCount(data != null ? data.size() : 0);
        response.setHasMore(hasMore);
        return response;
    }

    /**
     * 空数据响应（第一页无数据或已到末尾）
     * 
     * @param pageSize 每页数量
     * @param <T>      数据类型
     * @return 游标分页响应
     */
    public static <T> CursorPageResponse<T> empty(long pageSize) {
        CursorPageResponse<T> response = new CursorPageResponse<>();
        response.setSuccess(true);
        response.setData(List.of());
        response.setCursor(null);
        response.setPageSize(pageSize);
        response.setCount(0);
        response.setHasMore(false);
        return response;
    }

    // =================================== 辅助方法 ===================================

    /**
     * 检查是否为空结果
     * 
     * @return true 如果没有数据
     */
    public boolean isEmpty() {
        return count == 0;
    }

    /**
     * 检查是否为最后一页
     * 
     * @return true 如果没有更多数据
     */
    public boolean isLastPage() {
        return !hasMore;
    }
}