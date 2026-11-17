package co.edu.uniquindio.poo.servicio;

import java.math.BigDecimal;

/** Una flecha del grafo de transferencias: cuántas veces y cuánto dinero ha ido de origen a destino. */
public record AristaRed(String origen, String destino, int cantidad, BigDecimal total) {
}
