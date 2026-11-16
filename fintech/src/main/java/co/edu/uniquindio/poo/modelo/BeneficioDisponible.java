package co.edu.uniquindio.poo.modelo;

/**
 * Oferta del catálogo de beneficios que un usuario puede canjear con sus puntos.
 */
public class BeneficioDisponible {

    private final String id;
    private final String nombre;
    private final int puntosCosto;

    public BeneficioDisponible(String id, String nombre, int puntosCosto) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El identificador del beneficio es obligatorio");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del beneficio es obligatorio");
        }
        if (puntosCosto <= 0) {
            throw new IllegalArgumentException("El costo en puntos debe ser mayor que cero");
        }
        this.id = id;
        this.nombre = nombre;
        this.puntosCosto = puntosCosto;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getPuntosCosto() {
        return puntosCosto;
    }
}