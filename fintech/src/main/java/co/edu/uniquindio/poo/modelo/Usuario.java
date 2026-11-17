package co.edu.uniquindio.poo.modelo;

import co.edu.uniquindio.poo.Estructuras.Cola;
import co.edu.uniquindio.poo.Estructuras.SimpleLinkedList;
import co.edu.uniquindio.poo.Estructuras.Stack;
import co.edu.uniquindio.poo.Estructuras.TablaHash;
import co.edu.uniquindio.poo.seguridad.ClaveSegura;

public class Usuario {

    private final String identificacion;
    private String nombre;
    private String correo;
    private final String claveHash;
    private final Rol rol;
    private int puntos;
    private final TablaHash<String, Billetera> billeteras;
    // Listas enlazadas: movimientos en los que participa el usuario y beneficios que ha canjeado, en orden de llegada
    private final SimpleLinkedList<Transaccion> historial = new SimpleLinkedList<>();
    private final SimpleLinkedList<Beneficio> canjes = new SimpleLinkedList<>();
    // Pilas de deshacer: la más reciente queda arriba. Reversibles = aún se pueden revertir; deshechas = ya revertidas
    private final Stack<Transaccion> reversibles = new Stack<>();
    private final Stack<Transaccion> deshechas = new Stack<>();

    /** claveHash puede ser null solo para usuarios antiguos sin contraseña; esos no pueden iniciar sesión. */
    public Usuario(String identificacion, String nombre, String correo, String claveHash) {
        this(identificacion, nombre, correo, claveHash, Rol.USUARIO);
    }

    public Usuario(String identificacion, String nombre, String correo, String claveHash, Rol rol) {
        this.identificacion = validarTexto(identificacion, "La identificación");
        this.nombre = validarTexto(nombre, "El nombre");
        this.correo = correo == null ? "" : correo.trim();
        this.claveHash = claveHash;
        this.rol = rol == null ? Rol.USUARIO : rol;
        this.puntos = 0;
        this.billeteras = new TablaHash<>();
    }

    public void agregarBilletera(Billetera billetera) {
        if (billetera == null) {
            throw new IllegalArgumentException("La billetera no puede ser nula");
        }
        if (billeteras.containsKey(billetera.getCodigo())) {
            throw new IllegalStateException("Ya existe la billetera " + billetera.getCodigo());
        }
        billeteras.put(billetera.getCodigo(), billetera);
    }

    public void registrarMovimiento(Transaccion transaccion) {
        historial.addLast(transaccion);
    }

    // Sin prefijo get: así Jackson no incluye las listas en el JSON del usuario
    public SimpleLinkedList<Transaccion> historialMovimientos() {
        return historial;
    }

    public Stack<Transaccion> pilaReversibles() {
        return reversibles;
    }

    public Stack<Transaccion> pilaDeshechas() {
        return deshechas;
    }

    public void registrarCanje(Beneficio beneficio) {
        canjes.addLast(beneficio);
    }

    public SimpleLinkedList<Beneficio> canjesRealizados() {
        return canjes;
    }

    public Billetera buscarBilletera(String codigo) {
        return billeteras.get(codigo);
    }

    public boolean coincideClave(String clave) {
        return ClaveSegura.coincide(clave, claveHash);
    }

    // Sin prefijo get: así Jackson no lo incluye en el JSON que llega al navegador
    public String hashClave() {
        return claveHash;
    }

    public Rol getRol() {
        return rol;
    }

    public boolean esAdmin() {
        return rol == Rol.ADMIN;
    }

    public int cantidadBilleteras() {
        return billeteras.size();
    }

    public Cola<String> codigosBilleteras() {
        return billeteras.claves();
    }

    public void sumarPuntos(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("Los puntos a sumar no pueden ser negativos");
        }
        puntos += cantidad;
    }

    public void restarPuntos(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("Los puntos a restar no pueden ser negativos");
        }
        if (cantidad > puntos) {
            throw new IllegalStateException("El usuario no tiene puntos suficientes");
        }
        puntos -= cantidad;
    }

    public Nivel getNivel() {
        return Nivel.desdePuntos(puntos);
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public int getPuntos() {
        return puntos;
    }

    private static String validarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " es obligatorio");
        }
        return valor.trim();
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) return true;
        if (!(otro instanceof Usuario)) return false;
        Usuario usuario = (Usuario) otro;
        return identificacion.equals(usuario.identificacion);
    }

    @Override
    public int hashCode() {
        return identificacion.hashCode();
    }

    @Override
    public String toString() {
        return nombre + " (" + identificacion + ") - " + puntos + " pts - " + getNivel();
    }
}