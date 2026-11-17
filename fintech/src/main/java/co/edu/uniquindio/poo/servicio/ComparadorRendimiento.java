package co.edu.uniquindio.poo.servicio;

import co.edu.uniquindio.poo.Estructuras.*;
import co.edu.uniquindio.poo.Estructuras.Stack;

import java.util.*;

/**
 * Compara el rendimiento de las estructuras propias del proyecto con sus equivalentes de Java en operaciones
 * parecidas (insertar, buscar, obtener el menor, sacar todo en orden, apilar/encolar). Mide con nanoTime sobre
 * claves enteras en orden aleatorio, tras una pasada de calentamiento. Los tiempos son orientativos: dependen de
 * la máquina, pero muestran cómo crece cada estructura (por ejemplo, la búsqueda en la lista contra la tabla hash).
 */
public final class ComparadorRendimiento {

    public static final int MIN_ELEMENTOS = 100;
    public static final int MAX_ELEMENTOS = 20_000;
    private static final int BUSQUEDAS = 1000;
    private static final int PEOR_CASO_MAXIMO = 2000;   // el árbol con claves ordenadas es recursivo y lineal: se acota

    private final SimpleLinkedList<ResultadoRendimiento> resultados = new SimpleLinkedList<>();
    private final Integer[] claves;
    private final Integer[] consultas;
    private final int n;
    private final boolean medir;

    private ComparadorRendimiento(int n, boolean medir) {
        this.n = n;
        this.medir = medir;
        Random azar = new Random(42);
        claves = new Integer[n];
        for (int i = 0; i < n; i++) {
            claves[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {      // barajar: claves únicas en orden aleatorio
            int j = azar.nextInt(i + 1);
            Integer tmp = claves[i];
            claves[i] = claves[j];
            claves[j] = tmp;
        }
        consultas = new Integer[Math.min(n, BUSQUEDAS)];
        for (int i = 0; i < consultas.length; i++) {
            consultas[i] = claves[azar.nextInt(n)];
        }
    }

    public static ResultadoRendimiento[] comparar(int elementos) {
        if (elementos < MIN_ELEMENTOS || elementos > MAX_ELEMENTOS) {
            throw new IllegalArgumentException("La cantidad de elementos debe estar entre " + MIN_ELEMENTOS
                    + " y " + MAX_ELEMENTOS);
        }
        new ComparadorRendimiento(MIN_ELEMENTOS, false).ejecutar();   // calentamiento: no se registra
        ComparadorRendimiento real = new ComparadorRendimiento(elementos, true);
        real.ejecutar();
        ResultadoRendimiento[] salida = new ResultadoRendimiento[real.resultados.getSize()];
        int i = 0;
        for (NodeSimple<ResultadoRendimiento> nodo = real.resultados.getFirstNode(); nodo != null; nodo = nodo.getNext()) {
            salida[i++] = nodo.getData();
        }
        return salida;
    }

    private void ejecutar() {
        // Estructuras ya cargadas para las búsquedas y el "menor"
        SimpleLinkedList<Integer> lista = new SimpleLinkedList<>();
        TablaHash<Integer, Integer> hash = new TablaHash<>();
        ArbolBinario<Integer> arbol = new ArbolBinario<>();
        MonticuloBinario<Integer> monticulo = new MonticuloBinario<>();
        ArrayList<Integer> arrayList = new ArrayList<>();
        HashMap<Integer, Integer> hashMap = new HashMap<>();
        TreeMap<Integer, Integer> treeMap = new TreeMap<>();
        PriorityQueue<Integer> prioridad = new PriorityQueue<>();

        String insertar = "Insertar " + n + " claves";
        medir(insertar, "Lista enlazada propia (al final)", "Propia", n, "O(1) cada una", () -> { for (Integer c : claves) lista.addLast(c); });
        medir(insertar, "Tabla hash propia", "Propia", n, "O(1) promedio", () -> { for (Integer c : claves) hash.put(c, c); });
        medir(insertar, "Árbol binario de búsqueda propio", "Propia", n, "O(log n) si está equilibrado", () -> { for (Integer c : claves) arbol.insertar(c); });
        medir(insertar, "Montículo binario propio", "Propia", n, "O(log n)", () -> { for (Integer c : claves) monticulo.insertar(c); });
        medir(insertar, "ArrayList de Java", "Java", n, "O(1) amortizado", () -> { for (Integer c : claves) arrayList.add(c); });
        medir(insertar, "HashMap de Java", "Java", n, "O(1) promedio", () -> { for (Integer c : claves) hashMap.put(c, c); });
        medir(insertar, "TreeMap de Java (árbol rojo-negro)", "Java", n, "O(log n) siempre", () -> { for (Integer c : claves) treeMap.put(c, c); });
        medir(insertar, "PriorityQueue de Java", "Java", n, "O(log n)", () -> { for (Integer c : claves) prioridad.add(c); });

        int m = consultas.length;
        String buscar = "Buscar " + m + " claves";
        int[] aciertos = {0};
        medir(buscar, "Lista enlazada propia", "Propia", m, "O(n) cada una", () -> { for (Integer c : consultas) if (lista.contains(c)) aciertos[0]++; });
        medir(buscar, "Tabla hash propia", "Propia", m, "O(1) promedio", () -> { for (Integer c : consultas) if (hash.containsKey(c)) aciertos[0]++; });
        medir(buscar, "Árbol binario de búsqueda propio", "Propia", m, "O(log n) si está equilibrado", () -> { for (Integer c : consultas) if (arbol.existe(c)) aciertos[0]++; });
        medir(buscar, "ArrayList de Java", "Java", m, "O(n) cada una", () -> { for (Integer c : consultas) if (arrayList.contains(c)) aciertos[0]++; });
        medir(buscar, "HashMap de Java", "Java", m, "O(1) promedio", () -> { for (Integer c : consultas) if (hashMap.containsKey(c)) aciertos[0]++; });
        medir(buscar, "TreeMap de Java", "Java", m, "O(log n) siempre", () -> { for (Integer c : consultas) if (treeMap.containsKey(c)) aciertos[0]++; });

        // Peor caso del árbol sin balancear: claves ya ordenadas lo convierten en una lista
        int p = Math.min(n, PEOR_CASO_MAXIMO);
        ArbolBinario<Integer> degenerado = new ArbolBinario<>();
        String peor = "Árbol con claves ya ordenadas (" + p + " claves)";
        medir(peor, "Insertar " + p + " claves ordenadas", "Propia", p, "O(n) cada una: degenera en lista", () -> { for (int i = 0; i < p; i++) degenerado.insertar(i); });
        medir(peor, "Buscar " + Math.min(m, p) + " claves", "Propia", Math.min(m, p), "O(n) cada una", () -> { for (int i = 0; i < Math.min(m, p); i++) if (degenerado.existe(consultas[i] % p)) aciertos[0]++; });

        String menor = "Obtener el menor (" + BUSQUEDAS + " veces)";
        int[] suma = {0};
        medir(menor, "Montículo binario propio (peek)", "Propia", BUSQUEDAS, "O(1)", () -> { for (int i = 0; i < BUSQUEDAS; i++) suma[0] += monticulo.peek(); });
        medir(menor, "Árbol binario de búsqueda propio", "Propia", BUSQUEDAS, "O(altura)", () -> { for (int i = 0; i < BUSQUEDAS; i++) suma[0] += arbol.obtenerMenor().getElemento(); });
        medir(menor, "Lista enlazada propia (recorrer)", "Propia", BUSQUEDAS, "O(n) cada una", () -> {
            for (int i = 0; i < BUSQUEDAS; i++) {
                int min = Integer.MAX_VALUE;
                for (NodeSimple<Integer> nodo = lista.getFirstNode(); nodo != null; nodo = nodo.getNext()) {
                    if (nodo.getData() < min) min = nodo.getData();
                }
                suma[0] += min;
            }
        });
        medir(menor, "PriorityQueue de Java (peek)", "Java", BUSQUEDAS, "O(1)", () -> { for (int i = 0; i < BUSQUEDAS; i++) suma[0] += prioridad.peek(); });
        medir(menor, "TreeMap de Java (firstKey)", "Java", BUSQUEDAS, "O(log n)", () -> { for (int i = 0; i < BUSQUEDAS; i++) suma[0] += treeMap.firstKey(); });

        String todo = "Sacar las " + n + " claves en orden ascendente";
        medir(todo, "Montículo binario propio (extraer uno a uno)", "Propia", n, "O(n log n)", () -> { while (!monticulo.isEmpty()) suma[0] += monticulo.extraerMinimo(); });
        medir(todo, "Árbol binario propio (recorrido inorden)", "Propia", n, "O(n)", () -> suma[0] += arbol.inorden().getSize());
        medir(todo, "PriorityQueue de Java (poll)", "Java", n, "O(n log n)", () -> { while (!prioridad.isEmpty()) suma[0] += prioridad.poll(); });
        medir(todo, "TreeMap de Java (recorrido)", "Java", n, "O(n)", () -> { for (Integer c : treeMap.keySet()) suma[0] += c; });

        String pila = "Apilar y desapilar " + n + " elementos";
        medir(pila, "Pila propia", "Propia", n * 2, "O(1) cada una", () -> {
            Stack<Integer> propia = new Stack<>();
            for (Integer c : claves) propia.push(c);
            while (!propia.isEmpty()) suma[0] += propia.pop();
        });
        medir(pila, "ArrayDeque de Java", "Java", n * 2, "O(1) cada una", () -> {
            ArrayDeque<Integer> java = new ArrayDeque<>();
            for (Integer c : claves) java.push(c);
            while (!java.isEmpty()) suma[0] += java.pop();
        });

        String cola = "Encolar y desencolar " + n + " elementos";
        medir(cola, "Cola propia", "Propia", n * 2, "O(1) cada una", () -> {
            Cola<Integer> propia = new Cola<>();
            for (Integer c : claves) propia.encolar(c);
            while (!propia.isEmpty()) suma[0] += propia.desencolar();
        });
        medir(cola, "ArrayDeque de Java", "Java", n * 2, "O(1) cada una", () -> {
            ArrayDeque<Integer> java = new ArrayDeque<>();
            for (Integer c : claves) java.addLast(c);
            while (!java.isEmpty()) suma[0] += java.pollFirst();
        });
        if (suma[0] == Integer.MIN_VALUE && aciertos[0] < 0) {
            throw new IllegalStateException();   // usa los acumuladores para que el compilador no elimine el trabajo
        }
    }

    private void medir(String operacion, String estructura, String origen, int operaciones, String complejidad,
                       Runnable trabajo) {
        long inicio = System.nanoTime();
        trabajo.run();
        long total = System.nanoTime() - inicio;
        if (medir) {
            resultados.addLast(new ResultadoRendimiento(operacion, estructura, origen, operaciones, total,
                    total / Math.max(1, operaciones), complejidad));
        }
    }
}
