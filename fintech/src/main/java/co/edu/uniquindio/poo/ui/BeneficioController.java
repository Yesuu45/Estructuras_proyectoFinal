package co.edu.uniquindio.poo.ui;

import co.edu.uniquindio.poo.modelo.Beneficio;
import co.edu.uniquindio.poo.modelo.BeneficioDisponible;
import co.edu.uniquindio.poo.servicio.PlataformaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/beneficios")
public class BeneficioController {

    private final PlataformaService servicio;

    public BeneficioController(PlataformaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/catalogo")
    public BeneficioDisponible[] catalogo() {
        return servicio.listarCatalogo();
    }

    @GetMapping
    public Beneficio[] listar(HttpSession sesion) {
        return servicio.listarBeneficios(SesionActual.identificacion(sesion));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Beneficio canjear(@RequestBody BeneficioRequest solicitud, HttpSession sesion) {
        // Se canjea siempre con los puntos de quien inició sesión; se ignora el usuario de la solicitud
        return servicio.canjear(SesionActual.identificacion(sesion), solicitud.idBeneficio());
    }
}