package co.edu.uniquindio.poo.seguridad;

/** El usuario está identificado, pero no tiene permiso para esa operación o ese dato (HTTP 403). */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
