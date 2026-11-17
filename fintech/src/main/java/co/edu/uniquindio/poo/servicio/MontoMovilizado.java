package co.edu.uniquindio.poo.servicio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MontoMovilizado(LocalDateTime desde, LocalDateTime hasta, int transacciones, BigDecimal total) {
}
