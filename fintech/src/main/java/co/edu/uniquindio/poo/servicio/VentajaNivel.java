package co.edu.uniquindio.poo.servicio;

import co.edu.uniquindio.poo.modelo.Nivel;

import java.math.BigDecimal;

/** Lo que otorga un nivel: rango de puntos y límite por operación de salida (null = sin límite). */
public record VentajaNivel(Nivel nivel, int puntosMinimo, Integer puntosMaximo, BigDecimal limiteOperacion) {
}
