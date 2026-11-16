package co.edu.uniquindio.poo.ui;

import co.edu.uniquindio.poo.modelo.TipoTransaccion;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProgramadaRequest(
        LocalDateTime fechaEjecucion,
        TipoTransaccion tipo,
        String codigoOrigen,
        String codigoDestino,
        BigDecimal valor) {
}