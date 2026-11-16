package co.edu.uniquindio.poo.servicio;

import co.edu.uniquindio.poo.Estructuras.Cola;
import co.edu.uniquindio.poo.Estructuras.TablaHash;
import co.edu.uniquindio.poo.modelo.Billetera;
import co.edu.uniquindio.poo.modelo.TipoBilletera;
import co.edu.uniquindio.poo.modelo.Usuario;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * Fachada de la plataforma. La interfaz (controladores) solo habla con esta clase.
 * Por ahora los datos viven en memoria; la persistencia en PostgreSQL llega después.
 */

@Service
public class PlataformaService {

    private final TablaHash<String, Usuario> usuarios = new TablaHash<>();
    private final TablaHash<String, Billetera> billeteras = new TablaHash<>();

    public Usuario registrarUsuario(String identificacion, String nombre, String correo) {
        if (usuarios.containsKey(identificacion)) {
            throw new IllegalStateException("Ya existe un usuario con identificación " + identificacion);
        }
        Usuario usuario = new Usuario(identificacion, nombre, correo);
        usuarios.put(identificacion, usuario);
        return usuario;
    }

    public Usuario buscarUsuario(String identificacion) {
        Usuario usuario = usuarios.get(identificacion);
        if (usuario == null) {
            throw new IllegalArgumentException("No existe el usuario " + identificacion);
        }
        return usuario;
    }

    public Usuario[] listarUsuarios() {
        Cola<String> claves = usuarios.claves();
        Usuario[] resultado = new Usuario[claves.getSize()];
        for (int i = 0; i < resultado.length; i++) {
            resultado[i] = usuarios.get(claves.desencolar());
        }
        return resultado;
    }

    public Billetera crearBilletera(String codigo, String nombre, TipoBilletera tipo,
                                    String identificacionTitular, BigDecimal saldoInicial,
                                    BigDecimal umbralSaldoBajo) {
        Usuario titular = buscarUsuario(identificacionTitular);
        if (billeteras.containsKey(codigo)) {
            throw new IllegalStateException("Ya existe la billetera " + codigo);
        }
        Billetera billetera = new Billetera(codigo, nombre, tipo, identificacionTitular,
                saldoInicial, umbralSaldoBajo);
        billeteras.put(codigo, billetera);
        titular.agregarBilletera(billetera);
        return billetera;
    }

    public Billetera buscarBilletera(String codigo) {
        Billetera billetera = billeteras.get(codigo);
        if (billetera == null) {
            throw new IllegalArgumentException("No existe la billetera " + codigo);
        }
        return billetera;
    }

    public Billetera[] listarBilleteras() {
        Cola<String> claves = billeteras.claves();
        Billetera[] resultado = new Billetera[claves.getSize()];
        for (int i = 0; i < resultado.length; i++) {
            resultado[i] = billeteras.get(claves.desencolar());
        }
        return resultado;
    }
}
