package co.edu.uniquindio.poo.servicio;

import java.math.BigDecimal;

/**
 * Un ciclo o una ruta del grafo de transferencias. 'frecuencia' es el número de veces de la arista MENOS usada
 * del recorrido (cuántas veces se pudo completar la ruta como mínimo) y 'total' suma el dinero de sus aristas.
 */
public record RutaFrecuente(String[] vertices, int frecuencia, BigDecimal total) {
}
