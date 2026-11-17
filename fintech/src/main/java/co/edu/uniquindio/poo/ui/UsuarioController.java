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

    @PutMapping("/{identificacion}")
    public Usuario modificar(@PathVariable String identificacion, @RequestBody UsuarioRequest solicitud,
                             HttpSession sesion) {
        return servicio.modificarUsuario(SesionActual.identificacion(sesion), identificacion,
                solicitud.nombre(), solicitud.correo(), solicitud.clave());
    }

    @DeleteMapping("/{identificacion}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String identificacion, HttpSession sesion) {
        servicio.eliminarUsuario(SesionActual.identificacion(sesion), identificacion);
    }
}
