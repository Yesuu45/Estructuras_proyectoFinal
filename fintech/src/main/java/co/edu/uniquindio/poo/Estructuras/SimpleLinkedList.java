package co.edu.uniquindio.poo.Estructuras;

import java.lang.reflect.Array;
import java.util.Objects;

/**
 * Lista enlazada simple genérica. Es la SimpleLinkedList del taller de clase, pero guardando cualquier
 * tipo y con un puntero al último nodo: así addLast es O(1) en vez de recorrer toda la lista.
 * Se usa para los historiales, que solo crecen por el final y se leen en orden de llegada.
 */
public class SimpleLinkedList<T> {

    private int size;
    private NodeSimple<T> firstNode;
    private NodeSimple<T> lastNode;

    public SimpleLinkedList() {
        this.size = 0;
        this.firstNode = null;
        this.lastNode = null;
    }

    public int getSize() {
        return size;
    }

    public boolean isEmpty() {
        return firstNode == null;
    }

    /** Para recorrer la lista: for (NodeSimple<T> n = lista.getFirstNode(); n != null; n = n.getNext()). */
    public NodeSimple<T> getFirstNode() {
        return firstNode;
    }

    public void addFirst(T data) {
        NodeSimple<T> newNode = new NodeSimple<>(data);
        if (isEmpty()) {
            firstNode = newNode;
            lastNode = newNode;
        } else {
            newNode.setNext(firstNode);
            firstNode = newNode;
        }
        size++;
    }

    public void addLast(T data) {
        NodeSimple<T> newNode = new NodeSimple<>(data);
        if (isEmpty()) {
            firstNode = newNode;
        } else {
            lastNode.setNext(newNode);
        }
        lastNode = newNode;
        size++;
    }

    public boolean addInPosition(int index, T data) {
        if (index < 0 || index > size) {
            return false;
        }
        if (index == 0) {
            addFirst(data);
            return true;
        }
        if (index == size) {
            addLast(data);
            return true;
        }
        NodeSimple<T> aux = firstNode;
        for (int i = 0; i < index - 1; i++) {
            aux = aux.getNext();
        }
        NodeSimple<T> newNode = new NodeSimple<>(data);
        newNode.setNext(aux.getNext());
        aux.setNext(newNode);
        size++;
        return true;
    }

    public void deleteFirst() {
        if (isEmpty()) {
            return;
        }
        firstNode = firstNode.getNext();
        if (firstNode == null) {
            lastNode = null;
        }
        size--;
    }

    public void deleteLast() {
        if (isEmpty()) {
            return;
        }
        if (firstNode == lastNode) {
            firstNode = null;
            lastNode = null;
        } else {
            NodeSimple<T> aux = firstNode;
            while (aux.getNext() != lastNode) {
                aux = aux.getNext();
            }
            aux.setNext(null);
            lastNode = aux;
        }
        size--;
    }

    public boolean remove(T data) {
        if (isEmpty()) {
            return false;
        }
        if (Objects.equals(firstNode.getData(), data)) {
            deleteFirst();
            return true;
        }
        NodeSimple<T> aux = firstNode;
        while (aux.getNext() != null && !Objects.equals(aux.getNext().getData(), data)) {
            aux = aux.getNext();
        }
        if (aux.getNext() == null) {
            return false;
        }
        if (aux.getNext() == lastNode) {
            lastNode = aux;
        }
        aux.setNext(aux.getNext().getNext());
        size--;
        return true;
    }

    public T getNodeValue(int position) {
        if (!indexValid(position)) {
            throw new IllegalArgumentException("Posición inválida: " + position);
        }
        NodeSimple<T> aux = firstNode;
        for (int i = 0; i < position; i++) {
            aux = aux.getNext();
        }
        return aux.getData();
    }

    /** Posición del primer nodo que contiene el dato, o -1 si no está. */
    public int getNodePosition(T data) {
        int position = 0;
        for (NodeSimple<T> aux = firstNode; aux != null; aux = aux.getNext()) {
            if (Objects.equals(aux.getData(), data)) {
                return position;
            }
            position++;
        }
        return -1;
    }

    public boolean contains(T data) {
        return getNodePosition(data) != -1;
    }

    public boolean indexValid(int index) {
        return index >= 0 && index < size;
    }

    public boolean modifyNode(int index, T newValue) {
        if (!indexValid(index)) {
            return false;
        }
        NodeSimple<T> aux = firstNode;
        for (int i = 0; i < index; i++) {
            aux = aux.getNext();
        }
        aux.setData(newValue);
        return true;
    }

    public void clear() {
        size = 0;
        firstNode = null;
        lastNode = null;
    }

    /** Copia la lista, en orden, a un arreglo del mismo tipo que el recibido (la interfaz trabaja con arreglos). */
    @SuppressWarnings("unchecked")
    public T[] toArray(T[] modelo) {
        T[] resultado = (T[]) Array.newInstance(modelo.getClass().getComponentType(), size);
        int i = 0;
        for (NodeSimple<T> aux = firstNode; aux != null; aux = aux.getNext()) {
            resultado[i++] = aux.getData();
        }
        return resultado;
    }

    public void print() {
        for (NodeSimple<T> aux = firstNode; aux != null; aux = aux.getNext()) {
            System.out.print(aux.getData() + "->");
        }
        System.out.println();
    }
}
