package co.edu.uniquindio.poo.ui;

import co.edu.uniquindio.poo.modelo.OperacionProgramada;
import co.edu.uniquindio.poo.servicio.PlataformaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/programadas")
public class ProgramadaController {

    private final PlataformaService servicio;

    public ProgramadaController(PlataformaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public OperacionProgramada[] listar(HttpSession sesion) {
        return servicio.listarProgramadas(SesionActual.identificacion(sesion));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OperacionProgramada programar(@RequestBody ProgramadaRequest solicitud, HttpSession sesion) {
        return servicio.programar(SesionActual.identificacion(sesion), solicitud.fechaEjecucion(), solicitud.tipo(),
                solicitud.codigoOrigen(), solicitud.codigoDestino(), solicitud.valor(),
                solicitud.periodicidad(), solicitud.ocurrencias());
    }

    @PostMapping("/ejecutar")
    public ResultadoEjecucion ejecutar(HttpSession sesion) {
        return new ResultadoEjecucion(servicio.ejecutarPendientes(SesionActual.identificacion(sesion)));
    }
}