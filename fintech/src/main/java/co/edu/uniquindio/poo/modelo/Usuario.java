package co.edu.uniquindio.poo.modelo;

public class Usuario {
    private final String identificacion;
    private String nombre;
    private String correo;
    private int puntos;

    public Usuario(String identificacion, String nombre, String correo) {
        this.identificacion = validarTexto(identificacion, "La identificación");
        this.nombre = validarTexto(nombre, "El nombre");
        this.correo = correo == null ? "" : correo.trim();
        this.puntos = 0;
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
