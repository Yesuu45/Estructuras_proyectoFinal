package co.edu.uniquindio.poo.modelo;

import co.edu.uniquindio.poo.Estructuras.Cola;
import co.edu.uniquindio.poo.Estructuras.TablaHash;
import co.edu.uniquindio.poo.seguridad.ClaveSegura;

public class Usuario {

    private final String identificacion;
    private String nombre;
    private String correo;
    private final String claveHash;
    private int puntos;
    private final TablaHash<String, Billetera> billeteras;

    /** claveHash puede ser null solo para usuarios antiguos sin contraseña; esos no pueden iniciar sesión. */
    public Usuario(String identificacion, String nombre, String correo, String claveHash) {
        this.identificacion = validarTexto(identificacion, "La identificación");
        this.nombre = validarTexto(nombre, "El nombre");
        this.correo = correo == null ? "" : correo.trim();
        this.claveHash = claveHash;
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