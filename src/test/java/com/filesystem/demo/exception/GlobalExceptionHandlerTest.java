package com.filesystem.demo.exception;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        Mockito.when(request.getRequestURI()).thenReturn("/test");
    }

    @Test
    void testHandleHttpMediaTypeNotAcceptableException() {
        var ex = new HttpMediaTypeNotAcceptableException("Not acceptable");
        ResponseEntity<ErrorResponse> response = handler.handleHttpMediaTypeNotAcceptableException(ex, request);
        assertEquals(406, response.getStatusCodeValue());
    }

    @Test
    void testHandleHttpMediaTypeNotSupportedException() {
        var ex = new HttpMediaTypeNotSupportedException("Not supported");
        ResponseEntity<ErrorResponse> response = handler.handleHttpMediaTypeNotSupportedException(ex, request);
        assertEquals(415, response.getStatusCodeValue());
    }

    @Test
    void testHandleNoHandlerFoundException() {
        var ex = new NoHandlerFoundException("GET", "/test", null);
        ResponseEntity<ErrorResponse> response = handler.handleNoHandlerFoundException(ex, request);
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void testHandleMethodArgumentTypeMismatchException() {
        var ex = new MethodArgumentTypeMismatchException("val", String.class, "param", null, new RuntimeException());
        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentTypeMismatchException(ex, request);
        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void testHandleConstraintViolationException() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        jakarta.validation.Path mockPath = mock(jakarta.validation.Path.class);
        Mockito.when(mockPath.toString()).thenReturn("field");
        Mockito.when(violation.getPropertyPath()).thenReturn(mockPath);
        Mockito.when(violation.getMessage()).thenReturn("must not be null");

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));
        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolationException(ex, request);
        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void testHandleMissingPathVariableException() {
        var ex = new MissingPathVariableException("id", null);
        ResponseEntity<ErrorResponse> response = handler.handleMissingPathVariableException(ex, request);
        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void testHandleServletRequestBindingException() {
        var ex = new ServletRequestBindingException("Missing header");
        ResponseEntity<ErrorResponse> response = handler.handleServletRequestBindingException(ex, request);
        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void testHandleConversionNotSupportedException() {
        var ex = new ConversionNotSupportedException("input", String.class, new RuntimeException());
        ResponseEntity<ErrorResponse> response = handler.handleConversionNotSupportedException(ex, request);
        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void testHandleDataIntegrityViolationException() {
        var cause = new Throwable("Duplicate entry");
        var ex = new DataIntegrityViolationException("Integrity error", cause);
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolationException(ex, request);
        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void testHandleHttpMessageNotReadableException() {
        var ex = new HttpMessageNotReadableException("Invalid JSON");
        ResponseEntity<ErrorResponse> response = handler.handleHttpMessageNotReadableException(ex, request);
        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void testHandleHttpRequestMethodNotSupportedException() {
        var ex = new HttpRequestMethodNotSupportedException("PUT");
        ResponseEntity<ErrorResponse> response = handler.handleHttpRequestMethodNotSupportedException(ex, request);
        assertEquals(405, response.getStatusCodeValue());
    }
} 
