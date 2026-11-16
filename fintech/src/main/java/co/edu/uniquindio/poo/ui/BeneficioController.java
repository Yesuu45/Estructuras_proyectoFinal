package co.edu.uniquindio.poo.ui;

import co.edu.uniquindio.poo.modelo.Beneficio;
import co.edu.uniquindio.poo.modelo.BeneficioDisponible;
import co.edu.uniquindio.poo.servicio.PlataformaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
    public Beneficio[] listar() {
        return servicio.listarBeneficios();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Beneficio canjear(@RequestBody BeneficioRequest solicitud) {
        return servicio.canjear(solicitud.identificacionUsuario(), solicitud.idBeneficio());
    }
}