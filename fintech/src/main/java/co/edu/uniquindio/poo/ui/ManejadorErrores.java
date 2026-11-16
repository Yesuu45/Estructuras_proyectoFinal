package co.edu.uniquindio.poo.ui;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Convierte los errores de negocio en respuestas JSON con el campo "mensaje",
 * que es lo que lee el JavaScript de la interfaz.
 */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorRespuesta> manejarErrorNegocio(RuntimeException ex) {
        return ResponseEntity.badRequest().body(new ErrorRespuesta(ex.getMessage()));
    }
}
