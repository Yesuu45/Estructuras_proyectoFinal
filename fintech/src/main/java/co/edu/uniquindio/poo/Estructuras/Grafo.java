package co.edu.uniquindio.poo.Estructuras;

import java.math.BigDecimal;

/**
 * Grafo dirigido y ponderado, representado con listas de adyacencia.
 * Cada vértice (TablaHash: acceso O(1)) guarda una lista enlazada con sus aristas de salida.
 * Una transferencia A -> B suma una vez a la arista A -> B: así el grafo resume el flujo de dinero.
 * No usa colecciones de java.util: solo TablaHash, SimpleLinkedList y Cola propias.
 */
public class Grafo<V> {

    private final TablaHash<V, SimpleLinkedList<Arista<V>>> adyacencia = new TablaHash<>();
    private int numeroAristas = 0;

    public void agregarVertice(V vertice) {
        validar(vertice, "El vértice");
        if (!adyacencia.containsKey(vertice)) {
            adyacencia.put(vertice, new SimpleLinkedList<>());
        }
    }

    /** Registra un movimiento origen -> destino. Si la arista ya existe, acumula; si no, la crea. */
    public void agregarArista(V origen, V destino, BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero");
        }
        agregarVertice(origen);
        agregarVertice(destino);
        Arista<V> existente = getArista(origen, destino);
        if (existente != null) {
            existente.sumar(monto);
        } else {
            adyacencia.get(origen).addLast(new Arista<>(origen, destino, monto));
            numeroAristas++;
        }
    }

    /** Deshace un movimiento (por ejemplo, una reversión). Si la arista queda sin movimientos, desaparece. */
    public boolean restarArista(V origen, V destino, BigDecimal monto) {
        Arista<V> arista = getArista(origen, destino);
        if (arista == null) {
            return false;
        }
        arista.restar(monto);
        if (arista.getCantidad() <= 0) {
            adyacencia.get(origen).remove(arista);
            numeroAristas--;
        }
        return true;
    }

    /** Quita un vértice. Solo si no tiene aristas, para no dejar el grafo con extremos sueltos. */
    public boolean eliminarVertice(V vertice) {
        if (!existeVertice(vertice)) {
            return false;
        }
        if (gradoSalida(vertice) > 0 || gradoEntrada(vertice) > 0) {
            throw new IllegalStateException("El vértice " + vertice + " todavía tiene aristas");
        }
        adyacencia.remove(vertice);
        return true;
    }

    public boolean existeVertice(V vertice) {
        return adyacencia.containsKey(vertice);
    }

    public boolean existeArista(V origen, V destino) {
        return getArista(origen, destino) != null;
    }

    public Arista<V> getArista(V origen, V destino) {
        SimpleLinkedList<Arista<V>> salientes = adyacencia.get(origen);
        if (salientes == null) {
            return null;
        }
        for (NodeSimple<Arista<V>> nodo = salientes.getFirstNode(); nodo != null; nodo = nodo.getNext()) {
            if (nodo.getData().getDestino().equals(destino)) {
                return nodo.getData();
            }
        }
        return null;
    }

    /** Aristas que salen del vértice (a quién le ha enviado dinero). Lista vacía si no existe. */
    public SimpleLinkedList<Arista<V>> vecinos(V vertice) {
        SimpleLinkedList<Arista<V>> salientes = adyacencia.get(vertice);
        return salientes == null ? new SimpleLinkedList<>() : salientes;
    }

    public int gradoSalida(V vertice) {
        return vecinos(vertice).getSize();
    }

    /** Cuántos vértices le envían a este. Recorre todo el grafo: O(V + E). */
    public int gradoEntrada(V vertice) {
        int grado = 0;
        Cola<V> claves = adyacencia.claves();
        while (!claves.isEmpty()) {
            if (getArista(claves.desencolar(), vertice) != null) {
                grado++;
            }
        }
        return grado;
    }

    public int numeroVertices() {
        return adyacencia.size();
    }

    public int numeroAristas() {
        return numeroAristas;
    }

    public Cola<V> vertices() {
        return adyacencia.claves();
    }

    public SimpleLinkedList<Arista<V>> aristas() {
        SimpleLinkedList<Arista<V>> todas = new SimpleLinkedList<>();
        Cola<V> claves = adyacencia.claves();
        while (!claves.isEmpty()) {
            SimpleLinkedList<Arista<V>> salientes = adyacencia.get(claves.desencolar());
            for (NodeSimple<Arista<V>> nodo = salientes.getFirstNode(); nodo != null; nodo = nodo.getNext()) {
                todas.addLast(nodo.getData());
            }
        }
        return todas;
    }

    /**
     * Ruta con menos saltos de origen a destino (recorrido en amplitud con una cola).
     * Devuelve null si no hay ruta o algún vértice no existe.
     */
    public SimpleLinkedList<V> caminoMasCorto(V origen, V destino) {
        if (!existeVertice(origen) || !existeVertice(destino)) {
            return null;
        }
        TablaHash<V, V> padre = new TablaHash<>();      // vértice -> de dónde se llegó; también marca los visitados
        Cola<V> pendientes = new Cola<>();
        padre.put(origen, origen);
        pendientes.encolar(origen);
        while (!pendientes.isEmpty() && !padre.containsKey(destino)) {
            V actual = pendientes.desencolar();
            for (NodeSimple<Arista<V>> nodo = vecinos(actual).getFirstNode(); nodo != null; nodo = nodo.getNext()) {
                V siguiente = nodo.getData().getDestino();
                if (!padre.containsKey(siguiente)) {
                    padre.put(siguiente, actual);
                    pendientes.encolar(siguiente);
                }
            }
        }
        if (!padre.containsKey(destino)) {
            return null;
        }
        SimpleLinkedList<V> camino = new SimpleLinkedList<>();
        for (V paso = destino; ; paso = padre.get(paso)) {
            camino.addFirst(paso);
            if (paso.equals(origen)) {
                break;
            }
        }
        return camino;
    }

    private static void validar(Object valor, String nombre) {
        if (valor == null) {
            throw new IllegalArgumentException(nombre + " no puede ser nulo");
        }
    }
}
