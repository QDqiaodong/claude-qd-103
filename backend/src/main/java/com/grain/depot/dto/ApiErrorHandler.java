package com.grain.depot.dto;

import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrorHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Map<String, Object>> handleBiz(BizException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("ok", false);
        body.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    /** 行锁之外的数据库兜底（如同一间仓两张密闭单的唯一索引），给句能看懂的话。 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegrity(DataIntegrityViolationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("ok", false);
        String msg = ex.getMessage() == null ? "" : ex.getMessage();
        if (msg.contains("uk_fumigation_active_granary")) {
            body.put("message", "这间仓已经在密闭，不能再挂第二张密闭中的单");
        } else if (msg.contains("uk_fumigation_code")) {
            body.put("message", "熏蒸单号已经用过了");
        } else {
            body.put("message", "数据冲突：这条记录已经被占住了，请刷新后再试");
        }
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("ok", false);
        body.put("message", "服务处理失败：" + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
