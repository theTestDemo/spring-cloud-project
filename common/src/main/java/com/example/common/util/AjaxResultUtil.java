package com.example.common.util;

import com.alibaba.fastjson.JSON;
import com.example.common.domain.AjaxResult;

import java.util.Collections;
import java.util.List;

public class AjaxResultUtil {

    /**
     * 从成功的 AjaxResult 中提取 data 并转换为指定类型
     *
     * @param result  AjaxResult 对象
     * @param clazz   目标类型
     * @param <T>     泛型
     * @return 转换后的对象，如果 result 不成功或 data 为空则返回 null
     */
    public static <T> T getData(AjaxResult result, Class<T> clazz) {
        if (result == null || !result.isSuccess()) {
            return null;
        }
        Object data = result.get(AjaxResult.DATA_TAG);
        if (data == null) {
            return null;
        }
        // 如果 data 已经是目标类型，直接返回
        if (clazz.isAssignableFrom(data.getClass())) {
            return clazz.cast(data);
        }
        // 否则通过 JSON 转换
        return JSON.parseObject(JSON.toJSONString(data), clazz);
    }
    /**
     * 从成功的 AjaxResult 中提取 data 并转换为指定类型的列表
     *
     * @param result  AjaxResult 对象
     * @param clazz   列表元素的目标类型
     * @param <T>     泛型
     * @return 转换后的列表，如果 result 不成功或 data 为空则返回空列表
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> getDataList(AjaxResult result, Class<T> clazz) {
        if (result == null || !result.isSuccess()) {
            return Collections.emptyList();
        }
        Object data = result.get(AjaxResult.DATA_TAG);
        if (data == null) {
            return Collections.emptyList();
        }
        // 如果 data 已经是 List 类型，并且元素匹配，直接返回
        if (data instanceof List) {
            List<?> rawList = (List<?>) data;
            if (!rawList.isEmpty() && clazz.isAssignableFrom(rawList.get(0).getClass())) {
                return (List<T>) rawList;
            }
        }
        // 否则通过 JSON 转换
        return JSON.parseArray(JSON.toJSONString(data), clazz);
    }
}