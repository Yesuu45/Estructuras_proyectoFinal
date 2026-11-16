package co.edu.uniquindio.poo.servicio;

import co.edu.uniquindio.poo.Estructuras.Cola;
import co.edu.uniquindio.poo.Estructuras.MonticuloBinario;
import co.edu.uniquindio.poo.Estructuras.TablaHash;
import co.edu.uniquindio.poo.modelo.Beneficio;
import co.edu.uniquindio.poo.modelo.BeneficioDisponible;
import co.edu.uniquindio.poo.modelo.Billetera;
import co.edu.uniquindio.poo.modelo.EstadoTransaccion;
import co.edu.uniquindio.poo.modelo.EventoAuditoria;
import co.edu.uniquindio.poo.modelo.Nivel;
import co.edu.uniquindio.poo.modelo.NivelRiesgo;
import co.edu.uniquindio.poo.modelo.Notificacion;
import co.edu.uniquindio.poo.modelo.OperacionProgramada;
import co.edu.uniquindio.poo.modelo.TipoAlerta;
import co.edu.uniquindio.poo.modelo.TipoBilletera;
import co.edu.uniquindio.poo.modelo.TipoTransaccion;
import co.edu.uniquindio.poo.modelo.Transaccion;
import co.edu.uniquindio.poo.modelo.Usuario;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/**
 * Fachada de la plataforma. La interfaz (controladores) solo habla con esta clase.
 * Por ahora los datos viven en memoria; la persistencia en PostgreSQL llega después.
 */
@Service
public class PlataformaService {

    private static final BigDecimal MONTO_PARA_PUNTOS = new BigDecimal("100");
    private static final int BONO_PAGO_PROGRAMADO = 5;

    private final TablaHash<String, Usuario> usuarios = new TablaHash<>();
    private final TablaHash<String, Billetera> billeteras = new TablaHash<>();
    private final TablaHash<String, Transaccion> transacciones = new TablaHash<>();
    private final TablaHash<String, OperacionProgramada> programadas = new TablaHash<>();
    private final MonticuloBinario<OperacionProgramada> pendientes = new MonticuloBinario<>();
    private final TablaHash<String, BeneficioDisponible> catalogo = cargarCatalogo();
    private final Cola<Beneficio> beneficios = new Cola<>();
    private final Cola<EventoAuditoria> eventos = new Cola<>();
    private final Cola<Notificacion> notificaciones = new Cola<>();
    private int secuencia = 0;

    // ---------- Usuarios ----------

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

    // ---------- Billeteras ----------

    public Billetera crearBilletera(String codigo, String nombre, TipoBilletera tipo,
                                    String identificacionTitular, BigDecimal saldoInicial,
                                    BigDecimal umbralSaldoBajo) {
        Usuario titular = buscarUsuario(identificacionTitular);
        String codigoFinal = normalizar(codigo) == null ? generarCodigoBilletera() : normalizar(codigo);
        if (billeteras.containsKey(codigoFinal)) {
            throw new IllegalStateException("Ya existe la billetera " + codigoFinal);
        }
        Billetera billetera = new Billetera(codigoFinal, nombre, tipo, identificacionTitular,
                saldoInicial, umbralSaldoBajo);
        billeteras.put(codigo, billetera);
        titular.agregarBilletera(billetera);
        return billetera;
    }

    public Billetera buscarBilletera(String codigo) {
        if (codigo == null) {
            throw new IllegalArgumentException("El código de la billetera es obligatorio");
        }
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

    // ---------- Transacciones ----------

    public Transaccion operar(TipoTransaccion tipo, String origen, String destino, BigDecimal valor) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de operación es obligatorio");
        }
        String codigoOrigen = normalizar(origen);
        String codigoDestino = normalizar(destino);
        return switch (tipo) {
            case RECARGA -> recargar(codigoDestino, valor);
            case RETIRO -> retirar(codigoOrigen, valor);
            case TRANSFERENCIA -> transferir(codigoOrigen, codigoDestino, valor);
            case PAGO_PROGRAMADO -> throw new IllegalArgumentException(
                    "Los pagos programados se crean en la sección Programadas");
        };
    }

    public Transaccion recargar(String codigoDestino, BigDecimal valor) {
        Billetera destino = buscarBilletera(codigoDestino);
        destino.recargar(valor);
        Transaccion transaccion = crearTransaccion(TipoTransaccion.RECARGA, valor, null, codigoDestino);
        premiar(destino.getIdentificacionTitular(), transaccion, puntosPor(valor, 1));
        registrarEvento(destino.getIdentificacionTitular(), "RECARGA", NivelRiesgo.BAJO, "Monto " + valor);
        return transaccion;
    }

    public Transaccion retirar(String codigoOrigen, BigDecimal valor) {
        Billetera origen = buscarBilletera(codigoOrigen);
        origen.retirar(valor);
        Transaccion transaccion = crearTransaccion(TipoTransaccion.RETIRO, valor, codigoOrigen, null);
        premiar(origen.getIdentificacionTitular(), transaccion, puntosPor(valor, 2));
        registrarEvento(origen.getIdentificacionTitular(), "RETIRO", NivelRiesgo.BAJO, "Monto " + valor);
        verificarSaldoBajo(origen);
        return transaccion;
    }

    public Transaccion transferir(String codigoOrigen, String codigoDestino, BigDecimal valor) {
        return ejecutarMovimiento(TipoTransaccion.TRANSFERENCIA, codigoOrigen, codigoDestino, valor, 3);
    }

    public Transaccion revertir(String id) {
        Transaccion transaccion = buscarTransaccion(id);
        if (transaccion.getEstado() != EstadoTransaccion.COMPLETADA) {
            throw new IllegalStateException("Solo se puede revertir una transacción completada");
        }
        Billetera origen = transaccion.getCodigoOrigen() == null
                ? null : buscarBilletera(transaccion.getCodigoOrigen());
        Billetera destino = transaccion.getCodigoDestino() == null
                ? null : buscarBilletera(transaccion.getCodigoDestino());

        if (origen != null && !origen.isActiva()) {
            throw new IllegalStateException("La billetera origen está bloqueada");
        }
        if (destino != null && destino.getSaldo().compareTo(transaccion.getValor()) < 0) {
            throw new IllegalStateException("La billetera destino no tiene saldo suficiente para revertir");
        }
        String titular = origen != null ? origen.getIdentificacionTitular() : destino.getIdentificacionTitular();
        Usuario usuarioTitular = buscarUsuario(titular);
        if (usuarioTitular.getPuntos() < transaccion.getPuntosGenerados()) {
            throw new IllegalStateException("El usuario no tiene puntos suficientes para revertir");
        }

        if (destino != null) {
            destino.retirar(transaccion.getValor());
        }
        if (origen != null) {
            origen.recargar(transaccion.getValor());
        }
        usuarioTitular.restarPuntos(transaccion.getPuntosGenerados());
        transaccion.marcarRevertida();
        registrarEvento(titular, "REVERSION", NivelRiesgo.MEDIO, "Transacción " + id + " revertida");
        return transaccion;
    }

    public Transaccion buscarTransaccion(String id) {
        Transaccion transaccion = transacciones.get(id);
        if (transaccion == null) {
            throw new IllegalArgumentException("No existe la transacción " + id);
        }
        return transaccion;
    }

    public Transaccion[] listarTransacciones() {
        Cola<String> claves = transacciones.claves();
        Transaccion[] resultado = new Transaccion[claves.getSize()];
        for (int i = 0; i < resultado.length; i++) {
            resultado[i] = transacciones.get(claves.desencolar());
        }
        return resultado;
    }

    // ---------- Operaciones programadas ----------

    public OperacionProgramada programar(LocalDateTime fecha, TipoTransaccion tipo,
                                         String codigoOrigen, String codigoDestino, BigDecimal valor) {
        if (tipo != TipoTransaccion.TRANSFERENCIA && tipo != TipoTransaccion.PAGO_PROGRAMADO) {
            throw new IllegalArgumentException("Solo se pueden programar transferencias o pagos");
        }
        String origen = normalizar(codigoOrigen);
        String destino = normalizar(codigoDestino);
        buscarBilletera(origen);
        buscarBilletera(destino);

        String id = "P" + (++secuencia);
        OperacionProgramada operacion = new OperacionProgramada(id, fecha, tipo, valor, origen, destino);
        programadas.put(id, operacion);
        pendientes.insertar(operacion);
        return operacion;
    }

    public int ejecutarPendientes() {
        int ejecutadas = 0;
        while (!pendientes.isEmpty()) {
            OperacionProgramada operacion = pendientes.extraerMinimo();
            String titular = buscarBilletera(operacion.getCodigoOrigen()).getIdentificacionTitular();
            try {
                ejecutarMovimiento(operacion.getTipo(), operacion.getCodigoOrigen(),
                        operacion.getCodigoDestino(), operacion.getValor(), 3);
                if (operacion.getTipo() == TipoTransaccion.PAGO_PROGRAMADO) {
                    sumarPuntos(titular, BONO_PAGO_PROGRAMADO);
                }
                operacion.marcarEjecutada();
                notificar(titular, TipoAlerta.PAGO_PROGRAMADO_EJECUTADO,
                        "La operación " + operacion.getId() + " se ejecutó");
                ejecutadas++;
            } catch (IllegalArgumentException | IllegalStateException e) {
                operacion.marcarFallida();
                notificar(titular, TipoAlerta.PAGO_PROGRAMADO_EJECUTADO,
                        "La operación " + operacion.getId() + " falló: " + e.getMessage());
            }
        }
        return ejecutadas;
    }

    public OperacionProgramada[] listarProgramadas() {
        Cola<String> claves = programadas.claves();
        OperacionProgramada[] resultado = new OperacionProgramada[claves.getSize()];
        for (int i = 0; i < resultado.length; i++) {
            resultado[i] = programadas.get(claves.desencolar());
        }
        return resultado;
    }

    // ---------- Beneficios ----------

    public Beneficio canjear(String identificacion, String idBeneficio) {
        Usuario usuario = buscarUsuario(identificacion);
        BeneficioDisponible oferta = buscarBeneficioDisponible(idBeneficio);
        if (usuario.getPuntos() < oferta.getPuntosCosto()) {
            throw new IllegalStateException("Puntos insuficientes: tienes " + usuario.getPuntos()
                    + " y el beneficio cuesta " + oferta.getPuntosCosto());
        }
        usuario.restarPuntos(oferta.getPuntosCosto());
        Beneficio beneficio = new Beneficio(identificacion, oferta.getNombre(),
                oferta.getPuntosCosto(), LocalDateTime.now());
        beneficios.encolar(beneficio);
        registrarEvento(identificacion, "CANJE_BENEFICIO", NivelRiesgo.BAJO,
                oferta.getNombre() + " (" + oferta.getPuntosCosto() + " pts)");
        return beneficio;
    }

    public BeneficioDisponible[] listarCatalogo() {
        Cola<String> claves = catalogo.claves();
        BeneficioDisponible[] resultado = new BeneficioDisponible[claves.getSize()];
        for (int i = 0; i < resultado.length; i++) {
            resultado[i] = catalogo.get(claves.desencolar());
        }
        return resultado;
    }

    private BeneficioDisponible buscarBeneficioDisponible(String id) {
        if (normalizar(id) == null) {
            throw new IllegalArgumentException("Debes elegir un beneficio");
        }
        BeneficioDisponible oferta = catalogo.get(id);
        if (oferta == null) {
            throw new IllegalArgumentException("No existe el beneficio " + id);
        }
        return oferta;
    }

    private static TablaHash<String, BeneficioDisponible> cargarCatalogo() {
        TablaHash<String, BeneficioDisponible> oferta = new TablaHash<>();
        oferta.put("D5", new BeneficioDisponible("D5", "Descuento del 5% en comercios aliados", 500));
        oferta.put("C1", new BeneficioDisponible("C1", "Cupón de 10.000 para recargas", 1500));
        oferta.put("E1", new BeneficioDisponible("E1", "Entrada a evento de la plataforma", 2000));
        return oferta;
    }

    private String generarCodigoBilletera() {
        String codigo;
        do {
            long numero = (long) (Math.random() * 100_000_000L);
            codigo = "B" + String.format("%08d", numero);
        } while (billeteras.containsKey(codigo));
        return codigo;
    }

    public Beneficio[] listarBeneficios() {
        Cola<Beneficio> copia = beneficios.clonarCola();
        Beneficio[] resultado = new Beneficio[copia.getSize()];
        for (int i = 0; i < resultado.length; i++) {
            resultado[i] = copia.desencolar();
        }
        return resultado;
    }

    // ---------- Auditoría y notificaciones ----------

    public EventoAuditoria[] listarEventos() {
        Cola<EventoAuditoria> copia = eventos.clonarCola();
        EventoAuditoria[] resultado = new EventoAuditoria[copia.getSize()];
        for (int i = 0; i < resultado.length; i++) {
            resultado[i] = copia.desencolar();
        }
        return resultado;
    }

    public Notificacion[] listarNotificaciones() {
        Cola<Notificacion> copia = notificaciones.clonarCola();
        Notificacion[] resultado = new Notificacion[copia.getSize()];
        for (int i = 0; i < resultado.length; i++) {
            resultado[i] = copia.desencolar();
        }
        return resultado;
    }

    // ---------- Lógica interna ----------

    private Transaccion ejecutarMovimiento(TipoTransaccion tipo, String codigoOrigen, String codigoDestino,
                                           BigDecimal valor, int factorPuntos) {
        if (codigoOrigen == null || codigoDestino == null) {
            throw new IllegalArgumentException("La operación necesita billetera origen y destino");
        }
        if (codigoOrigen.equals(codigoDestino)) {
            throw new IllegalArgumentException("La billetera origen y destino deben ser distintas");
        }
        Billetera origen = buscarBilletera(codigoOrigen);
        Billetera destino = buscarBilletera(codigoDestino);
        if (!destino.isActiva()) {
            throw new IllegalStateException("La billetera destino está bloqueada");
        }
        origen.retirar(valor);
        destino.recargar(valor);
        Transaccion transaccion = crearTransaccion(tipo, valor, codigoOrigen, codigoDestino);
        premiar(origen.getIdentificacionTitular(), transaccion, puntosPor(valor, factorPuntos));
        registrarEvento(origen.getIdentificacionTitular(), tipo.name(), NivelRiesgo.BAJO,
                "Monto " + valor + " hacia " + codigoDestino);
        verificarSaldoBajo(origen);
        return transaccion;
    }

    private Transaccion crearTransaccion(TipoTransaccion tipo, BigDecimal valor,
                                         String codigoOrigen, String codigoDestino) {
        String id = "T" + (++secuencia);
        Transaccion transaccion = new Transaccion(id, LocalDateTime.now(), tipo, valor,
                codigoOrigen, codigoDestino);
        transacciones.put(id, transaccion);
        return transaccion;
    }

    private void premiar(String identificacion, Transaccion transaccion, int puntos) {
        transaccion.setPuntosGenerados(puntos);
        if (puntos > 0) {
            sumarPuntos(identificacion, puntos);
        }
    }

    private void sumarPuntos(String identificacion, int puntos) {
        Usuario usuario = buscarUsuario(identificacion);
        Nivel anterior = usuario.getNivel();
        usuario.sumarPuntos(puntos);
        if (usuario.getNivel() != anterior) {
            notificar(identificacion, TipoAlerta.ASCENSO_NIVEL, "Subiste al nivel " + usuario.getNivel());
        }
    }

    private void verificarSaldoBajo(Billetera billetera) {
        if (billetera.tieneSaldoBajo()) {
            notificar(billetera.getIdentificacionTitular(), TipoAlerta.SALDO_BAJO,
                    "La billetera " + billetera.getCodigo() + " tiene saldo bajo");
        }
    }

    private void registrarEvento(String identificacion, String evento, NivelRiesgo riesgo, String detalle) {
        eventos.encolar(new EventoAuditoria(LocalDateTime.now(), identificacion, evento, riesgo, detalle));
    }

    private void notificar(String identificacion, TipoAlerta tipo, String mensaje) {
        notificaciones.encolar(new Notificacion(identificacion, tipo, mensaje, LocalDateTime.now()));
    }

    private static int puntosPor(BigDecimal valor, int factor) {
        return valor.divideToIntegralValue(MONTO_PARA_PUNTOS).intValue() * factor;
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}