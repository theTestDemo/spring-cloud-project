package com.example.common.exception;

/**
 * 业务异常类
 * <p>
 * 用于表示业务逻辑校验失败的异常，如用户名已存在、余额不足、用户不存在等
 * 继承 {@link RuntimeException},属于非受检异常，不强制要求 try-catch
 * 可以统一捕获并返回规范的错误响应（如{@link com.example.common.domain.AjaxResult}）
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-20
 */
public class BusinessException extends RuntimeException{
    /**
     * 构造业务异常
     *
     * @param message 异常信息（将作为错误消息返回给客户端）
     */
    public BusinessException(String message) {
        super(message);   // 把错误信息交给父类（RuntimeException）
    }
}
