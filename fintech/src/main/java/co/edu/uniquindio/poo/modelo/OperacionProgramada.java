package co.edu.uniquindio.poo.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OperacionProgramada implements Comparable<OperacionProgramada> {

    private final String id;
    private final LocalDateTime fechaEjecucion;
    private final TipoTransaccion tipo;
    private final BigDecimal valor;
    private final String codigoOrigen;
    private final String codigoDestino;
    private EstadoOperacionProgramada estado;

    public OperacionProgramada(String id, LocalDateTime fechaEjecucion, TipoTransaccion tipo,
                               BigDecimal valor, String codigoOrigen, String codigoDestino) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id es obligatorio");
        }
        if (fechaEjecucion == null) {
            throw new IllegalArgumentException("La fecha de ejecución es obligatoria");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo es obligatorio");
        }
        if (tipo != TipoTransaccion.TRANSFERENCIA && tipo != TipoTransaccion.PAGO_PROGRAMADO) {
            throw new IllegalArgumentException("Solo se pueden programar transferencias o pagos");
        }
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("El valor debe ser mayor que cero");
        }
        if (codigoOrigen == null || codigoDestino == null) {
            throw new IllegalArgumentException("La operación necesita billetera origen y destino");
        }
        this.id = id.trim();
        this.fechaEjecucion = fechaEjecucion;
        this.tipo = tipo;
        this.valor = valor;
        this.codigoOrigen = codigoOrigen;
        this.codigoDestino = codigoDestino;
        this.estado = EstadoOperacionProgramada.PENDIENTE;
    }

    public void marcarEjecutada() {
        if (estado != EstadoOperacionProgramada.PENDIENTE) {
            throw new IllegalStateException("Solo una operación pendiente puede ejecutarse");
        }
        estado = EstadoOperacionProgramada.EJECUTADA;
    }

    public void marcarFallida() {
        if (estado != EstadoOperacionProgramada.PENDIENTE) {
            throw new IllegalStateException("Solo una operación pendiente puede fallar");
        }
        estado = EstadoOperacionProgramada.FALLIDA;
    }

    @Override
    public int compareTo(OperacionProgramada otra) {
        int comparacion = fechaEjecucion.compareTo(otra.fechaEjecucion);
        if (comparacion == 0) {
            comparacion = id.compareTo(otra.id);
        }
        return comparacion;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getFechaEjecucion() {
        return fechaEjecucion;
    }

    public TipoTransaccion getTipo() {
        return tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getCodigoOrigen() {
        return codigoOrigen;
    }

    public String getCodigoDestino() {
        return codigoDestino;
    }

    public EstadoOperacionProgramada getEstado() {
        return estado;
    }

    @Override
    public String toString() {
        return id + " " + tipo + " " + valor + " el " + fechaEjecucion + " [" + estado + "]";
    }
}