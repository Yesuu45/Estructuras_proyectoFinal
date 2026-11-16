package co.edu.uniquindio.poo.Estructuras;

public class MonticuloBinario<T extends Comparable<T>> {

    private static final int CAPACIDAD_INICIAL = 16;

    private T[] datos;
    private int tamano;

    @SuppressWarnings("unchecked")
    public MonticuloBinario() {
        this.datos = (T[]) new Comparable[CAPACIDAD_INICIAL];
        this.tamano = 0;
    }

    public int size() {
        return tamano;
    }

    public boolean isEmpty() {
        return tamano == 0;
    }

    public void insertar(T elemento) {
        if (elemento == null) {
            throw new IllegalArgumentException("El elemento no puede ser nulo");
        }
        if (tamano == datos.length) {
            duplicarCapacidad();
        }
        datos[tamano] = elemento;
        subir(tamano);
        tamano++;
    }

    // Devuelve el menor sin sacarlo
    public T peek() {
        if (isEmpty()) {
            throw new IllegalStateException("El montículo está vacío");
        }
        return datos[0];
    }

    // Saca y devuelve el menor
    public T extraerMinimo() {
        if (isEmpty()) {
            throw new IllegalStateException("El montículo está vacío");
        }
        T minimo = datos[0];
        tamano--;
        datos[0] = datos[tamano];
        datos[tamano] = null;
        if (tamano > 0) {
            bajar(0);
        }
        return minimo;
    }

    private void subir(int i) {
        while (i > 0) {
            int padre = (i - 1) / 2;
            if (datos[i].compareTo(datos[padre]) >= 0) {
                break;
            }
            intercambiar(i, padre);
            i = padre;
        }
    }

    private void bajar(int i) {
        while (true) {
            int izquierdo = 2 * i + 1;
            int derecho = 2 * i + 2;
            int menor = i;
            if (izquierdo < tamano && datos[izquierdo].compareTo(datos[menor]) < 0) {
                menor = izquierdo;
            }
            if (derecho < tamano && datos[derecho].compareTo(datos[menor]) < 0) {
                menor = derecho;
            }
            if (menor == i) {
                break;
            }
            intercambiar(i, menor);
            i = menor;
        }
    }

    private void intercambiar(int a, int b) {
        T temporal = datos[a];
        datos[a] = datos[b];
        datos[b] = temporal;
    }

    @SuppressWarnings("unchecked")
    private void duplicarCapacidad() {
        T[] nuevo = (T[]) new Comparable[datos.length * 2];
        for (int i = 0; i < tamano; i++) {
            nuevo[i] = datos[i];
        }
        datos = nuevo;
    }
}