package co.edu.uniquindio.poo.modelo;

import java.time.LocalDateTime;

public class Notificacion {

    private final String identificacionUsuario;
    private final TipoAlerta tipo;
    private final String mensaje;
    private final LocalDateTime fecha;
    private boolean despachada;

    public Notificacion(String identificacionUsuario, TipoAlerta tipo, String mensaje, LocalDateTime fecha) {
        if (identificacionUsuario == null || identificacionUsuario.isBlank()) {
            throw new IllegalArgumentException("La identificación del usuario es obligatoria");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de alerta es obligatorio");
        }
        if (mensaje == null || mensaje.isBlank()) {
            throw new IllegalArgumentException("El mensaje es obligatorio");
        }
        this.identificacionUsuario = identificacionUsuario.trim();
        this.tipo = tipo;
        this.mensaje = mensaje.trim();
        this.fecha = fecha == null ? LocalDateTime.now() : fecha;
        this.despachada = false;
    }

    public void marcarDespachada() {
        despachada = true;
    }

    public String getIdentificacionUsuario() {
        return identificacionUsuario;
    }

    public TipoAlerta getTipo() {
        return tipo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public boolean isDespachada() {
        return despachada;
    }

    @Override
    public String toString() {
        return tipo + ": " + mensaje + " [" + (despachada ? "despachada" : "pendiente") + "]";
    }
}