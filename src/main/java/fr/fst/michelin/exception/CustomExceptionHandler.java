package fr.fst.michelin.exception;

import fr.fst.michelin.dto.ErreurDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.security.Principal;

@ControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<ErreurDto> handle(NotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ErreurDto(404, ex.getMessage()));
    }

    @ExceptionHandler
    public ResponseEntity<ErreurDto> handle(AccessDeniedException ex, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(new ErreurDto(401, "Vous devez être connecté"));
        }
        return ResponseEntity.status(403)
                .body(new ErreurDto(403, "Vous n'avez pas le droit de faire cette action"));
    }

    @ExceptionHandler
    public ResponseEntity<ErreurDto> handle(Exception ex) {
        logger.error("Erreur interne", ex);
        return ResponseEntity.status(500)
                .body(new ErreurDto(500, "Erreur interne du serveur"));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        FieldError erreur = ex.getBindingResult().getFieldError();
        String message = erreur.getField() + " : " + erreur.getDefaultMessage();
        return ResponseEntity.status(400)
                .body(new ErreurDto(400, message));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        String message = HttpStatus.valueOf(statusCode.value()).getReasonPhrase();
        return ResponseEntity.status(statusCode)
                .body(new ErreurDto(statusCode.value(), message));
    }
}
