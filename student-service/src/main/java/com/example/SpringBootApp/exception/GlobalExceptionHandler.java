package com.example.SpringBootApp.exception;

import com.example.SpringBootApp.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(StudentNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "Student o podanym ID nie został odnaleziony."
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // 400 - Zły wiek (np. ujemny) lub puste/błędne imię studenta
    @ExceptionHandler({StudentInvalidAgeException.class, InvalidNameException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "Niepoprawne dane studenta (błędne imię lub wiek)."
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // 500 - Gdy wywali się coś niespodziewanego (np. problem z restTemplate przy usuwaniu ocen studenta)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                ex.getMessage() != null ? ex.getMessage() : "Wystąpił wewnętrzny błąd serwera."
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}