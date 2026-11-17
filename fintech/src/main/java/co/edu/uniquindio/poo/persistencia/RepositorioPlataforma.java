package co.edu.uniquindio.poo.persistencia;

import co.edu.uniquindio.poo.Estructuras.Cola;
import co.edu.uniquindio.poo.Estructuras.MonticuloBinario;
import co.edu.uniquindio.poo.modelo.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Acceso a PostgreSQL. Solo guarda y lee filas; las reglas de negocio siguen en PlataformaService.
 * Las cargas llenan nuestras propias colas (Cola, MonticuloBinario), no colecciones de java.util.
 */
@Repository
public class RepositorioPlataforma {

    private final JdbcTemplate jdbc;

    public RepositorioPlataforma(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------- Escrituras ----------

    public void insertarUsuario(String identificacion, String nombre, String correo, String claveHash,
                                Rol rol, Nivel nivel) {
        jdbc.update("INSERT INTO usuarios (identificacion, nombre, correo, clave_hash, rol, puntos, nivel, "
                + "fecha_registro) VALUES (?, ?, ?, ?, ?, 0, ?, ?)",
                identificacion, nombre, correo, claveHash, rol.name(), nivel.name(), ahora());
    }

    public void actualizarUsuario(String identificacion, String nombre, String correo, String claveHash,
                                  int puntos, Nivel nivel) {
        jdbc.update("UPDATE usuarios SET nombre = ?, correo = ?, clave_hash = ?, puntos = ?, nivel = ? "
                + "WHERE identificacion = ?",
                nombre, correo, claveHash, puntos, nivel.name(), identificacion);
    }

    public void eliminarUsuario(String identificacion) {
        jdbc.update("DELETE FROM usuarios WHERE identificacion = ?", identificacion);
    }

    public void insertarBilletera(String codigo, String identificacionTitular, String nombre,
                                  TipoBilletera tipo, BigDecimal saldo, BigDecimal umbralSaldoBajo) {
        jdbc.update("INSERT INTO billeteras (codigo, identificacion_usuario, nombre, tipo, saldo, estado, "
                + "umbral_saldo_bajo) VALUES (?, ?, ?, ?, ?, 'ACTIVA', ?)",
                codigo, identificacionTitular, nombre, tipo.name(), saldo, umbralSaldoBajo);
    }

    public void actualizarBilletera(String codigo, BigDecimal saldo, boolean activa, BigDecimal umbralSaldoBajo) {
        jdbc.update("UPDATE billeteras SET saldo = ?, estado = ?, umbral_saldo_bajo = ? WHERE codigo = ?",
                saldo, activa ? "ACTIVA" : "BLOQUEADA", umbralSaldoBajo, codigo);
    }

    public void insertarTransaccion(String id, LocalDateTime fecha, TipoTransaccion tipo, BigDecimal valor,
                                    String codigoOrigen, String codigoDestino) {
        jdbc.update("INSERT INTO transacciones (id, fecha, tipo, valor, codigo_origen, codigo_destino, "
                + "estado, puntos_generados, nivel_riesgo) VALUES (?, ?, ?, ?, ?, ?, 'COMPLETADA', 0, 'BAJO')",
                id, Timestamp.valueOf(fecha), tipo.name(), valor, codigoOrigen, codigoDestino);
    }

    public void actualizarTransaccion(String id, EstadoTransaccion estado, int puntosGenerados,
                                      NivelRiesgo riesgo) {
        jdbc.update("UPDATE transacciones SET estado = ?, puntos_generados = ?, nivel_riesgo = ? WHERE id = ?",
                estado.name(), puntosGenerados, riesgo.name(), id);
    }

    public void insertarProgramada(String id, LocalDateTime fechaEjecucion, TipoTransaccion tipo,
                                   BigDecimal valor, String codigoOrigen, String codigoDestino,
                                   Periodicidad periodicidad, int repeticionesRestantes) {
        jdbc.update("INSERT INTO operaciones_programadas (id, fecha_ejecucion, tipo, valor, codigo_origen, "
                + "codigo_destino, estado, periodicidad, repeticiones_restantes, avisada) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'PENDIENTE', ?, ?, false)",
                id, Timestamp.valueOf(fechaEjecucion), tipo.name(), valor, codigoOrigen, codigoDestino,
                periodicidad.name(), repeticionesRestantes);
    }

    public void actualizarProgramada(String id, EstadoOperacionProgramada estado, boolean avisada) {
        jdbc.update("UPDATE operaciones_programadas SET estado = ?, avisada = ? WHERE id = ?",
                estado.name(), avisada, id);
    }

    public void insertarBeneficio(String identificacion, String descripcion, int puntosCosto, LocalDateTime fecha) {
        jdbc.update("INSERT INTO beneficios (identificacion_usuario, descripcion, puntos_costo, fecha_canje) "
                + "VALUES (?, ?, ?, ?)", identificacion, descripcion, puntosCosto, Timestamp.valueOf(fecha));
    }

    public void insertarEvento(LocalDateTime fecha, String identificacion, String evento,
                               NivelRiesgo riesgo, String detalle) {
        jdbc.update("INSERT INTO auditoria (fecha, identificacion_usuario, evento, nivel_riesgo, detalle) "
                + "VALUES (?, ?, ?, ?, ?)", Timestamp.valueOf(fecha), identificacion, evento, riesgo.name(), detalle);
    }

    // ---------- Lecturas ----------

    public int ultimaSecuencia() {
        Integer maximo = jdbc.queryForObject(
                "SELECT COALESCE(MAX(n), 0) FROM (SELECT SUBSTRING(id FROM 2)::int AS n FROM transacciones "
                        + "UNION ALL SELECT SUBSTRING(id FROM 2)::int FROM operaciones_programadas) AS ids",
                Integer.class);
        return maximo == null ? 0 : maximo;
    }

    public Cola<Usuario> cargarUsuarios() {
        Cola<Usuario> cola = new Cola<>();
        jdbc.query("SELECT identificacion, nombre, correo, clave_hash, rol, puntos FROM usuarios",
                (RowCallbackHandler) rs -> {
                    Usuario usuario = new Usuario(rs.getString("identificacion"),
                            rs.getString("nombre"), rs.getString("correo"), rs.getString("clave_hash"),
                            Rol.valueOf(rs.getString("rol")));
                    int puntos = rs.getInt("puntos");
                    if (puntos > 0) {
                        usuario.sumarPuntos(puntos);
                    }
                    cola.encolar(usuario);
                });
        return cola;
    }

    public Cola<Billetera> cargarBilleteras() {
        Cola<Billetera> cola = new Cola<>();
        jdbc.query("SELECT codigo, identificacion_usuario, nombre, tipo, saldo, estado, umbral_saldo_bajo "
                + "FROM billeteras ORDER BY codigo",
                (RowCallbackHandler) rs -> {
                    Billetera billetera = new Billetera(rs.getString("codigo"), rs.getString("nombre"),
                            TipoBilletera.valueOf(rs.getString("tipo")), rs.getString("identificacion_usuario"),
                            rs.getBigDecimal("saldo"), rs.getBigDecimal("umbral_saldo_bajo"));
                    if ("BLOQUEADA".equals(rs.getString("estado"))) {
                        billetera.bloquear();
                    }
                    cola.encolar(billetera);
                });
        return cola;
    }

    /** Orden cronológico: por fecha y, si empatan, por el número del id (T2 antes que T10). */
    public Cola<Transaccion> cargarTransacciones() {
        Cola<Transaccion> cola = new Cola<>();
        jdbc.query("SELECT id, fecha, tipo, valor, codigo_origen, codigo_destino, estado, puntos_generados, "
                + "nivel_riesgo FROM transacciones "
                + "ORDER BY fecha, CAST(SUBSTRING(id FROM 2) AS INTEGER)",
                (RowCallbackHandler) rs -> {
                    Transaccion transaccion = new Transaccion(rs.getString("id"),
                            rs.getTimestamp("fecha").toLocalDateTime(),
                            TipoTransaccion.valueOf(rs.getString("tipo")), rs.getBigDecimal("valor"),
                            rs.getString("codigo_origen"), rs.getString("codigo_destino"));
                    transaccion.setPuntosGenerados(rs.getInt("puntos_generados"));
                    transaccion.marcarRiesgo(NivelRiesgo.valueOf(rs.getString("nivel_riesgo")));
                    if ("REVERTIDA".equals(rs.getString("estado"))) {
                        transaccion.marcarRevertida();
                    }
                    cola.encolar(transaccion);
                });
        return cola;
    }

    /** Devuelve todas las programadas y deja en el montículo solo las pendientes. */
    public Cola<OperacionProgramada> cargarProgramadas(MonticuloBinario<OperacionProgramada> pendientes) {
        Cola<OperacionProgramada> cola = new Cola<>();
        jdbc.query("SELECT id, fecha_ejecucion, tipo, valor, codigo_origen, codigo_destino, estado, "
                + "periodicidad, repeticiones_restantes, avisada FROM operaciones_programadas "
                + "ORDER BY fecha_ejecucion, CAST(SUBSTRING(id FROM 2) AS INTEGER)",
                (RowCallbackHandler) rs -> {
                    OperacionProgramada operacion = new OperacionProgramada(rs.getString("id"),
                            rs.getTimestamp("fecha_ejecucion").toLocalDateTime(),
                            TipoTransaccion.valueOf(rs.getString("tipo")), rs.getBigDecimal("valor"),
                            rs.getString("codigo_origen"), rs.getString("codigo_destino"),
                            Periodicidad.valueOf(rs.getString("periodicidad")),
                            rs.getInt("repeticiones_restantes"));
                    String estado = rs.getString("estado");
                    if ("EJECUTADA".equals(estado)) {
                        operacion.marcarEjecutada();
                    } else if ("FALLIDA".equals(estado)) {
                        operacion.marcarFallida();
                    } else {
                        if (rs.getBoolean("avisada")) {
                            operacion.marcarAvisada();
                        }
                        pendientes.insertar(operacion);
                    }
                    cola.encolar(operacion);
                });
        return cola;
    }

    public Cola<Beneficio> cargarBeneficios() {
        Cola<Beneficio> cola = new Cola<>();
        jdbc.query("SELECT identificacion_usuario, descripcion, puntos_costo, fecha_canje "
                + "FROM beneficios ORDER BY id",
                (RowCallbackHandler) rs -> cola.encolar(new Beneficio(rs.getString("identificacion_usuario"),
                        rs.getString("descripcion"), rs.getInt("puntos_costo"),
                        rs.getTimestamp("fecha_canje").toLocalDateTime())));
        return cola;
    }

    public Cola<EventoAuditoria> cargarEventos() {
        Cola<EventoAuditoria> cola = new Cola<>();
        jdbc.query("SELECT fecha, identificacion_usuario, evento, nivel_riesgo, detalle FROM auditoria ORDER BY id",
                (RowCallbackHandler) rs -> cola.encolar(new EventoAuditoria(
                        rs.getTimestamp("fecha").toLocalDateTime(), rs.getString("identificacion_usuario"),
                        rs.getString("evento"), NivelRiesgo.valueOf(rs.getString("nivel_riesgo")),
                        rs.getString("detalle"))));
        return cola;
    }

    private static Timestamp ahora() {
        return Timestamp.valueOf(LocalDateTime.now());
    }
}