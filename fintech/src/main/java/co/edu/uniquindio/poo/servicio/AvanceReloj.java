package co.edu.uniquindio.poo.servicio;

import java.time.LocalDateTime;

/** Estado del reloj de la plataforma y lo que pasó al moverlo. */
public record AvanceReloj(LocalDateTime ahora, boolean simulado, int avisadas, int ejecutadas) {
}
