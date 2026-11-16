package co.edu.uniquindio.poo.modelo;

import java.time.LocalDateTime;

public class Beneficio {

    private final String identificacionUsuario;
    private final String descripcion;
    private final int puntosCosto;
    private final LocalDateTime fechaCanje;

    public Beneficio(String identificacionUsuario, String descripcion, int puntosCosto, LocalDateTime fechaCanje) {
        if (identificacionUsuario == null || identificacionUsuario.isBlank()) {
            throw new IllegalArgumentException("La identificación del usuario es obligatoria");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción es obligatoria");
        }
        if (puntosCosto <= 0) {
            throw new IllegalArgumentException("El costo en puntos debe ser mayor que cero");
        }
        this.identificacionUsuario = identificacionUsuario.trim();
        this.descripcion = descripcion.trim();
        this.puntosCosto = puntosCosto;
        this.fechaCanje = fechaCanje == null ? LocalDateTime.now() : fechaCanje;
    }

    public String getIdentificacionUsuario() {
        return identificacionUsuario;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getPuntosCosto() {
        return puntosCosto;
    }

    public LocalDateTime getFechaCanje() {
        return fechaCanje;
    }

    @Override
    public String toString() {
        return descripcion + " (" + puntosCosto + " pts) el " + fechaCanje;
    }
}