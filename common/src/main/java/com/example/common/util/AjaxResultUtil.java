package com.example.common.util;

import com.alibaba.fastjson.JSON;
import com.example.common.domain.AjaxResult;

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
}