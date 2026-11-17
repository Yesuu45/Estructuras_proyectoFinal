package co.edu.uniquindio.poo.modelo;

/**
 * Clave con la que cada usuario se guarda en el árbol de puntos: primero por puntos y, si hay empate,
 * por identificación. Dos usuarios con los mismos puntos tienen claves distintas (el árbol no admite repetidos).
 * Los límites inferior/superior sirven para buscar por rango de puntos sin importar la identificación.
 */
public class ClavePuntos implements Comparable<ClavePuntos> {

    private final int puntos;
    private final String identificacion;
    private final int limite;      // -1: menor que cualquier usuario con esos puntos; 0: usuario real; 1: mayor que todos

    public ClavePuntos(int puntos, String identificacion) {
        this(puntos, identificacion, 0);
    }

    private ClavePuntos(int puntos, String identificacion, int limite) {
        this.puntos = puntos;
        this.identificacion = identificacion;
        this.limite = limite;
    }

    public static ClavePuntos limiteInferior(int puntos) {
        return new ClavePuntos(puntos, null, -1);
    }

    public static ClavePuntos limiteSuperior(int puntos) {
        return new ClavePuntos(puntos, null, 1);
    }

    public int getPuntos() {
        return puntos;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    @Override
    public int compareTo(ClavePuntos otra) {
        int porPuntos = Integer.compare(puntos, otra.puntos);
        if (porPuntos != 0) {
            return porPuntos;
        }
        int porLimite = Integer.compare(limite, otra.limite);
        if (porLimite != 0 || limite != 0) {
            return porLimite;
        }
        return identificacion.compareTo(otra.identificacion);
    }

    @Override
    public String toString() {
        return identificacion + " (" + puntos + " pts)";
    }
}
