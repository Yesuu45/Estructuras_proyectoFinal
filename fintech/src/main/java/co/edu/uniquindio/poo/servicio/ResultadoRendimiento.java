package co.edu.uniquindio.poo.servicio;

/** Una medición: una operación sobre una estructura (propia o de Java), con su tiempo y su costo teórico. */
public record ResultadoRendimiento(String operacion, String estructura, String origen, int operaciones,
                                   long nanosTotal, long nanosPorOperacion, String complejidad) {
}
