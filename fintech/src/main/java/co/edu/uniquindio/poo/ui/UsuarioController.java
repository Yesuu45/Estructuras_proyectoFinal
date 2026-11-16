package co.edu.uniquindio.poo.ui;

import co.edu.uniquindio.poo.modelo.Usuario;
import co.edu.uniquindio.poo.servicio.PlataformaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final PlataformaService servicio;

    public UsuarioController(PlataformaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Usuario[] listar(HttpSession sesion) {
        return servicio.listarUsuarios(SesionActual.identificacion(sesion));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Usuario registrar(@RequestBody UsuarioRequest solicitud) {
        return servicio.registrarUsuario(solicitud.identificacion(), solicitud.nombre(),
                solicitud.correo(), solicitud.clave());
    }
}
