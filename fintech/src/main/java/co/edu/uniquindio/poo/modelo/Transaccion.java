package co.edu.uniquindio.poo.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaccion {

    private final String id;
    private final LocalDateTime fecha;
    private final TipoTransaccion tipo;
    private final BigDecimal valor;
    private final String codigoOrigen;
    private final String codigoDestino;
    private EstadoTransaccion estado;
    private int puntosGenerados;

    public Transaccion(String id, LocalDateTime fecha, TipoTransaccion tipo, BigDecimal valor,
                       String codigoOrigen, String codigoDestino) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id es obligatorio");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo es obligatorio");
        }
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("El valor debe ser mayor que cero");
        }
        validarOrigenDestino(tipo, codigoOrigen, codigoDestino);

        this.id = id.trim();
        this.fecha = fecha == null ? LocalDateTime.now() : fecha;
        this.tipo = tipo;
        this.valor = valor;
        this.codigoOrigen = codigoOrigen;
        this.codigoDestino = codigoDestino;
        this.estado = EstadoTransaccion.COMPLETADA;
        this.puntosGenerados = 0;
    }

    public void marcarRevertida() {
        if (estado != EstadoTransaccion.COMPLETADA) {
            throw new IllegalStateException("Solo se puede revertir una transacción completada");
        }
        estado = EstadoTransaccion.REVERTIDA;
    }

    public void setPuntosGenerados(int puntosGenerados) {
        if (puntosGenerados < 0) {
            throw new IllegalArgumentException("Los puntos no pueden ser negativos");
        }
        this.puntosGenerados = puntosGenerados;
    }

    private static void validarOrigenDestino(TipoTransaccion tipo, String origen, String destino) {
        switch (tipo) {
            case RECARGA -> {
                if (destino == null) throw new IllegalArgumentException("La recarga necesita billetera destino");
            }
            case RETIRO -> {
                if (origen == null) throw new IllegalArgumentException("El retiro necesita billetera origen");
            }
            case TRANSFERENCIA, PAGO_PROGRAMADO -> {
                if (origen == null || destino == null) {
                    throw new IllegalArgumentException("La operación necesita billetera origen y destino");
                }
            }
        }
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getFecha() {
        return fecha;
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

    public EstadoTransaccion getEstado() {
        return estado;
    }

    public int getPuntosGenerados() {
        return puntosGenerados;
    }

    @Override
    public String toString() {
        return tipo + " " + valor + " [" + estado + "]";
    }
}