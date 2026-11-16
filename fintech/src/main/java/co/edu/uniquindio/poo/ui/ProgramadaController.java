package co.edu.uniquindio.poo.ui;

import co.edu.uniquindio.poo.modelo.OperacionProgramada;
import co.edu.uniquindio.poo.servicio.PlataformaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/programadas")
public class ProgramadaController {

    private final PlataformaService servicio;

    public ProgramadaController(PlataformaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public OperacionProgramada[] listar() {
        return servicio.listarProgramadas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OperacionProgramada programar(@RequestBody ProgramadaRequest solicitud) {
        return servicio.programar(solicitud.fechaEjecucion(), solicitud.tipo(),
                solicitud.codigoOrigen(), solicitud.codigoDestino(), solicitud.valor());
    }

    @PostMapping("/ejecutar")
    public ResultadoEjecucion ejecutar() {
        return new ResultadoEjecucion(servicio.ejecutarPendientes());
    }
}