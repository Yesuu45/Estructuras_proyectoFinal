package co.edu.uniquindio.poo.ui;

import co.edu.uniquindio.poo.modelo.Transaccion;
import co.edu.uniquindio.poo.servicio.PlataformaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/transacciones")
public class TransaccionController {

    private final PlataformaService servicio;

    public TransaccionController(PlataformaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Transaccion[] listar(@RequestParam(required = false) String desde,
                                @RequestParam(required = false) String hasta,
                                HttpSession sesion) {
        return servicio.listarTransacciones(SesionActual.identificacion(sesion), fecha(desde), fecha(hasta));
    }

    private static LocalDateTime fecha(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha inválida: " + texto);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaccion registrar(@RequestBody TransaccionRequest solicitud, HttpSession sesion) {
        return servicio.operar(SesionActual.identificacion(sesion), solicitud.tipo(), solicitud.codigoOrigen(),
                solicitud.codigoDestino(), solicitud.valor());
    }

    /** Revierte la operación que está en el tope de la pila del usuario (la más reciente). */
    @PostMapping("/deshacer")
    public Transaccion deshacer(HttpSession sesion) {
        return servicio.deshacerUltima(SesionActual.identificacion(sesion));
    }

    @GetMapping("/reversibles")
    public Transaccion[] reversibles(HttpSession sesion) {
        return servicio.listarReversibles(SesionActual.identificacion(sesion));
    }

    @GetMapping("/deshechas")
    public Transaccion[] deshechas(HttpSession sesion) {
        return servicio.listarDeshechas(SesionActual.identificacion(sesion));
    }

    @PostMapping("/{id}/revertir")
    public Transaccion revertir(@PathVariable String id, HttpSession sesion) {
        return servicio.revertir(SesionActual.identificacion(sesion), id);
    }
}