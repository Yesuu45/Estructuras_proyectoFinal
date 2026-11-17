package co.edu.uniquindio.poo.Estructuras;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de pila, lista enlazada, árbol binario de búsqueda y grafo. */
class EstructurasBasicasTest {

    @Test
    void pilaDesapilaEnOrdenInverso() {
        Stack<String> pila = new Stack<>();
        pila.push("a");
        pila.push("b");
        pila.push("c");
        assertEquals("c", pila.peek());
        assertEquals("c", pila.pop());
        assertEquals("b", pila.pop());
        assertEquals(1, pila.getSize());
    }

    @Test
    void pilaEliminaUnElementoIntermedio() {
        Stack<Integer> pila = new Stack<>();
        pila.push(1);
        pila.push(2);
        pila.push(3);
        assertTrue(pila.eliminar(2));
        assertEquals(3, pila.pop());
        assertEquals(1, pila.pop());
        assertTrue(pila.isEmpty());
    }

    @Test
    void listaAgregaAlFinalYBusca() {
        SimpleLinkedList<Integer> lista = new SimpleLinkedList<>();
        lista.addLast(10);
        lista.addLast(20);
        lista.addFirst(5);
        assertEquals(3, lista.getSize());
        assertEquals(5, lista.getNodeValue(0));
        assertEquals(20, lista.getNodeValue(2));
        assertTrue(lista.contains(10));
        assertTrue(lista.remove(10));
        assertFalse(lista.contains(10));
    }

    @Test
    void arbolEntregaInordenDescendenteYRango() {
        ArbolBinario<Integer> arbol = new ArbolBinario<>();
        for (int n : new int[]{50, 30, 70, 20, 40, 60, 80}) {
            arbol.insertar(n);
        }
        SimpleLinkedList<Integer> descendente = arbol.inordenDescendente();
        assertEquals(80, descendente.getNodeValue(0));
        assertEquals(20, descendente.getNodeValue(6));

        SimpleLinkedList<Integer> rango = arbol.enRango(35, 65);
        assertEquals(3, rango.getSize());
        assertTrue(rango.contains(40) && rango.contains(50) && rango.contains(60));
    }

    @Test
    void arbolEliminaYReinsertaUnElemento() {
        ArbolBinario<Integer> arbol = new ArbolBinario<>();
        for (int n : new int[]{50, 30, 70}) {
            arbol.insertar(n);
        }
        arbol.eliminar(50);
        assertFalse(arbol.existe(50));
        arbol.insertar(90);
        assertTrue(arbol.existe(90));
        assertEquals(3, arbol.inorden().getSize());
    }

    @Test
    void arbolConClavesOrdenadasSeDegenera() {
        ArbolBinario<Integer> arbol = new ArbolBinario<>();
        for (int i = 0; i < 50; i++) {
            arbol.insertar(i);
        }
        assertTrue(arbol.ObtenerAltura() >= 50, "sin balanceo, la altura crece con n");
    }

    @Test
    void grafoEncuentraElCaminoMasCorto() {
        Grafo<String> grafo = new Grafo<>();
        for (String v : new String[]{"A", "B", "C", "D"}) {
            grafo.agregarVertice(v);
        }
        grafo.agregarArista("A", "B", BigDecimal.TEN);
        grafo.agregarArista("B", "C", BigDecimal.TEN);
        grafo.agregarArista("A", "D", BigDecimal.TEN);
        grafo.agregarArista("D", "C", BigDecimal.TEN);
        SimpleLinkedList<String> camino = grafo.caminoMasCorto("A", "C");
        assertEquals(3, camino.getSize());
        assertEquals("A", camino.getNodeValue(0));
        assertEquals("C", camino.getNodeValue(2));
    }

    @Test
    void grafoAcumulaMontoYLoResta() {
        Grafo<String> grafo = new Grafo<>();
        grafo.agregarVertice("A");
        grafo.agregarVertice("B");
        grafo.agregarArista("A", "B", new BigDecimal("100"));
        grafo.agregarArista("A", "B", new BigDecimal("50"));
        assertEquals(1, grafo.numeroAristas());
        assertEquals(0, new BigDecimal("150").compareTo(grafo.getArista("A", "B").getTotal()));
        grafo.restarArista("A", "B", new BigDecimal("100"));
        assertTrue(grafo.existeArista("A", "B"), "queda una de las dos transferencias");
        grafo.restarArista("A", "B", new BigDecimal("50"));
        assertFalse(grafo.existeArista("A", "B"));
    }

    @Test
    void grafoDetectaUnCiclo() {
        Grafo<String> grafo = new Grafo<>();
        for (String v : new String[]{"A", "B", "C"}) {
            grafo.agregarVertice(v);
        }
        grafo.agregarArista("A", "B", BigDecimal.ONE);
        grafo.agregarArista("B", "C", BigDecimal.ONE);
        grafo.agregarArista("C", "A", BigDecimal.ONE);
        assertEquals(1, grafo.ciclos(100).getSize());
    }

    @Test
    void tablaHashSeRedimensionaSinPerderClaves() {
        TablaHash<Integer, Integer> tabla = new TablaHash<>();
        for (int i = 0; i < 1000; i++) {
            tabla.put(i, i * 2);
        }
        for (int i = 0; i < 1000; i++) {
            assertEquals(i * 2, tabla.get(i));
        }
        assertEquals(1000, tabla.claves().getSize());
    }
}
