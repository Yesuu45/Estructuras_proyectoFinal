package co.edu.uniquindio.poo.ui;

import co.edu.uniquindio.poo.modelo.TipoBilletera;
import java.math.BigDecimal;

public record BilleteraRequest(
        String codigo,
        String nombre,
        TipoBilletera tipo,
        String identificacionTitular,
        BigDecimal saldoInicial,
        BigDecimal umbralSaldoBajo) {
}
