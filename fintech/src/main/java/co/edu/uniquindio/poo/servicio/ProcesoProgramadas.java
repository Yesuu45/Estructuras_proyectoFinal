package co.edu.uniquindio.poo.servicio;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProcesoProgramadas {

    private final PlataformaService servicio;

    public ProcesoProgramadas(PlataformaService servicio) {
        this.servicio = servicio;
    }

    @Scheduled(fixedRate = 5000)
    public void revisar() {
        servicio.ejecutarPendientes();  
    }
}