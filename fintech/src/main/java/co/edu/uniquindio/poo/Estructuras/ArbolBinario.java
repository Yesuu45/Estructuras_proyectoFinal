package co.edu.uniquindio.poo.Estructuras;

public class ArbolBinario<T extends Comparable<T>>{

    private int peso;
    private NodeArbol<T> raiz;

    public ArbolBinario(){
        this.peso = 0;
        this.raiz = null;
    }

    public int getPeso() {
        return peso;
    }

    public void setPeso(int peso) {
        this.peso = peso;
    }

    public NodeArbol<T> getRaiz() {
        return raiz;
    }

    public void setRaiz(NodeArbol<T> raiz) {
        this.raiz = raiz;
    }

    public boolean isEmpty(){
        return raiz == null;
    }

    public void recorrerInorden(){
        recorrerInorden(raiz);
    }

    private void recorrerInorden(NodeArbol<T> raiz){
        if(raiz == null) return;
        recorrerInorden(raiz.getIzquierdo());
        System.out.print(raiz.getElemento() + " ");
        recorrerInorden(raiz.getDerecho());
    }

    public void recorrerPreorden(){
        recorrerPreorden(raiz);
    }
    private void recorrerPreorden(NodeArbol<T> raiz){
        if(raiz == null) return;
        System.out.print(raiz.getElemento());
        recorrerPreorden(raiz.getIzquierdo());
        recorrerPreorden(raiz.getDerecho());
    }

    public void recorrerPostorden(){
        recorrerPostorden(raiz);
    }
    private void recorrerPostorden(NodeArbol<T> raiz){
        if(raiz == null) return;

        recorrerPostorden(raiz.getIzquierdo());
        recorrerPostorden(raiz.getDerecho());
        System.out.print(raiz.getElemento());
    }



    public int obtenerPeso() {
        return obtenerPeso(raiz);
    }
    private int obtenerPeso(NodeArbol<T> raiz) {
        //Caso base
        if (raiz == null) return 0;

        return 1 + obtenerPeso(raiz.getIzquierdo())+
        obtenerPeso(raiz.getDerecho());
    }

    public int ObtenerAltura(){
        return obtenerAltura(raiz);
    }
    private int obtenerAltura(NodeArbol<T> raiz) {
        if (raiz == null) {
            return 0;
        }

        int alturaIzquierda = obtenerAltura(raiz.getIzquierdo());
        int alturaDerecha = obtenerAltura(raiz.getDerecho());

        return 1 + (Math.max(alturaIzquierda, alturaDerecha));
    }


    public void recorridoAmplitud() {
        if (raiz == null) return;                      // árbol vacío: nada que recorrer

        Cola<NodeArbol<T>> cola = new Cola<>();        // la cola guarda NODOS, no elementos
        cola.encolar(raiz);

        while (!cola.isEmpty()) {
            NodeArbol<T> actual = cola.desencolar();
            System.out.print(actual.getElemento() + " ");

            if (actual.getIzquierdo() != null) {
                cola.encolar(actual.getIzquierdo());
            }
            if (actual.getDerecho() != null) {
                cola.encolar(actual.getDerecho());
            }
        }
    }


    public boolean existe(T elemento) {
        return existe(raiz, elemento);
    }

    private boolean existe(NodeArbol<T> raizAux, T elemento) {
        if (raizAux == null) {
            return false;
        }
        int comp = raizAux.getElemento().compareTo(elemento);

        if (comp == 0) return true;

        if (comp > 0) {
            return existe(raizAux.getIzquierdo(), elemento);
        } else {
            return existe(raizAux.getDerecho(), elemento);
        }
    }


    public void insertar(T elemento) {
        raiz= insertar(raiz, elemento,raiz);
    }

    private NodeArbol<T> insertar(NodeArbol<T> raizAux, T elemento, NodeArbol<T> padre) {
        if (raizAux == null) {
            peso++;
            NodeArbol<T> nuevoNodo = new NodeArbol<>(elemento);
            nuevoNodo.setPadre(padre);
            return nuevoNodo;
        } else {
            int comp = raizAux.getElemento().compareTo(elemento);

            if (comp == 0) throw new RuntimeException("Error: ya existe ese elemento");

            if (comp > 0) {
                raizAux.setIzquierdo(insertar(raizAux.getIzquierdo(), elemento, raizAux));
            } else {
                raizAux.setDerecho(insertar(raizAux.getDerecho(), elemento, raizAux));
            }
            return raizAux;
        }
    }

    //Tarea
    public void eliminar(T elemento) {
        raiz = eliminar(raiz, elemento);
        if (raiz != null) {
            raiz.setPadre(null);                      // la raíz nunca tiene padre
        }
    }

    private NodeArbol<T> eliminar(NodeArbol<T> nodo, T elemento) {
        if (nodo == null) {
            return null;                              // no se encontró: nada que eliminar
        }

        int cmp = elemento.compareTo(nodo.getElemento());

        if (cmp < 0) {
            NodeArbol<T> nuevoIzq = eliminar(nodo.getIzquierdo(), elemento);
            nodo.setIzquierdo(nuevoIzq);
            if (nuevoIzq != null) nuevoIzq.setPadre(nodo);

        } else if (cmp > 0) {
            NodeArbol<T> nuevoDer = eliminar(nodo.getDerecho(), elemento);
            nodo.setDerecho(nuevoDer);
            if (nuevoDer != null) nuevoDer.setPadre(nodo);

        } else {                                      // encontré el nodo a eliminar
            // Caso 1: hoja
            if (nodo.getIzquierdo() == null && nodo.getDerecho() == null) {
                peso--;
                return null;
            }
            // Caso 2: un solo hijo
            if (nodo.getIzquierdo() == null) {
                peso--;
                return nodo.getDerecho();
            }
            if (nodo.getDerecho() == null) {
                peso--;
                return nodo.getIzquierdo();
            }
            // Caso 3: dos hijos
            NodeArbol<T> sucesor = minimo(nodo.getDerecho());
            nodo.setElemento(sucesor.getElemento());              // copio el valor
            NodeArbol<T> nuevoDer = eliminar(nodo.getDerecho(), sucesor.getElemento());
            nodo.setDerecho(nuevoDer);                            // elimino el sucesor
            if (nuevoDer != null) nuevoDer.setPadre(nodo);
        }
        return nodo;
    }

    private NodeArbol<T> minimo(NodeArbol<T> nodo) {
        while (nodo.getIzquierdo() != null) {
            nodo = nodo.getIzquierdo();
        }
        return nodo;
    }


    // ---------- Consultas que devuelven una lista (para reportes) ----------

    /** Todos los elementos de menor a mayor (recorrido inorden). */
    public SimpleLinkedList<T> inorden() {
        SimpleLinkedList<T> lista = new SimpleLinkedList<>();
        inorden(raiz, lista);
        return lista;
    }

    private void inorden(NodeArbol<T> nodo, SimpleLinkedList<T> lista) {
        if (nodo == null) return;
        inorden(nodo.getIzquierdo(), lista);
        lista.addLast(nodo.getElemento());
        inorden(nodo.getDerecho(), lista);
    }

    /** Todos los elementos de mayor a menor (inorden invertido: derecho, raíz, izquierdo). */
    public SimpleLinkedList<T> inordenDescendente() {
        SimpleLinkedList<T> lista = new SimpleLinkedList<>();
        inordenDescendente(raiz, lista);
        return lista;
    }

    private void inordenDescendente(NodeArbol<T> nodo, SimpleLinkedList<T> lista) {
        if (nodo == null) return;
        inordenDescendente(nodo.getDerecho(), lista);
        lista.addLast(nodo.getElemento());
        inordenDescendente(nodo.getIzquierdo(), lista);
    }

    /**
     * Elementos entre inferior y superior (ambos incluidos), de menor a mayor.
     * Poda: solo baja por una rama si ahí puede haber elementos del rango, así que no recorre todo el árbol.
     */
    public SimpleLinkedList<T> enRango(T inferior, T superior) {
        SimpleLinkedList<T> lista = new SimpleLinkedList<>();
        if (inferior.compareTo(superior) <= 0) {
            enRango(raiz, inferior, superior, lista);
        }
        return lista;
    }

    private void enRango(NodeArbol<T> nodo, T inferior, T superior, SimpleLinkedList<T> lista) {
        if (nodo == null) return;
        int conInferior = nodo.getElemento().compareTo(inferior);
        int conSuperior = nodo.getElemento().compareTo(superior);
        if (conInferior > 0) {
            enRango(nodo.getIzquierdo(), inferior, superior, lista);   // a la izquierda aún puede haber valores en el rango
        }
        if (conInferior >= 0 && conSuperior <= 0) {
            lista.addLast(nodo.getElemento());
        }
        if (conSuperior < 0) {
            enRango(nodo.getDerecho(), inferior, superior, lista);     // a la derecha aún puede haber valores en el rango
        }
    }

    public NodeArbol<T> obtenerMenor(){
        return obtenerMenor(raiz);
    }

    private NodeArbol<T> obtenerMenor(NodeArbol<T> actual){
        if (actual == null) {
            return null;
        }

        while(actual.getIzquierdo()!=null){
            actual = actual.getIzquierdo();
        }
        return actual;
    }
}