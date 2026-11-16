package co.edu.uniquindio.poo.ui;

import co.edu.uniquindio.poo.modelo.TipoTransaccion;
import java.math.BigDecimal;

public record TransaccionRequest(
        TipoTransaccion tipo,
        String codigoOrigen,
        String codigoDestino,
        BigDecimal valor) {
}