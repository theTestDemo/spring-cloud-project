package com.example.orderservice.handler;

import com.example.common.domain.AjaxResult;
import com.example.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * <p>
 * 统一处理 Controller 层抛出的异常，返回规范的错误响应格式
 * 避免在每个接口中冲副编写 try-catch 代码，提高代码可维护性
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-26
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常（如用户不存在、库存不足等）
     * <p>
     * 当 Service 层抛出{@link BusinessException}时，返回 HTTP 400状态码，
     * 响应体为{code：500，msg：异常信息}
     * </p>
     *
     * @param e 业务异常对象
     * @return 标准错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<AjaxResult> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(AjaxResult.error(e.getMessage()));
    }

    /**
     * 处理其他未捕获的异常（系统错误）
     * <p>
     * 当发生未预期的异常（如空指针、数据库连接失败等）时，返回 HTTP 500 状态码，
     * 响应体为通用提示，避免暴露系统内部细节
     * </p>
     *
     * @param e 异常对象
     * @return 系统错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AjaxResult> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AjaxResult.error("系统繁忙，请稍后再试"));
    }
}