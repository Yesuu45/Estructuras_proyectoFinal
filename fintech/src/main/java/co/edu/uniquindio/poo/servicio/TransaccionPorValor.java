package co.edu.uniquindio.poo.servicio;

import co.edu.uniquindio.poo.modelo.Transaccion;

/**
 * Envoltorio para ordenar transacciones en el montículo (que saca primero el "menor"):
 * el "menor" es el de mayor valor; si empatan, el más antiguo y luego el id.
 */
class TransaccionPorValor implements Comparable<TransaccionPorValor> {

    private final Transaccion transaccion;

    TransaccionPorValor(Transaccion transaccion) {
        this.transaccion = transaccion;
    }

    Transaccion transaccion() {
        return transaccion;
    }

    @Override
    public int compareTo(TransaccionPorValor otra) {
        int comparacion = otra.transaccion.getValor().compareTo(transaccion.getValor());
        if (comparacion == 0) {
            comparacion = transaccion.getFecha().compareTo(otra.transaccion.getFecha());
        }
        if (comparacion == 0) {
            comparacion = transaccion.getId().compareTo(otra.transaccion.getId());
        }
        return comparacion;
    }
}
