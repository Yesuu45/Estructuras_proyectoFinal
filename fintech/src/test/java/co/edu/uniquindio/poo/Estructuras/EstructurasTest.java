package co.edu.uniquindio.poo.Estructuras;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstructurasTest {

    @Test
    void tablaHashGuardaBuscaYElimina() {
        TablaHash<String, Integer> tabla = new TablaHash<>();
        tabla.put("a", 1);
        tabla.put("b", 2);
        assertEquals(1, tabla.get("a"));
        assertTrue(tabla.containsKey("b"));
        tabla.remove("a");
        assertFalse(tabla.containsKey("a"));
        assertEquals(1, tabla.claves().getSize());
    }

    @Test
    void colaRespetaElOrdenPrimeroEnEntrarPrimeroEnSalir() {
        Cola<Integer> cola = new Cola<>();
        cola.encolar(1);
        cola.encolar(2);
        cola.encolar(3);
        assertEquals(1, cola.desencolar());
        assertEquals(2, cola.getSize());
        assertFalse(cola.isEmpty());
    }

    @Test
    void monticuloEntregaElMinimoPrimero() {
        MonticuloBinario<Integer> monticulo = new MonticuloBinario<>();
        monticulo.insertar(5);
        monticulo.insertar(1);
        monticulo.insertar(3);
        assertEquals(3, monticulo.size());
        assertEquals(1, monticulo.peek());
        assertEquals(1, monticulo.extraerMinimo());
        assertEquals(3, monticulo.extraerMinimo());
    }
}