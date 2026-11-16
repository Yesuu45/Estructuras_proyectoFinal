package co.edu.uniquindio.poo.ui;

import co.edu.uniquindio.poo.modelo.Transaccion;
import co.edu.uniquindio.poo.servicio.PlataformaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transacciones")
public class TransaccionController {

    private final PlataformaService servicio;

    public TransaccionController(PlataformaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Transaccion[] listar() {
        return servicio.listarTransacciones();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaccion registrar(@RequestBody TransaccionRequest solicitud) {
        return servicio.operar(solicitud.tipo(), solicitud.codigoOrigen(),
                solicitud.codigoDestino(), solicitud.valor());
    }

    @PostMapping("/{id}/revertir")
    public Transaccion revertir(@PathVariable String id) {
        return servicio.revertir(id);
    }
}