package co.edu.uniquindio.poo.Estructuras;

public class TablaHash<K, V> {

    private static final int CAPACIDAD_INICIAL = 16;
    private static final double FACTOR_CARGA = 0.75;

    private Node<Entrada<K, V>>[] cubetas;
    private int tamano;

    public TablaHash() {
        this(CAPACIDAD_INICIAL);
    }

    @SuppressWarnings("unchecked")
    public TablaHash(int capacidad) {
        if (capacidad <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser mayor que cero");
        }
        this.cubetas = (Node<Entrada<K, V>>[]) new Node[capacidad];
        this.tamano = 0;
    }

    public int size() {
        return tamano;
    }

    public boolean isEmpty() {
        return tamano == 0;
    }

    public void put(K clave, V valor) {
        validarClave(clave);
        int indice = indice(clave, cubetas.length);
        Entrada<K, V> existente = buscarEnCubeta(cubetas[indice], clave);
        if (existente != null) {
            existente.valor = valor;
            return;
        }
        Node<Entrada<K, V>> nuevo = new Node<>(new Entrada<>(clave, valor));
        nuevo.setNext(cubetas[indice]);
        cubetas[indice] = nuevo;
        tamano++;
        if ((double) tamano / cubetas.length > FACTOR_CARGA) {
            redimensionar();
        }
    }

    public V get(K clave) {
        if (clave == null) {
            return null;
        }
        Entrada<K, V> entrada = buscarEnCubeta(cubetas[indice(clave, cubetas.length)], clave);
        return entrada == null ? null : entrada.valor;
    }

    public boolean containsKey(K clave) {
        return clave != null && buscarEnCubeta(cubetas[indice(clave, cubetas.length)], clave) != null;
    }

    public V remove(K clave) {
        if (clave == null) {
            return null;
        }
        int indice = indice(clave, cubetas.length);
        Node<Entrada<K, V>> anterior = null;
        Node<Entrada<K, V>> actual = cubetas[indice];
        while (actual != null) {
            if (actual.getData().clave.equals(clave)) {
                if (anterior == null) {
                    cubetas[indice] = actual.getNext();
                } else {
                    anterior.setNext(actual.getNext());
                }
                tamano--;
                return actual.getData().valor;
            }
            anterior = actual;
            actual = actual.getNext();
        }
        return null;
    }

    // Devuelve todas las claves en una cola propia, para recorrer sin Iterator
    public Cola<K> claves() {
        Cola<K> resultado = new Cola<>();
        for (Node<Entrada<K, V>> cabeza : cubetas) {
            for (Node<Entrada<K, V>> actual = cabeza; actual != null; actual = actual.getNext()) {
                resultado.encolar(actual.getData().clave);
            }
        }
        return resultado;
    }

    private Entrada<K, V> buscarEnCubeta(Node<Entrada<K, V>> cabeza, K clave) {
        for (Node<Entrada<K, V>> actual = cabeza; actual != null; actual = actual.getNext()) {
            if (actual.getData().clave.equals(clave)) {
                return actual.getData();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void redimensionar() {
        Node<Entrada<K, V>>[] anteriores = cubetas;
        cubetas = (Node<Entrada<K, V>>[]) new Node[anteriores.length * 2];
        for (Node<Entrada<K, V>> cabeza : anteriores) {
            Node<Entrada<K, V>> actual = cabeza;
            while (actual != null) {
                Node<Entrada<K, V>> siguiente = actual.getNext();
                int indice = indice(actual.getData().clave, cubetas.length);
                actual.setNext(cubetas[indice]);
                cubetas[indice] = actual;
                actual = siguiente;
            }
        }
    }

    private static int indice(Object clave, int longitud) {
        return (clave.hashCode() & 0x7fffffff) % longitud;
    }

    private void validarClave(K clave) {
        if (clave == null) {
            throw new IllegalArgumentException("La clave no puede ser nula");
        }
    }

    private static final class Entrada<A, B> {
        private final A clave;
        private B valor;

        Entrada(A clave, B valor) {
            this.clave = clave;
            this.valor = valor;
        }
    }
}