package co.edu.uniquindio.poo.modelo;

import java.time.LocalDateTime;

public class EventoAuditoria {

    private final LocalDateTime fecha;
    private final String identificacionUsuario;
    private final String evento;
    private final NivelRiesgo nivelRiesgo;
    private final String detalle;

    public EventoAuditoria(LocalDateTime fecha, String identificacionUsuario, String evento,
                           NivelRiesgo nivelRiesgo, String detalle) {
        if (evento == null || evento.isBlank()) {
            throw new IllegalArgumentException("El evento es obligatorio");
        }
        this.fecha = fecha == null ? LocalDateTime.now() : fecha;
        this.identificacionUsuario = identificacionUsuario;
        this.evento = evento.trim();
        this.nivelRiesgo = nivelRiesgo == null ? NivelRiesgo.BAJO : nivelRiesgo;
        this.detalle = detalle;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getIdentificacionUsuario() {
        return identificacionUsuario;
    }

    public String getEvento() {
        return evento;
    }

    public NivelRiesgo getNivelRiesgo() {
        return nivelRiesgo;
    }

    public String getDetalle() {
        return detalle;
    }

    @Override
    public String toString() {
        return fecha + " [" + nivelRiesgo + "] " + evento + (detalle == null ? "" : " - " + detalle);
    }
}