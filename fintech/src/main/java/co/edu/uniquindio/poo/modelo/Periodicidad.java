package co.edu.uniquindio.poo.modelo;

import java.time.LocalDateTime;

/** Cada cuánto se repite una operación programada. UNICA se ejecuta una sola vez. */
public enum Periodicidad {
    UNICA,
    DIARIA,
    SEMANAL,
    MENSUAL;

    /** Fecha de la siguiente ocurrencia a partir de la indicada (null si no se repite). */
    public LocalDateTime siguiente(LocalDateTime desde) {
        return switch (this) {
            case UNICA -> null;
            case DIARIA -> desde.plusDays(1);
            case SEMANAL -> desde.plusWeeks(1);
            case MENSUAL -> desde.plusMonths(1);
        };
    }
}
