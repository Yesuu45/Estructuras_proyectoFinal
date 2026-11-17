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

    // ---------- Ciclos y rutas ----------

    /** Tope de pasos de exploración, para que un grafo muy denso no deje la consulta colgada. */
    private static final int PRESUPUESTO_PASOS = 500_000;

    /**
     * Ciclos simples (sin repetir vértices) del grafo dirigido, por búsqueda en profundidad.
     * Cada ciclo sale una sola vez, empezando por su vértice de menor orden interno.
     * Se detiene al llegar a 'limite' ciclos. Cada ciclo va sin repetir el vértice inicial al final.
     */
    public SimpleLinkedList<SimpleLinkedList<V>> ciclos(int limite) {
        SimpleLinkedList<SimpleLinkedList<V>> encontrados = new SimpleLinkedList<>();
        TablaHash<V, Integer> orden = ordenDeVertices();
        int[] pasos = {0};
        Cola<V> inicios = vertices();
        while (!inicios.isEmpty() && encontrados.getSize() < limite && pasos[0] < PRESUPUESTO_PASOS) {
            V inicio = inicios.desencolar();
            Stack<V> camino = new Stack<>();
            TablaHash<V, Boolean> enCamino = new TablaHash<>();
            camino.push(inicio);
            enCamino.put(inicio, Boolean.TRUE);
            buscarCiclos(inicio, inicio, orden, camino, enCamino, encontrados, limite, pasos);
        }
        return encontrados;
    }

    private void buscarCiclos(V inicio, V actual, TablaHash<V, Integer> orden, Stack<V> camino,
                              TablaHash<V, Boolean> enCamino, SimpleLinkedList<SimpleLinkedList<V>> encontrados,
                              int limite, int[] pasos) {
        for (NodeSimple<Arista<V>> nodo = vecinos(actual).getFirstNode(); nodo != null; nodo = nodo.getNext()) {
            if (encontrados.getSize() >= limite || ++pasos[0] > PRESUPUESTO_PASOS) {
                return;
            }
            V siguiente = nodo.getData().getDestino();
            if (siguiente.equals(inicio)) {
                encontrados.addLast(copiarCamino(camino));
            } else if (orden.get(siguiente) > orden.get(inicio) && !enCamino.containsKey(siguiente)) {
                camino.push(siguiente);
                enCamino.put(siguiente, Boolean.TRUE);
                buscarCiclos(inicio, siguiente, orden, camino, enCamino, encontrados, limite, pasos);
                enCamino.remove(siguiente);
                camino.pop();
            }
        }
    }

    /**
     * Caminos simples (sin repetir vértices) con entre 'minSaltos' y 'maxSaltos' aristas.
     * Sirve para encontrar rutas de dinero de varios pasos. Se detiene al llegar a 'limite' caminos.
     */
    public SimpleLinkedList<SimpleLinkedList<V>> caminos(int minSaltos, int maxSaltos, int limite) {
        if (minSaltos < 1 || maxSaltos < minSaltos) {
            throw new IllegalArgumentException("El rango de saltos no es válido");
        }
        SimpleLinkedList<SimpleLinkedList<V>> encontrados = new SimpleLinkedList<>();
        int[] pasos = {0};
        Cola<V> inicios = vertices();
        while (!inicios.isEmpty() && encontrados.getSize() < limite && pasos[0] < PRESUPUESTO_PASOS) {
            V inicio = inicios.desencolar();
            Stack<V> camino = new Stack<>();
            TablaHash<V, Boolean> enCamino = new TablaHash<>();
            camino.push(inicio);
            enCamino.put(inicio, Boolean.TRUE);
            buscarCaminos(inicio, 0, minSaltos, maxSaltos, camino, enCamino, encontrados, limite, pasos);
        }
        return encontrados;
    }

    private void buscarCaminos(V actual, int saltos, int minSaltos, int maxSaltos, Stack<V> camino,
                               TablaHash<V, Boolean> enCamino, SimpleLinkedList<SimpleLinkedList<V>> encontrados,
                               int limite, int[] pasos) {
        if (saltos >= minSaltos) {
            encontrados.addLast(copiarCamino(camino));
        }
        if (saltos == maxSaltos) {
            return;
        }
        for (NodeSimple<Arista<V>> nodo = vecinos(actual).getFirstNode(); nodo != null; nodo = nodo.getNext()) {
            if (encontrados.getSize() >= limite || ++pasos[0] > PRESUPUESTO_PASOS) {
                return;
            }
            V siguiente = nodo.getData().getDestino();
            if (!enCamino.containsKey(siguiente)) {
                camino.push(siguiente);
                enCamino.put(siguiente, Boolean.TRUE);
                buscarCaminos(siguiente, saltos + 1, minSaltos, maxSaltos, camino, enCamino, encontrados, limite, pasos);
                enCamino.remove(siguiente);
                camino.pop();
            }
        }
    }

    private TablaHash<V, Integer> ordenDeVertices() {
        TablaHash<V, Integer> orden = new TablaHash<>();
        Cola<V> claves = vertices();
        int indice = 0;
        while (!claves.isEmpty()) {
            orden.put(claves.desencolar(), indice++);
        }
        return orden;
    }

    /** El camino sale de la pila de arriba hacia abajo; se invierte para dejarlo en orden de recorrido. */
    private SimpleLinkedList<V> copiarCamino(Stack<V> camino) {
        Stack<V> copia = camino.clonStack();
        SimpleLinkedList<V> lista = new SimpleLinkedList<>();
        while (!copia.isEmpty()) {
            lista.addFirst(copia.pop());
        }
        return lista;
    }

    private static void validar(Object valor, String nombre) {
        if (valor == null) {
            throw new IllegalArgumentException(nombre + " no puede ser nulo");
        }
    }
}
