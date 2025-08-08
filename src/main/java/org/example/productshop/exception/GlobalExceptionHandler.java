package org.example.productshop.exception;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<String> handleDuplicateKey(DuplicateKeyException e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("註冊失敗：email 已被註冊");
    }

    @ExceptionHandler(BadSqlGrammarException.class)
    public ResponseEntity<String> handleBadSql(BadSqlGrammarException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("資料庫錯誤，可能是欄位不存在或SQL錯誤");
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleOtherErrors(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("server error"+e.getMessage());
    }
}
