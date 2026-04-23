package com.acme.cicd.product.infrastructure.web;

import com.acme.cicd.product.application.DuplicateSkuException;
import com.acme.cicd.product.application.ProductNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

  @ExceptionHandler(ProductNotFoundException.class)
  ProblemDetail notFound(ProductNotFoundException ex) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    pd.setTitle("Not Found");
    pd.setDetail(ex.getMessage());
    return pd;
  }

  @ExceptionHandler(DuplicateSkuException.class)
  ProblemDetail conflict(DuplicateSkuException ex) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    pd.setTitle("Conflict");
    pd.setDetail(ex.getMessage());
    return pd;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail validation(MethodArgumentNotValidException ex) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    pd.setTitle("Validation error");
    pd.setDetail("request validation failed");
    return pd;
  }
}
