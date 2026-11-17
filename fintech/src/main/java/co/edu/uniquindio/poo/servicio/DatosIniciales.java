package co.edu.uniquindio.poo.servicio;

import co.edu.uniquindio.poo.modelo.TipoBilletera;
import co.edu.uniquindio.poo.modelo.TipoTransaccion;
import co.edu.uniquindio.poo.modelo.Transaccion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Datos de demostración que se cargan solos al arrancar: 5 usuarios con billeteras, movimientos,
 * una reversión, canjes y operaciones programadas. Todo entra por PlataformaService, así que los
 * puntos, niveles y notificaciones salen de las mismas reglas que usa la aplicación.
 *
 * Todos los usuarios tienen la clave "clave123". Se puede apagar con app.datos-demo=false.
 * Si el usuario 1001 ya existe (por ejemplo, cuando haya base de datos), no se vuelve a cargar.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    static final String CLAVE = "clave123";

    private final PlataformaService servicio;
    private final boolean activo;

    public DatosIniciales(PlataformaService servicio, @Value("${app.datos-demo:true}") boolean activo) {
        this.servicio = servicio;
        this.activo = activo;
    }

    @Override
    public void run(String... argumentos) {
        if (activo) {
            cargar();
        }
    }

    void cargar() {
        if (servicio.existeUsuario("1001")) {
            return;
        }

        // ---- Usuarios ----
        servicio.registrarUsuario("1001", "Laura Gómez", "laura@correo.com", CLAVE);
        servicio.registrarUsuario("1002", "Carlos Ruiz", "carlos@correo.com", CLAVE);
        servicio.registrarUsuario("1003", "Daniela Mora", "daniela@correo.com", CLAVE);
        servicio.registrarUsuario("1004", "Esteban Rojas", "esteban@correo.com", CLAVE);
        servicio.registrarUsuario("1005", "Valentina Ortiz", "valentina@correo.com", CLAVE);

        // ---- Billeteras (código, nombre, tipo, titular, saldo inicial, umbral de saldo bajo) ----
        billetera("LAU-AH", "Ahorros Laura", TipoBilletera.AHORROS, "1001", 0, 100_000);
        billetera("LAU-DG", "Gastos diarios", TipoBilletera.DIGITAL, "1001", 250_000, 50_000);
        billetera("CAR-CO", "Cuenta Carlos", TipoBilletera.CORRIENTE, "1002", 400_000, 50_000);
        billetera("CAR-AH", "Ahorro Carlos", TipoBilletera.AHORROS, "1002", 100_000, 20_000);
        billetera("DAN-DG", "Daniela diaria", TipoBilletera.DIGITAL, "1003", 80_000, 20_000);
        billetera("EST-DG", "Esteban digital", TipoBilletera.DIGITAL, "1004", 12_000, 5_000);
        billetera("VAL-AH", "Ahorros Valentina", TipoBilletera.AHORROS, "1005", 30_000, 10_000);

        // ---- Movimientos (1 pt por 100 en recargas, 2 en retiros, 3 en transferencias) ----
        // Laura: recarga fuerte y una transferencia -> 7.500 pts, nivel PLATINO
        servicio.recargar("LAU-AH", valor(600_000));
        servicio.transferir("LAU-AH", "DAN-DG", valor(50_000));

        // Carlos: recarga y retiro -> 2.600 pts, nivel ORO
        servicio.recargar("CAR-CO", valor(200_000));
        servicio.retirar("CAR-CO", valor(30_000));

        // Carlos se equivoca de destinatario y revierte: los puntos y saldos vuelven atrás
        Transaccion error = servicio.transferir("CAR-CO", "EST-DG", valor(15_000));
        servicio.revertir("1002", error.getId());

        // Daniela: recarga y retiro -> 700 pts, nivel PLATA
        servicio.recargar("DAN-DG", valor(50_000));
        servicio.retirar("DAN-DG", valor(10_000));

        // Esteban: deja su billetera casi vacía -> alerta de saldo bajo, nivel BRONCE
        servicio.retirar("EST-DG", valor(9_000));
        servicio.transferir("EST-DG", "VAL-AH", valor(1_000));

        // Valentina: usuaria nueva, sin movimientos propios (solo recibió una transferencia)

        // Más transferencias entre usuarios: forman el ciclo Laura -> Daniela -> Carlos -> Laura en el grafo
        servicio.transferir("DAN-DG", "CAR-CO", valor(5_000));   // Daniela sube a 850 pts (sigue en PLATA)
        servicio.transferir("CAR-CO", "LAU-DG", valor(2_000));   // Carlos sube a 2.160 pts (sigue en ORO)

        // ---- Canjes ----
        servicio.canjear("1001", "C1");   // Laura: cupón de recargas (1.500 pts)
        servicio.canjear("1002", "D5");   // Carlos: descuento del 5% (500 pts)

        // ---- Operaciones programadas (quedan pendientes) ----
        LocalDateTime ahora = LocalDateTime.now();
        servicio.programar("1001", ahora.plusDays(2), TipoTransaccion.TRANSFERENCIA,
                "LAU-DG", "LAU-AH", valor(30_000));        // ahorro automático entre sus billeteras
        servicio.programar("1001", ahora.plusDays(7), TipoTransaccion.PAGO_PROGRAMADO,
                "LAU-DG", "DAN-DG", valor(20_000));
        servicio.programar("1002", ahora.plusDays(3), TipoTransaccion.TRANSFERENCIA,
                "CAR-CO", "VAL-AH", valor(25_000));
    }

    private void billetera(String codigo, String nombre, TipoBilletera tipo, String titular,
                           long saldoInicial, long umbral) {
        servicio.crearBilletera(codigo, nombre, tipo, titular, valor(saldoInicial), valor(umbral));
    }

    private static BigDecimal valor(long cantidad) {
        return BigDecimal.valueOf(cantidad);
    }
}
