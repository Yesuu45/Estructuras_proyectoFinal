package co.edu.uniquindio.poo.Estructuras;

import java.math.BigDecimal;

/**
 * Arista dirigida de un grafo (origen -> destino) con dos pesos: cuántas veces se repitió
 * y el monto total acumulado. Sirve para saber qué rutas de dinero son más frecuentes.
 */
public class Arista<V> {

    private final V origen;
    private final V destino;
    private int cantidad;
    private BigDecimal total;

    public Arista(V origen, V destino, BigDecimal monto) {
        this.origen = origen;
        this.destino = destino;
        this.cantidad = 1;
        this.total = monto;
    }

    void sumar(BigDecimal monto) {
        cantidad++;
        total = total.add(monto);
    }

    void restar(BigDecimal monto) {
        cantidad--;
        total = total.subtract(monto).max(BigDecimal.ZERO);
    }

    public V getOrigen() {
        return origen;
    }

    public V getDestino() {
        return destino;
    }

    public int getCantidad() {
        return cantidad;
    }

    public BigDecimal getTotal() {
        return total;
    }

    @Override
    public String toString() {
        return origen + " -> " + destino + " (" + cantidad + " veces, total " + total + ")";
    }
}
