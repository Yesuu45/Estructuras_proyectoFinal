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
    private final Periodicidad periodicidad;
    private final int repeticionesRestantes;   // ocurrencias que faltan DESPUÉS de esta
    private boolean avisada;                   // ya se avisó que está próxima a ejecutarse

    public OperacionProgramada(String id, LocalDateTime fechaEjecucion, TipoTransaccion tipo,
                               BigDecimal valor, String codigoOrigen, String codigoDestino) {
        this(id, fechaEjecucion, tipo, valor, codigoOrigen, codigoDestino, Periodicidad.UNICA, 0);
    }

    public OperacionProgramada(String id, LocalDateTime fechaEjecucion, TipoTransaccion tipo,
                               BigDecimal valor, String codigoOrigen, String codigoDestino,
                               Periodicidad periodicidad, int repeticionesRestantes) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id es obligatorio");
        }
        if (fechaEjecucion == null) {
            throw new IllegalArgumentException("La fecha de ejecución es obligatoria");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo es obligatorio");
        }
        if (tipo == TipoTransaccion.RETIRO) {
            throw new IllegalArgumentException("Solo se pueden programar recargas, transferencias o pagos");
        }
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("El valor debe ser mayor que cero");
        }
        if (tipo == TipoTransaccion.RECARGA) {
            if (codigoDestino == null) {
                throw new IllegalArgumentException("La recarga programada necesita billetera destino");
            }
        } else if (codigoOrigen == null || codigoDestino == null) {
            throw new IllegalArgumentException("La operación necesita billetera origen y destino");
        }
        if (periodicidad == null) {
            periodicidad = Periodicidad.UNICA;
        }
        if (repeticionesRestantes < 0 || (periodicidad == Periodicidad.UNICA && repeticionesRestantes > 0)) {
            throw new IllegalArgumentException("Las repeticiones no son válidas para la periodicidad elegida");
        }
        this.periodicidad = periodicidad;
        this.repeticionesRestantes = repeticionesRestantes;
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

    public void marcarAvisada() {
        avisada = true;
    }

    public boolean isAvisada() {
        return avisada;
    }

    public Periodicidad getPeriodicidad() {
        return periodicidad;
    }

    public int getRepeticionesRestantes() {
        return repeticionesRestantes;
    }

    public boolean tieneSiguiente() {
        return repeticionesRestantes > 0;
    }

    /** Siguiente ocurrencia de la serie (mantiene el ritmo a partir de la fecha programada, no de la de ejecución). */
    public OperacionProgramada crearSiguiente(String nuevoId) {
        if (!tieneSiguiente()) {
            throw new IllegalStateException("La operación no tiene más repeticiones");
        }
        return new OperacionProgramada(nuevoId, periodicidad.siguiente(fechaEjecucion), tipo, valor,
                codigoOrigen, codigoDestino, periodicidad, repeticionesRestantes - 1);
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