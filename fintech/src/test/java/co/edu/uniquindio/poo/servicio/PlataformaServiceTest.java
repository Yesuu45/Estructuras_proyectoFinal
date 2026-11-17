package co.edu.uniquindio.poo.servicio;

import co.edu.uniquindio.poo.Estructuras.Cola;
import co.edu.uniquindio.poo.modelo.Nivel;
import co.edu.uniquindio.poo.modelo.TipoBilletera;
import co.edu.uniquindio.poo.modelo.TipoTransaccion;
import co.edu.uniquindio.poo.modelo.Transaccion;
import co.edu.uniquindio.poo.persistencia.RepositorioPlataforma;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de reglas de negocio. El repositorio es un doble: no hace falta PostgreSQL. */
class PlataformaServiceTest {

    private PlataformaService servicio;

    private static BigDecimal v(String valor) {
        return new BigDecimal(valor);
    }

    @BeforeEach
    void preparar() {
        RepositorioPlataforma repo = Mockito.mock(RepositorioPlataforma.class);
        Mockito.when(repo.cargarUsuarios()).thenReturn(new Cola<>());
        Mockito.when(repo.cargarBilleteras()).thenReturn(new Cola<>());
        Mockito.when(repo.cargarTransacciones()).thenReturn(new Cola<>());
        Mockito.when(repo.cargarProgramadas(Mockito.any())).thenReturn(new Cola<>());
        Mockito.when(repo.cargarBeneficios()).thenReturn(new Cola<>());
        Mockito.when(repo.cargarEventos()).thenReturn(new Cola<>());
        servicio = new PlataformaService(repo);

        servicio.sembrarAdministrador("admin", "Administrador", "admin123");
        servicio.registrarUsuario("u1", "Ana", "ana@correo.com", "clave123");
        servicio.registrarUsuario("u2", "Beto", "beto@correo.com", "clave123");
        servicio.crearBilletera("A1", "Ahorros Ana", TipoBilletera.AHORROS, "u1", v("100000"), v("1000"));
        servicio.crearBilletera("B1", "Digital Beto", TipoBilletera.DIGITAL, "u2", v("0"), v("0"));
    }

    private BigDecimal saldo(String codigo) {
        return servicio.buscarBilletera(codigo).getSaldo();
    }

    @Test
    void noPermiteUsuariosDuplicados() {
        assertThrows(IllegalStateException.class,
                () -> servicio.registrarUsuario("u1", "Otra", "otra@correo.com", "x"));
    }

    @Test
    void autenticaConLaClaveCorrecta() {
        assertEquals("u1", servicio.autenticar("u1", "clave123").getIdentificacion());
        assertThrows(RuntimeException.class, () -> servicio.autenticar("u1", "incorrecta"));
    }

    @Test
    void recargarYRetirarActualizanElSaldo() {
        servicio.recargar("A1", v("5000"));
        assertEquals(0, v("105000").compareTo(saldo("A1")));
        servicio.retirar("A1", v("2000"));
        assertEquals(0, v("103000").compareTo(saldo("A1")));
    }

    @Test
    void transferirMueveElDineroEntreBilleteras() {
        servicio.transferir("A1", "B1", v("30000"));
        assertEquals(0, v("70000").compareTo(saldo("A1")));
        assertEquals(0, v("30000").compareTo(saldo("B1")));
    }

    @Test
    void rechazaTransferirMasQueElSaldo() {
        assertThrows(IllegalStateException.class, () -> servicio.transferir("A1", "B1", v("999999")));
        assertEquals(0, v("100000").compareTo(saldo("A1")));
    }

    @Test
    void rechazaValoresInvalidosYBilleterasInexistentes() {
        assertThrows(RuntimeException.class, () -> servicio.recargar("A1", v("0")));
        assertThrows(RuntimeException.class, () -> servicio.recargar("A1", v("-5")));
        assertThrows(RuntimeException.class, () -> servicio.transferir("A1", "NOEXISTE", v("10")));
    }

    @Test
    void elNivelBronceLimitaElValorPorOperacion() {
        servicio.registrarUsuario("u3", "Caro", "caro@correo.com", "clave123");
        servicio.crearBilletera("C1", "Corriente Caro", TipoBilletera.CORRIENTE, "u3", v("1000000"), v("0"));
        assertEquals(Nivel.BRONCE, servicio.buscarUsuario("u3").getNivel());
        assertThrows(IllegalStateException.class, () -> servicio.transferir("C1", "B1", v("250000")));
        assertEquals(0, v("1000000").compareTo(saldo("C1")));
    }

    @Test
    void revertirDevuelveElSaldoYMarcaLaTransaccion() {
        Transaccion t = servicio.transferir("A1", "B1", v("30000"));
        servicio.revertir("u1", t.getId());
        assertEquals(0, v("100000").compareTo(saldo("A1")));
        assertEquals(0, v("0").compareTo(saldo("B1")));
        assertThrows(IllegalStateException.class, () -> servicio.revertir("u1", t.getId()));
    }

    @Test
    void deshacerUltimaRevierteLaUltimaOperacion() {
        servicio.recargar("A1", v("1000"));
        Transaccion ultima = servicio.transferir("A1", "B1", v("10000"));
        Transaccion deshecha = servicio.deshacerUltima("u1");
        assertEquals(ultima.getId(), deshecha.getId());
        assertEquals(0, v("101000").compareTo(saldo("A1")));
        assertEquals(1, servicio.listarDeshechas("u1").length);
    }

    @Test
    void unUsuarioNoVeLasTransaccionesDeOtro() {
        servicio.recargar("B1", v("1000"));
        assertEquals(0, servicio.listarTransacciones("u1").length);
        assertEquals(1, servicio.listarTransacciones("u2").length);
    }

    @Test
    void elHistorialListaLasOperacionesDeLaBilletera() {
        servicio.recargar("A1", v("1000"));
        servicio.retirar("A1", v("500"));
        assertEquals(2, servicio.historialBilletera("u1", "A1").length);
    }

    @Test
    void programadaSoloSeEjecutaCuandoLlegaSuFecha() {
        LocalDateTime futuro = servicio.ahora().plusDays(2);
        servicio.programar("u1", futuro, TipoTransaccion.TRANSFERENCIA, "A1", "B1", v("5000"));
        assertEquals(0, servicio.ejecutarPendientes());
        assertEquals(0, v("100000").compareTo(saldo("A1")));

        servicio.avanzarReloj("admin", 3 * 24 * 60, true);
        assertEquals(0, v("95000").compareTo(saldo("A1")));
        assertEquals(0, v("5000").compareTo(saldo("B1")));
    }

    @Test
    void redDeUsuariosRegistraLasTransferencias() {
        servicio.transferir("A1", "B1", v("1000"));
        String[] ruta = servicio.rutaDeDinero("admin", "u1", "u2");
        assertEquals(2, ruta.length);
    }

    @Test
    void rankingOrdenaPorPuntosDeMayorAMenor() {
        servicio.recargar("A1", v("150000"));
        var ranking = servicio.rankingDePuntos("admin");
        assertTrue(ranking.length >= 1);
        for (int i = 1; i < ranking.length; i++) {
            assertTrue(ranking[i - 1].getPuntos() >= ranking[i].getPuntos());
        }
    }
}
