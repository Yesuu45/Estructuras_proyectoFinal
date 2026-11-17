package co.edu.uniquindio.poo.servicio;

import co.edu.uniquindio.poo.modelo.Nivel;

/** Cuántos usuarios hay en un nivel. maximo es null en el último nivel (sin tope). */
public record ResumenNivel(Nivel nivel, int minimo, Integer maximo, int cantidad) {
}
