package co.edu.uniquindio.poo.ui;

import co.edu.uniquindio.poo.modelo.Billetera;
import co.edu.uniquindio.poo.servicio.PlataformaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billeteras")
public class BilleteraController {

    private final PlataformaService servicio;

    public BilleteraController(PlataformaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Billetera[] listar() {
        return servicio.listarBilleteras();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Billetera crear(@RequestBody BilleteraRequest solicitud) {
        return servicio.crearBilletera(
                solicitud.codigo(),
                solicitud.nombre(),
                solicitud.tipo(),
                solicitud.identificacionTitular(),
                solicitud.saldoInicial(),
                solicitud.umbralSaldoBajo());
    }
}
