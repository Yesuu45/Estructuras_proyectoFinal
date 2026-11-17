package co.edu.uniquindio.poo.ui;

import co.edu.uniquindio.poo.servicio.AvanceReloj;
import co.edu.uniquindio.poo.servicio.PlataformaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

/** Reloj simulado de la plataforma: cualquiera lo consulta, solo el administrador lo mueve. */
@RestController
@RequestMapping("/api/reloj")
public class RelojController {

    private final PlataformaService servicio;

    public RelojController(PlataformaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public AvanceReloj estado() {
        return servicio.estadoReloj();
    }

    @PostMapping("/avanzar")
    public AvanceReloj avanzar(@RequestBody AvanceRelojRequest solicitud, HttpSession sesion) {
        return servicio.avanzarReloj(SesionActual.identificacion(sesion), solicitud.minutos(), solicitud.ejecutar());
    }

    @PostMapping("/restablecer")
    public AvanceReloj restablecer(HttpSession sesion) {
        return servicio.restablecerReloj(SesionActual.identificacion(sesion));
    }
}
