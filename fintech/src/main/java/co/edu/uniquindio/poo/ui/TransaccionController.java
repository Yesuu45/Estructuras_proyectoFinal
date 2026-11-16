package co.edu.uniquindio.poo.ui;

import co.edu.uniquindio.poo.modelo.Transaccion;
import co.edu.uniquindio.poo.servicio.PlataformaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transacciones")
public class TransaccionController {

    private final PlataformaService servicio;

    public TransaccionController(PlataformaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Transaccion[] listar(HttpSession sesion) {
        return servicio.listarTransacciones(SesionActual.identificacion(sesion));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaccion registrar(@RequestBody TransaccionRequest solicitud, HttpSession sesion) {
        return servicio.operar(SesionActual.identificacion(sesion), solicitud.tipo(), solicitud.codigoOrigen(),
                solicitud.codigoDestino(), solicitud.valor());
    }

    @PostMapping("/{id}/revertir")
    public Transaccion revertir(@PathVariable String id, HttpSession sesion) {
        return servicio.revertir(SesionActual.identificacion(sesion), id);
    }
}