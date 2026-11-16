package co.edu.uniquindio.poo.ui;

import co.edu.uniquindio.poo.modelo.Billetera;
import co.edu.uniquindio.poo.servicio.PlataformaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/billeteras")
public class BilleteraController {

    private final PlataformaService servicio;

    public BilleteraController(PlataformaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Billetera[] listar(HttpSession sesion) {
        return servicio.listarBilleteras(SesionActual.identificacion(sesion));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Billetera crear(@RequestBody BilleteraRequest solicitud, HttpSession sesion) {
        // El titular es siempre quien inició sesión; se ignora lo que venga en la solicitud
        String titular = SesionActual.identificacion(sesion);
        return servicio.crearBilletera(
                solicitud.codigo(),
                solicitud.nombre(),
                solicitud.tipo(),
                titular,
                solicitud.saldoInicial(),
                solicitud.umbralSaldoBajo());
    }
}
