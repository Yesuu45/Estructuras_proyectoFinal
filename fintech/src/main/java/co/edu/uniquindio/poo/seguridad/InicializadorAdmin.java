package co.edu.uniquindio.poo.seguridad;

import co.edu.uniquindio.poo.servicio.PlataformaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Al arrancar, crea la cuenta de administrador si todavía no existe. Los datos salen de application.properties. */
@Component
public class InicializadorAdmin implements CommandLineRunner {

    private final PlataformaService servicio;
    private final String identificacion;
    private final String nombre;
    private final String clave;

    public InicializadorAdmin(PlataformaService servicio,
                              @Value("${app.admin.identificacion:admin}") String identificacion,
                              @Value("${app.admin.nombre:Administrador}") String nombre,
                              @Value("${app.admin.clave:admin123}") String clave) {
        this.servicio = servicio;
        this.identificacion = identificacion;
        this.nombre = nombre;
        this.clave = clave;
    }

    @Override
    public void run(String... argumentos) {
        servicio.sembrarAdministrador(identificacion, nombre, clave);
    }
}
