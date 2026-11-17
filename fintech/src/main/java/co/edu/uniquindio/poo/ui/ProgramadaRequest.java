package co.edu.uniquindio.poo.ui;

import co.edu.uniquindio.poo.modelo.Periodicidad;
import co.edu.uniquindio.poo.modelo.TipoTransaccion;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** periodicidad y ocurrencias son opcionales: sin ellas la operación se ejecuta una sola vez. */
public record ProgramadaRequest(
        LocalDateTime fechaEjecucion,
        TipoTransaccion tipo,
        String codigoOrigen,
        String codigoDestino,
        BigDecimal valor,
        Periodicidad periodicidad,
        Integer ocurrencias) {
}
