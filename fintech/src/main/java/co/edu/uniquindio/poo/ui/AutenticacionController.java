package co.edu.uniquindio.poo.ui;

import co.edu.uniquindio.poo.modelo.Usuario;
import co.edu.uniquindio.poo.servicio.PlataformaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Inicio y cierre de sesión. La sesión guarda solo la identificación del usuario. */
@RestController
@RequestMapping("/api/auth")
public class AutenticacionController {

    public static final String ATRIBUTO_USUARIO = "usuarioActual";

    private final PlataformaService servicio;

    public AutenticacionController(PlataformaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/login")
    public ResponseEntity<Object> iniciarSesion(@RequestBody LoginRequest solicitud, HttpServletRequest peticion) {
        try {
            Usuario usuario = servicio.autenticar(solicitud.identificacion(), solicitud.clave());
            HttpSession anterior = peticion.getSession(false);
            if (anterior != null) {
                anterior.invalidate();   // sesión nueva en cada ingreso
            }
            peticion.getSession(true).setAttribute(ATRIBUTO_USUARIO, usuario.getIdentificacion());
            return ResponseEntity.ok(usuario);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorRespuesta(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> cerrarSesion(HttpServletRequest peticion) {
        HttpSession sesion = peticion.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    /** Quién está conectado. 401 si no hay sesión; la interfaz lo usa para pedir el inicio de sesión. */
    @GetMapping("/yo")
    public ResponseEntity<Object> usuarioActual(HttpServletRequest peticion) {
        HttpSession sesion = peticion.getSession(false);
        String identificacion = sesion == null ? null : (String) sesion.getAttribute(ATRIBUTO_USUARIO);
        if (identificacion == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorRespuesta("Debes iniciar sesión"));
        }
        try {
            return ResponseEntity.ok(servicio.buscarUsuario(identificacion));
        } catch (IllegalArgumentException e) {
            sesion.invalidate();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorRespuesta("Debes iniciar sesión"));
        }
    }
}
