package co.edu.uniquindio.poo.ui;

import co.edu.uniquindio.poo.modelo.EventoAuditoria;
import co.edu.uniquindio.poo.modelo.Notificacion;
import co.edu.uniquindio.poo.servicio.PlataformaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final PlataformaService servicio;

    public AuditoriaController(PlataformaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/eventos")
    public EventoAuditoria[] eventos(HttpSession sesion) {
        return servicio.listarEventos(SesionActual.identificacion(sesion));
    }

    @GetMapping("/notificaciones")
    public Notificacion[] notificaciones(HttpSession sesion) {
        return servicio.listarNotificaciones(SesionActual.identificacion(sesion));
    }
}