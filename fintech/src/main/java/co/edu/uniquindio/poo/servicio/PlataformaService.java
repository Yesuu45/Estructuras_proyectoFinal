package co.edu.uniquindio.poo.servicio;

import co.edu.uniquindio.poo.Estructuras.*;
import co.edu.uniquindio.poo.modelo.*;
import co.edu.uniquindio.poo.persistencia.RepositorioPlataforma;
import co.edu.uniquindio.poo.seguridad.AccesoDenegadoException;
import co.edu.uniquindio.poo.seguridad.ClaveSegura;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Fachada de la plataforma. La interfaz (controladores) solo habla con esta clase.
 * Los datos se guardan en PostgreSQL con cada cambio y se reconstruyen al arrancar.
 * Las notificaciones y el reloj simulado no se guardan: se reinician al arrancar.
 */
@Service
public class PlataformaService {

    private static final BigDecimal MONTO_PARA_PUNTOS = new BigDecimal("100");
    private static final int BONO_PAGO_PROGRAMADO = 5;
    private static final int TOPE_NOTIFICACIONES = 10;       // notificaciones recientes que se conservan por usuario
    private static final int HORAS_AVISO_PROGRAMADA = 24;    // aviso previo de una operación programada
    private static final int MAX_OCURRENCIAS = 52;
    private static final int MAX_CICLOS = 100;
    private static final int MAX_RUTAS = 5000;

    private final RepositorioPlataforma repositorio;

    private final TablaHash<String, Usuario> usuarios = new TablaHash<>();
    private final TablaHash<String, Billetera> billeteras = new TablaHash<>();
    private final TablaHash<String, Transaccion> transacciones = new TablaHash<>();
    private final TablaHash<String, OperacionProgramada> programadas = new TablaHash<>();
    private final MonticuloBinario<OperacionProgramada> pendientes = new MonticuloBinario<>();
    private final TablaHash<String, BeneficioDisponible> catalogo = cargarCatalogo();
    // Listas enlazadas: libro cronológico de todas las transacciones y de todos los canjes
    private final SimpleLinkedList<Transaccion> libro = new SimpleLinkedList<>();
    private final SimpleLinkedList<Beneficio> beneficios = new SimpleLinkedList<>();
    // Árbol binario de búsqueda con los usuarios ordenados por puntos (ranking y búsquedas por rango)
    private final ArbolBinario<ClavePuntos> ranking = new ArbolBinario<>();
    // Grafos dirigidos de transferencias: entre usuarios y entre billeteras
    private final Grafo<String> redUsuarios = new Grafo<>();
    private final Grafo<String> redBilleteras = new Grafo<>();
    private final Cola<EventoAuditoria> eventos = new Cola<>();
    private final Cola<Notificacion> notificaciones = new Cola<>();
    // Historial especial de auditoría: solo los eventos de comportamiento inusual
    private final SimpleLinkedList<EventoAuditoria> eventosInusuales = new SimpleLinkedList<>();
    // Cuántas notificaciones recientes tiene cada usuario (para aplicar el tope)
    private final TablaHash<String, Integer> conteoNotificaciones = new TablaHash<>();
    // Reloj simulado: la hora de la plataforma es la real más este desfase (cero = reloj real)
    private Duration desfase = Duration.ZERO;
    private int secuencia = 0;

    public PlataformaService(RepositorioPlataforma repositorio) {
        this.repositorio = repositorio;
        restaurar();
    }

    // ---------- Arranque: reconstruir el estado desde PostgreSQL ----------

    /** Carga lo guardado y reconstruye las estructuras derivadas (ranking, redes, pilas, historiales). */
    private void restaurar() {
        secuencia = repositorio.ultimaSecuencia();

        Cola<Usuario> cargadosUsuarios = repositorio.cargarUsuarios();
        while (!cargadosUsuarios.isEmpty()) {
            Usuario usuario = cargadosUsuarios.desencolar();
            usuarios.put(usuario.getIdentificacion(), usuario);
            if (!usuario.esAdmin()) {
                redUsuarios.agregarVertice(usuario.getIdentificacion());
                ranking.insertar(clave(usuario));
            }
        }

        Cola<Billetera> cargadasBilleteras = repositorio.cargarBilleteras();
        while (!cargadasBilleteras.isEmpty()) {
            Billetera billetera = cargadasBilleteras.desencolar();
            billeteras.put(billetera.getCodigo(), billetera);
            redBilleteras.agregarVertice(billetera.getCodigo());
            usuarios.get(billetera.getIdentificacionTitular()).agregarBilletera(billetera);
        }

        // En orden cronológico: historiales, redes y pilas quedan como al crearse
        Cola<Transaccion> cargadasTransacciones = repositorio.cargarTransacciones();
        while (!cargadasTransacciones.isEmpty()) {
            Transaccion transaccion = cargadasTransacciones.desencolar();
            transacciones.put(transaccion.getId(), transaccion);
            libro.addLast(transaccion);
            registrarEnHistoriales(transaccion, transaccion.getCodigoOrigen(), transaccion.getCodigoDestino());
            apilarReversible(transaccion);
            if (transaccion.getEstado() == EstadoTransaccion.REVERTIDA) {
                // Igual que revertir(): sale de reversibles y queda en deshechas; su movimiento ya no cuenta en la red
                String codigo = transaccion.getCodigoOrigen() != null
                        ? transaccion.getCodigoOrigen() : transaccion.getCodigoDestino();
                Usuario titular = usuarios.get(billeteras.get(codigo).getIdentificacionTitular());
                titular.pilaReversibles().eliminar(transaccion);
                titular.pilaDeshechas().push(transaccion);
            } else if (transaccion.getTipo() == TipoTransaccion.TRANSFERENCIA
                    || transaccion.getTipo() == TipoTransaccion.PAGO_PROGRAMADO) {
                registrarEnLaRed(billeteras.get(transaccion.getCodigoOrigen()),
                        billeteras.get(transaccion.getCodigoDestino()), transaccion.getValor());
            }
        }

        Cola<OperacionProgramada> cargadasProgramadas = repositorio.cargarProgramadas(pendientes);
        while (!cargadasProgramadas.isEmpty()) {
            OperacionProgramada operacion = cargadasProgramadas.desencolar();
            programadas.put(operacion.getId(), operacion);
        }

        Cola<Beneficio> cargadosCanjes = repositorio.cargarBeneficios();
        while (!cargadosCanjes.isEmpty()) {
            Beneficio beneficio = cargadosCanjes.desencolar();
            beneficios.addLast(beneficio);
            usuarios.get(beneficio.getIdentificacionUsuario()).registrarCanje(beneficio);
        }

        Cola<EventoAuditoria> cargadosEventos = repositorio.cargarEventos();
        while (!cargadosEventos.isEmpty()) {
            EventoAuditoria evento = cargadosEventos.desencolar();
            eventos.encolar(evento);
            if ("COMPORTAMIENTO_INUSUAL".equals(evento.getEvento())) {   // verificar que EventoAuditoria tenga getEvento()
                eventosInusuales.addLast(evento);
            }
        }
    }

    // ---------- Persistencia: escrituras puntuales ----------

    private void persistirUsuario(Usuario usuario) {
        repositorio.actualizarUsuario(usuario.getIdentificacion(), usuario.getNombre(), usuario.getCorreo(),
                usuario.hashClave(), usuario.getPuntos(), usuario.getNivel());
    }

    private void persistirBilletera(Billetera billetera) {
        repositorio.actualizarBilletera(billetera.getCodigo(), billetera.getSaldo(),
                billetera.isActiva(), billetera.getUmbralSaldoBajo());
    }

    private void persistirTransaccion(Transaccion transaccion) {
        repositorio.actualizarTransaccion(transaccion.getId(), transaccion.getEstado(),
                transaccion.getPuntosGenerados(), transaccion.getNivelRiesgo());
    }

    private void persistirProgramada(OperacionProgramada operacion) {
        repositorio.actualizarProgramada(operacion.getId(), operacion.getEstado(), operacion.isAvisada());
    }

    private void persistirNuevaProgramada(OperacionProgramada operacion) {
        repositorio.insertarProgramada(operacion.getId(), operacion.getFechaEjecucion(), operacion.getTipo(),
                operacion.getValor(), operacion.getCodigoOrigen(), operacion.getCodigoDestino(),
                operacion.getPeriodicidad(), operacion.getRepeticionesRestantes());
    }

    // ---------- Usuarios ----------

    public Usuario registrarUsuario(String identificacion, String nombre, String correo, String clave) {
        if (usuarios.containsKey(identificacion)) {
            throw new IllegalStateException("Ya existe un usuario con identificación " + identificacion);
        }
        Usuario usuario = new Usuario(identificacion, nombre, correo, ClaveSegura.generarHash(clave));
        usuarios.put(usuario.getIdentificacion(), usuario);
        ranking.insertar(clave(usuario));
        redUsuarios.agregarVertice(usuario.getIdentificacion());
        repositorio.insertarUsuario(usuario.getIdentificacion(), usuario.getNombre(), usuario.getCorreo(),
                usuario.hashClave(), usuario.getRol(), usuario.getNivel());
        return usuario;
    }

    /** Crea la cuenta de administrador si no existe. La llama InicializadorAdmin al arrancar. */
    public void sembrarAdministrador(String identificacion, String nombre, String clave) {
        if (usuarios.containsKey(identificacion)) {
            return;
        }
        Usuario admin = new Usuario(identificacion, nombre, "", ClaveSegura.generarHash(clave), Rol.ADMIN);
        usuarios.put(identificacion, admin);
        repositorio.insertarUsuario(identificacion, nombre, "", admin.hashClave(), Rol.ADMIN, admin.getNivel());
    }

    /** Mismo mensaje si falla la identificación o la contraseña, para no revelar qué usuarios existen. */
    public Usuario autenticar(String identificacion, String clave) {
        Usuario usuario = identificacion == null ? null : usuarios.get(identificacion.trim());
        if (usuario == null || !usuario.coincideClave(clave)) {
            throw new IllegalArgumentException("Identificación o contraseña incorrectas");
        }
        return usuario;
    }

    public boolean existeUsuario(String identificacion) {
        return identificacion != null && usuarios.containsKey(identificacion);
    }

    public Usuario buscarUsuario(String identificacion) {
        Usuario usuario = usuarios.get(identificacion);
        if (usuario == null) {
            throw new IllegalArgumentException("No existe el usuario " + identificacion);
        }
        return usuario;
    }

    /** El administrador edita a cualquiera; un usuario solo a sí mismo. La clave nueva es opcional. */
    public Usuario modificarUsuario(String actor, String identificacion, String nombre, String correo,
                                    String claveNueva) {
        exigirPropietario(actor, identificacion == null ? "" : identificacion,
                "Solo puedes modificar tu propio perfil");
        Usuario usuario = buscarUsuario(identificacion);
        boolean cambiaClave = claveNueva != null && !claveNueva.isBlank();
        String hashNuevo = cambiaClave ? ClaveSegura.generarHash(claveNueva) : null;   // valida antes de tocar nada
        usuario.actualizarPerfil(nombre, correo);
        if (cambiaClave) {
            usuario.cambiarClave(hashNuevo);
        }
        persistirUsuario(usuario);
        registrarEvento(actor, "USUARIO_MODIFICADO", NivelRiesgo.BAJO,
                "Perfil de " + identificacion + (cambiaClave ? " (con cambio de contraseña)" : ""));
        return usuario;
    }

    /**
     * Solo el administrador. No se elimina al administrador ni a quien ya tiene billeteras,
     * porque sus movimientos siguen en el historial, el ranking y la red de transferencias.
     */
    public void eliminarUsuario(String actor, String identificacion) {
        exigirAdmin(actor, "Solo el administrador puede eliminar usuarios");
        Usuario usuario = buscarUsuario(identificacion);
        if (usuario.esAdmin()) {
            throw new IllegalStateException("No se puede eliminar la cuenta de administrador");
        }
        if (usuario.cantidadBilleteras() > 0) {
            throw new IllegalStateException("No se puede eliminar a " + identificacion + ": tiene "
                    + usuario.cantidadBilleteras() + " billetera(s) con movimientos registrados");
        }
        ranking.eliminar(clave(usuario));
        redUsuarios.eliminarVertice(identificacion);
        usuarios.remove(identificacion);
        repositorio.eliminarUsuario(identificacion);
        registrarEvento(actor, "USUARIO_ELIMINADO", NivelRiesgo.MEDIO, "Usuario " + identificacion + " eliminado");
    }

    /** Solo el administrador puede ver a todos los usuarios. */
    public Usuario[] listarUsuarios(String actor) {
        exigirAdmin(actor, "Solo el administrador puede ver la lista de usuarios");
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
        billeteras.put(codigoFinal, billetera);
        redBilleteras.agregarVertice(codigoFinal);
        titular.agregarBilletera(billetera);
        repositorio.insertarBilletera(codigoFinal, identificacionTitular, billetera.getNombre(),
                tipo, billetera.getSaldo(), billetera.getUmbralSaldoBajo());
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

    /** El administrador ve todas; un usuario normal, solo las suyas. */
    public Billetera[] listarBilleteras(String actor) {
        boolean admin = esAdmin(actor);
        Cola<String> claves = billeteras.claves();
        Cola<Billetera> visibles = new Cola<>();
        while (!claves.isEmpty()) {
            Billetera billetera = billeteras.get(claves.desencolar());
            if (admin || billetera.getIdentificacionTitular().equals(actor)) {
                visibles.encolar(billetera);
            }
        }
        Billetera[] resultado = new Billetera[visibles.getSize()];
        for (int i = 0; i < resultado.length; i++) {
            resultado[i] = visibles.desencolar();
        }
        return resultado;
    }

    // ---------- Transacciones ----------

    public Transaccion operar(String actor, TipoTransaccion tipo, String origen, String destino, BigDecimal valor) {
        try {
            return ejecutarOperacion(actor, tipo, origen, destino, valor);
        } catch (IllegalStateException | IllegalArgumentException e) {
            avisarRechazo(actor, tipo, e.getMessage());
            throw e;
        }
    }

    /** Operación rechazada por reglas de negocio (saldo, límite, billetera bloqueada...): alerta y auditoría. */
    private void avisarRechazo(String actor, TipoTransaccion tipo, String motivo) {
        if (actor == null || !usuarios.containsKey(actor)) {
            return;
        }
        String operacion = tipo == null ? "La operación" : tipo.name();
        notificar(actor, TipoAlerta.OPERACION_RECHAZADA, operacion + " rechazada: " + motivo);
        registrarEvento(actor, "OPERACION_RECHAZADA", NivelRiesgo.MEDIO, operacion + ": " + motivo);
    }

    private Transaccion ejecutarOperacion(String actor, TipoTransaccion tipo, String origen, String destino,
                                          BigDecimal valor) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de operación es obligatorio");
        }
        String codigoOrigen = normalizar(origen);
        String codigoDestino = normalizar(destino);
        return switch (tipo) {
            case RECARGA -> {
                exigirBilleteraPropia(actor, codigoDestino);
                yield recargar(codigoDestino, valor);
            }
            case RETIRO -> {
                exigirBilleteraPropia(actor, codigoOrigen);
                yield retirar(codigoOrigen, valor);
            }
            case TRANSFERENCIA -> {
                // Solo el origen debe ser propio: se puede transferir a billeteras de otras personas
                exigirBilleteraPropia(actor, codigoOrigen);
                yield transferir(codigoOrigen, codigoDestino, valor);
            }
            case PAGO_PROGRAMADO -> throw new IllegalArgumentException(
                    "Los pagos programados se crean en la sección Programadas");
        };
    }

    public Transaccion recargar(String codigoDestino, BigDecimal valor) {
        Billetera destino = buscarBilletera(codigoDestino);
        destino.recargar(valor);
        persistirBilletera(destino);
        Transaccion transaccion = crearTransaccion(TipoTransaccion.RECARGA, valor, null, codigoDestino);
        premiar(destino.getIdentificacionTitular(), transaccion, puntosPor(valor, 1));
        registrarEvento(destino.getIdentificacionTitular(), "RECARGA", NivelRiesgo.BAJO, "Monto " + valor);
        return transaccion;
    }

    public Transaccion retirar(String codigoOrigen, BigDecimal valor) {
        Billetera origen = buscarBilletera(codigoOrigen);
        exigirLimiteDelNivel(origen, valor);
        origen.retirar(valor);
        persistirBilletera(origen);
        Transaccion transaccion = crearTransaccion(TipoTransaccion.RETIRO, valor, codigoOrigen, null);
        premiar(origen.getIdentificacionTitular(), transaccion, puntosPor(valor, 2));
        evaluarRiesgo(transaccion, origen);
        registrarEvento(origen.getIdentificacionTitular(), "RETIRO", NivelRiesgo.BAJO, "Monto " + valor);
        verificarSaldoBajo(origen);
        return transaccion;
    }

    public Transaccion transferir(String codigoOrigen, String codigoDestino, BigDecimal valor) {
        return ejecutarMovimiento(TipoTransaccion.TRANSFERENCIA, codigoOrigen, codigoDestino, valor, 3);
    }

    public Transaccion revertir(String actor, String id) {
        Transaccion transaccion = buscarTransaccion(id);
        if (transaccion.getEstado() != EstadoTransaccion.COMPLETADA) {
            throw new IllegalStateException("Solo se puede revertir una transacción completada");
        }
        Billetera origen = transaccion.getCodigoOrigen() == null
                ? null : buscarBilletera(transaccion.getCodigoOrigen());
        Billetera destino = transaccion.getCodigoDestino() == null
                ? null : buscarBilletera(transaccion.getCodigoDestino());

        String titular = origen != null ? origen.getIdentificacionTitular() : destino.getIdentificacionTitular();
        exigirPropietario(actor, titular, "Solo quien hizo la transacción puede revertirla");

        if (origen != null && !origen.isActiva()) {
            throw new IllegalStateException("La billetera origen está bloqueada");
        }
        if (destino != null && destino.getSaldo().compareTo(transaccion.getValor()) < 0) {
            throw new IllegalStateException("La billetera destino no tiene saldo suficiente para revertir");
        }
        Usuario usuarioTitular = buscarUsuario(titular);
        if (usuarioTitular.getPuntos() < transaccion.getPuntosGenerados()) {
            throw new IllegalStateException("El usuario no tiene puntos suficientes para revertir");
        }

        if (destino != null) {
            destino.retirar(transaccion.getValor());
            persistirBilletera(destino);
        }
        if (origen != null) {
            origen.recargar(transaccion.getValor());
            persistirBilletera(origen);
        }
        cambiarPuntos(usuarioTitular, () -> usuarioTitular.restarPuntos(transaccion.getPuntosGenerados()));
        transaccion.marcarRevertida();
        persistirTransaccion(transaccion);
        if (origen != null && destino != null) {
            quitarDeLaRed(origen, destino, transaccion.getValor());
        }
        usuarioTitular.pilaReversibles().eliminar(transaccion);   // sale de la pila de reversibles (esté donde esté)...
        usuarioTitular.pilaDeshechas().push(transaccion);        // ...y queda arriba del historial de deshacer
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

    public Transaccion[] listarTransacciones(String actor) {
        return listarTransacciones(actor, null, null);
    }

    /**
     * Movimientos en orden cronológico, opcionalmente dentro de un período (desde y hasta incluidos; null = sin límite).
     * El administrador recorre el libro completo; un usuario normal, solo su propio historial.
     */
    public Transaccion[] listarTransacciones(String actor, LocalDateTime desde, LocalDateTime hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
        }
        SimpleLinkedList<Transaccion> fuente = esAdmin(actor) ? libro : buscarUsuario(actor).historialMovimientos();
        SimpleLinkedList<Transaccion> enPeriodo = new SimpleLinkedList<>();
        for (NodeSimple<Transaccion> nodo = fuente.getFirstNode(); nodo != null; nodo = nodo.getNext()) {
            LocalDateTime fecha = nodo.getData().getFecha();
            if ((desde == null || !fecha.isBefore(desde)) && (hasta == null || !fecha.isAfter(hasta))) {
                enPeriodo.addLast(nodo.getData());
            }
        }
        return enPeriodo.toArray(new Transaccion[0]);
    }

    /** Historial de una billetera. Solo su titular o el administrador. */
    public Transaccion[] historialBilletera(String actor, String codigo) {
        Billetera billetera = buscarBilletera(codigo);
        exigirPropietario(actor, billetera.getIdentificacionTitular(), "Esa billetera no es tuya");
        return billetera.historialMovimientos().toArray(new Transaccion[0]);
    }

    // ---------- Operaciones programadas ----------

    public OperacionProgramada programar(String actor, LocalDateTime fecha, TipoTransaccion tipo,
                                         String codigoOrigen, String codigoDestino, BigDecimal valor) {
        return programar(actor, fecha, tipo, codigoOrigen, codigoDestino, valor, Periodicidad.UNICA, 1);
    }

    /**
     * Programa una operación futura. Con periodicidad distinta de UNICA se repite hasta completar
     * 'ocurrencias' ejecuciones en total (recargas automáticas, ahorro semanal, pagos recurrentes...).
     * Las recargas programadas solo llevan billetera destino.
     */
    public OperacionProgramada programar(String actor, LocalDateTime fecha, TipoTransaccion tipo,
                                         String codigoOrigen, String codigoDestino, BigDecimal valor,
                                         Periodicidad periodicidad, Integer ocurrencias) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de operación es obligatorio");
        }
        if (tipo == TipoTransaccion.RETIRO) {
            throw new IllegalArgumentException("Solo se pueden programar recargas, transferencias o pagos");
        }
        Periodicidad frecuencia = periodicidad == null ? Periodicidad.UNICA : periodicidad;
        int total = frecuencia == Periodicidad.UNICA || ocurrencias == null ? 1 : ocurrencias;
        if (total < 1 || total > MAX_OCURRENCIAS) {
            throw new IllegalArgumentException("Las ocurrencias deben estar entre 1 y " + MAX_OCURRENCIAS);
        }
        String origen = normalizar(codigoOrigen);
        String destino = normalizar(codigoDestino);
        if (tipo == TipoTransaccion.RECARGA) {
            origen = null;
            exigirBilleteraPropia(actor, destino);
        } else {
            exigirBilleteraPropia(actor, origen);
            buscarBilletera(destino);
        }

        String id = "P" + (++secuencia);
        OperacionProgramada operacion = new OperacionProgramada(id, fecha, tipo, valor, origen, destino,
                frecuencia, total - 1);
        programadas.put(id, operacion);
        pendientes.insertar(operacion);
        persistirNuevaProgramada(operacion);
        avisarProximas();
        return operacion;
    }

    /** Ejecuta las operaciones vencidas (y avisa las próximas). Uso interno y pruebas; la API exige administrador. */
    public int ejecutarPendientes() {
        int ejecutadas = ejecutarVencidas();
        avisarProximas();
        return ejecutadas;
    }

    public int ejecutarPendientes(String actor) {
        exigirAdmin(actor, "Solo el administrador puede ejecutar las operaciones programadas");
        return ejecutarPendientes();
    }

    /**
     * Saca de la cola de prioridad, en orden de fecha, solo las operaciones cuya fecha ya llegó según el reloj
     * de la plataforma. Si la serie es periódica, deja programada la siguiente ocurrencia.
     */
    private int ejecutarVencidas() {
        int ejecutadas = 0;
        LocalDateTime ahora = ahora();
        while (!pendientes.isEmpty() && !pendientes.peek().getFechaEjecucion().isAfter(ahora)) {
            OperacionProgramada operacion = pendientes.extraerMinimo();
            String titular = titularDe(operacion);
            try {
                if (operacion.getTipo() == TipoTransaccion.RECARGA) {
                    recargar(operacion.getCodigoDestino(), operacion.getValor());
                } else {
                    ejecutarMovimiento(operacion.getTipo(), operacion.getCodigoOrigen(),
                            operacion.getCodigoDestino(), operacion.getValor(), 3);
                }
                if (operacion.getTipo() == TipoTransaccion.PAGO_PROGRAMADO) {
                    sumarPuntos(titular, BONO_PAGO_PROGRAMADO);
                }
                operacion.marcarEjecutada();
                notificar(titular, TipoAlerta.PAGO_PROGRAMADO_EJECUTADO,
                        "La operación " + operacion.getId() + " se ejecutó");
                ejecutadas++;
            } catch (IllegalArgumentException | IllegalStateException e) {
                operacion.marcarFallida();
                notificar(titular, TipoAlerta.OPERACION_RECHAZADA,
                        "La operación programada " + operacion.getId() + " falló: " + e.getMessage());
                registrarEvento(titular, "PROGRAMADA_FALLIDA", NivelRiesgo.MEDIO,
                        operacion.getId() + ": " + e.getMessage());
            }
            persistirProgramada(operacion);
            programarSiguiente(operacion);
        }
        return ejecutadas;
    }

    private void programarSiguiente(OperacionProgramada operacion) {
        if (operacion.tieneSiguiente()) {
            String id = "P" + (++secuencia);
            OperacionProgramada siguiente = operacion.crearSiguiente(id);
            programadas.put(id, siguiente);
            pendientes.insertar(siguiente);
            persistirNuevaProgramada(siguiente);
        }
    }

    private String titularDe(OperacionProgramada operacion) {
        String codigo = operacion.getCodigoOrigen() != null ? operacion.getCodigoOrigen() : operacion.getCodigoDestino();
        return buscarBilletera(codigo).getIdentificacionTitular();
    }

    /** Avisa (una sola vez) de las operaciones pendientes que se ejecutarán dentro de las próximas 24 horas. */
    public int avisarProximas() {
        LocalDateTime ahora = ahora();
        LocalDateTime limite = ahora.plusHours(HORAS_AVISO_PROGRAMADA);
        Cola<String> claves = programadas.claves();
        int avisadas = 0;
        while (!claves.isEmpty()) {
            OperacionProgramada operacion = programadas.get(claves.desencolar());
            LocalDateTime fecha = operacion.getFechaEjecucion();
            if (operacion.getEstado() == EstadoOperacionProgramada.PENDIENTE && !operacion.isAvisada()
                    && fecha.isAfter(ahora) && !fecha.isAfter(limite)) {
                operacion.marcarAvisada();
                persistirProgramada(operacion);
                notificar(titularDe(operacion), TipoAlerta.PROGRAMADA_PROXIMA,
                        "La operación " + operacion.getId() + " (" + operacion.getTipo() + " de "
                                + operacion.getValor().toPlainString() + ") se ejecutará el " + fecha);
                avisadas++;
            }
        }
        return avisadas;
    }

    // ---------- Reloj simulado ----------

    /** Hora de la plataforma: la real más el desfase simulado. Todo (fechas, programadas, alertas) usa esta hora. */
    public LocalDateTime ahora() {
        return LocalDateTime.now().plus(desfase);
    }

    public AvanceReloj estadoReloj() {
        return new AvanceReloj(ahora(), !desfase.isZero(), 0, 0);
    }

    /** Adelanta el reloj; si 'ejecutar', procesa de una vez las programadas que vencieron (ejecución automática simulada). */
    public AvanceReloj avanzarReloj(String actor, long minutos, boolean ejecutar) {
        exigirAdmin(actor, "Solo el administrador puede mover el reloj");
        if (minutos <= 0) {
            throw new IllegalArgumentException("Los minutos a avanzar deben ser mayores que cero");
        }
        desfase = desfase.plusMinutes(minutos);
        int ejecutadas = ejecutar ? ejecutarVencidas() : 0;
        int avisadas = avisarProximas();
        return new AvanceReloj(ahora(), !desfase.isZero(), avisadas, ejecutadas);
    }

    public AvanceReloj restablecerReloj(String actor) {
        exigirAdmin(actor, "Solo el administrador puede mover el reloj");
        desfase = Duration.ZERO;
        return estadoReloj();
    }

    /** Un usuario normal ve solo las operaciones programadas desde sus billeteras. */
    public OperacionProgramada[] listarProgramadas(String actor) {
        boolean admin = esAdmin(actor);
        Cola<String> claves = programadas.claves();
        Cola<OperacionProgramada> visibles = new Cola<>();
        while (!claves.isEmpty()) {
            OperacionProgramada operacion = programadas.get(claves.desencolar());
            if (admin || esBilleteraDe(actor, operacion.getCodigoOrigen() != null
                    ? operacion.getCodigoOrigen() : operacion.getCodigoDestino())) {
                visibles.encolar(operacion);
            }
        }
        OperacionProgramada[] resultado = new OperacionProgramada[visibles.getSize()];
        for (int i = 0; i < resultado.length; i++) {
            resultado[i] = visibles.desencolar();
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
        cambiarPuntos(usuario, () -> usuario.restarPuntos(oferta.getPuntosCosto()));
        Beneficio beneficio = new Beneficio(identificacion, oferta.getNombre(),
                oferta.getPuntosCosto(), ahora());
        beneficios.addLast(beneficio);
        usuario.registrarCanje(beneficio);
        repositorio.insertarBeneficio(identificacion, beneficio.getDescripcion(),
                beneficio.getPuntosCosto(), beneficio.getFechaCanje());
        registrarEvento(identificacion, "CANJE_BENEFICIO", NivelRiesgo.BAJO,
                oferta.getNombre() + " (" + oferta.getPuntosCosto() + " pts)");
        notificar(identificacion, TipoAlerta.CANJE_REALIZADO,
                "Canjeaste \"" + oferta.getNombre() + "\" por " + oferta.getPuntosCosto() + " puntos");
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

    /** El administrador ve todos los canjes; un usuario normal, los suyos. */
    public Beneficio[] listarBeneficios(String actor) {
        SimpleLinkedList<Beneficio> fuente = esAdmin(actor) ? beneficios : buscarUsuario(actor).canjesRealizados();
        return fuente.toArray(new Beneficio[0]);
    }

    // ---------- Auditoría y notificaciones ----------

    /** La auditoría completa es solo del administrador. */
    public EventoAuditoria[] listarEventos(String actor) {
        exigirAdmin(actor, "Solo el administrador puede ver la auditoría");
        Cola<EventoAuditoria> copia = eventos.clonarCola();
        EventoAuditoria[] resultado = new EventoAuditoria[copia.getSize()];
        for (int i = 0; i < resultado.length; i++) {
            resultado[i] = copia.desencolar();
        }
        return resultado;
    }

    public Notificacion[] listarNotificaciones(String actor) {
        boolean admin = esAdmin(actor);
        Cola<Notificacion> copia = notificaciones.clonarCola();
        Cola<Notificacion> visibles = new Cola<>();
        while (!copia.isEmpty()) {
            Notificacion notificacion = copia.desencolar();
            if (admin || notificacion.getIdentificacionUsuario().equals(actor)) {
                visibles.encolar(notificacion);
            }
        }
        Notificacion[] resultado = new Notificacion[visibles.getSize()];
        for (int i = 0; i < resultado.length; i++) {
            resultado[i] = visibles.desencolar();
        }
        return resultado;
    }

    /** Despacha las notificaciones pendientes: las propias (o todas, si es el administrador). Devuelve cuántas. */
    public int despacharNotificaciones(String actor) {
        boolean admin = esAdmin(actor);
        Cola<Notificacion> copia = notificaciones.clonarCola();
        int despachadas = 0;
        while (!copia.isEmpty()) {
            Notificacion notificacion = copia.desencolar();
            if (!notificacion.isDespachada() && (admin || notificacion.getIdentificacionUsuario().equals(actor))) {
                notificacion.marcarDespachada();
                despachadas++;
            }
        }
        return despachadas;
    }

    // ---------- Ventajas por nivel ----------

    public VentajaNivel[] ventajasPorNivel() {
        Nivel[] niveles = Nivel.values();
        VentajaNivel[] resultado = new VentajaNivel[niveles.length];
        for (int i = 0; i < niveles.length; i++) {
            Nivel nivel = niveles[i];
            Integer maximo = nivel.getMaximo() == Integer.MAX_VALUE ? null : nivel.getMaximo();
            resultado[i] = new VentajaNivel(nivel, nivel.getMinimo(), maximo, nivel.getLimiteOperacion());
        }
        return resultado;
    }

    private void exigirLimiteDelNivel(Billetera origen, BigDecimal valor) {
        if (valor == null) {
            return;   // la validación del valor la hace la billetera
        }
        Nivel nivel = usuarios.get(origen.getIdentificacionTitular()).getNivel();
        BigDecimal limite = nivel.getLimiteOperacion();
        if (limite != null && valor.compareTo(limite) > 0) {
            throw new IllegalStateException("Tu nivel " + nivel + " permite operaciones de hasta "
                    + limite.toPlainString() + " y esta es de " + valor.toPlainString());
        }
    }

    // ---------- Comportamiento financiero inusual ----------

    /**
     * Revisa una operación de salida (retiro, transferencia o pago) contra el historial reciente de su titular.
     * Reglas: ráfaga de salidas, valor muy alto frente al promedio, repetición hacia un mismo destino,
     * fragmentación con varias billeteras, cambio brusco de frecuencia y horario no habitual.
     * Si algo se dispara: marca la transacción con el riesgo, deja un evento en el historial especial,
     * lo registra en la auditoría y avisa al usuario.
     */
    private void evaluarRiesgo(Transaccion transaccion, Billetera origen) {
        String titular = origen.getIdentificacionTitular();
        Usuario usuario = usuarios.get(titular);
        LocalDateTime fecha = transaccion.getFecha();
        int salidasEn5 = 0, salidasEn10 = 0, salidasPrevias10 = 0, mismoDestino = 0;
        int previas = 0, previasNocturnas = 0;
        BigDecimal sumaPrevias = BigDecimal.ZERO;
        TablaHash<String, Boolean> billeterasHaciaDestino = new TablaHash<>();

        for (NodeSimple<Transaccion> nodo = usuario.historialMovimientos().getFirstNode(); nodo != null; nodo = nodo.getNext()) {
            Transaccion otra = nodo.getData();
            if (!esSalidaDe(otra, titular)) {
                continue;
            }
            if (otra != transaccion) {
                previas++;
                sumaPrevias = sumaPrevias.add(otra.getValor());
                if (esMadrugada(otra.getFecha().getHour())) {
                    previasNocturnas++;
                }
            }
            long segundos = Duration.between(otra.getFecha(), fecha).getSeconds();
            if (segundos < 0) {
                continue;
            }
            if (segundos <= 300) {
                salidasEn5++;
            }
            if (segundos <= 600) {
                salidasEn10++;
                if (otra.getCodigoDestino() != null && otra.getCodigoDestino().equals(transaccion.getCodigoDestino())) {
                    mismoDestino++;
                    billeterasHaciaDestino.put(otra.getCodigoOrigen(), Boolean.TRUE);
                }
            } else if (segundos <= 1200) {
                salidasPrevias10++;
            }
        }

        NivelRiesgo nivel = NivelRiesgo.BAJO;
        StringBuilder razones = new StringBuilder();
        if (salidasEn5 >= 4) {
            nivel = mayor(nivel, salidasEn5 >= 6 ? NivelRiesgo.ALTO : NivelRiesgo.MEDIO);
            razon(razones, salidasEn5 + " salidas de dinero en 5 minutos");
        }
        if (previas >= 3) {
            BigDecimal promedio = sumaPrevias.divide(BigDecimal.valueOf(previas), 2, java.math.RoundingMode.HALF_UP);
            if (transaccion.getValor().compareTo(promedio.multiply(BigDecimal.valueOf(3))) > 0) {
                boolean muyAlto = transaccion.getValor().compareTo(promedio.multiply(BigDecimal.valueOf(10))) > 0;
                nivel = mayor(nivel, muyAlto ? NivelRiesgo.ALTO : NivelRiesgo.MEDIO);
                razon(razones, "valor " + transaccion.getValor().toPlainString() + " muy por encima de su promedio ("
                        + promedio.toPlainString() + ")");
            }
        }
        if (transaccion.getCodigoDestino() != null && mismoDestino >= 3) {
            nivel = mayor(nivel, NivelRiesgo.MEDIO);
            razon(razones, mismoDestino + " envíos a " + transaccion.getCodigoDestino() + " en 10 minutos");
            if (billeterasHaciaDestino.size() >= 2) {
                nivel = mayor(nivel, NivelRiesgo.ALTO);
                razon(razones, "montos fragmentados desde " + billeterasHaciaDestino.size() + " billeteras propias");
            }
        }
        if (salidasEn10 >= 4 && salidasPrevias10 >= 1 && salidasEn10 >= 3 * salidasPrevias10) {
            nivel = mayor(nivel, NivelRiesgo.MEDIO);
            razon(razones, "la frecuencia pasó de " + salidasPrevias10 + " a " + salidasEn10 + " salidas en 10 minutos");
        }
        if (esMadrugada(fecha.getHour()) && previas >= 3 && previasNocturnas == 0) {
            nivel = mayor(nivel, NivelRiesgo.MEDIO);
            razon(razones, "actividad de madrugada (" + fecha.getHour() + " h), fuera de su horario habitual");
        }

        if (nivel != NivelRiesgo.BAJO) {
            transaccion.marcarRiesgo(nivel);
            persistirTransaccion(transaccion);
            String detalle = "Transacción " + transaccion.getId() + ": " + razones;
            LocalDateTime momento = ahora();
            EventoAuditoria evento = new EventoAuditoria(momento, titular, "COMPORTAMIENTO_INUSUAL", nivel, detalle);
            eventos.encolar(evento);
            eventosInusuales.addLast(evento);
            repositorio.insertarEvento(momento, titular, "COMPORTAMIENTO_INUSUAL", nivel, detalle);
            notificar(titular, TipoAlerta.COMPORTAMIENTO_INUSUAL,
                    "Alerta de seguridad (riesgo " + nivel + ") en " + transaccion.getId() + ": " + razones);
        }
    }

    private boolean esSalidaDe(Transaccion transaccion, String titular) {
        if (transaccion.getCodigoOrigen() == null || transaccion.getTipo() == TipoTransaccion.RECARGA) {
            return false;
        }
        Billetera origen = billeteras.get(transaccion.getCodigoOrigen());
        return origen != null && origen.getIdentificacionTitular().equals(titular);
    }

    private static boolean esMadrugada(int hora) {
        return hora >= 0 && hora < 5;
    }

    private static NivelRiesgo mayor(NivelRiesgo a, NivelRiesgo b) {
        return b.ordinal() > a.ordinal() ? b : a;
    }

    private static void razon(StringBuilder razones, String texto) {
        if (razones.length() > 0) {
            razones.append("; ");
        }
        razones.append(texto);
    }

    /** Consulta posterior del historial especial: el administrador ve todos, un usuario los suyos. */
    public EventoAuditoria[] listarEventosInusuales(String actor, NivelRiesgo minimo) {
        boolean admin = esAdmin(actor);
        NivelRiesgo piso = minimo == null ? NivelRiesgo.BAJO : minimo;
        Cola<EventoAuditoria> visibles = new Cola<>();
        for (NodeSimple<EventoAuditoria> nodo = eventosInusuales.getFirstNode(); nodo != null; nodo = nodo.getNext()) {
            EventoAuditoria evento = nodo.getData();
            if (evento.getNivelRiesgo().ordinal() >= piso.ordinal()
                    && (admin || actor != null && actor.equals(evento.getIdentificacionUsuario()))) {
                visibles.encolar(evento);
            }
        }
        EventoAuditoria[] resultado = new EventoAuditoria[visibles.getSize()];
        for (int i = 0; i < resultado.length; i++) {
            resultado[i] = visibles.desencolar();
        }
        return resultado;
    }

    // ---------- Analítica de movimientos (solo administrador) ----------

    public Conteo[] billeterasMasUsadas(String actor, int n) {
        exigirAdmin(actor, "Solo el administrador puede ver la analítica");
        Cola<String> claves = billeteras.claves();
        Conteo[] todos = new Conteo[claves.getSize()];
        for (int i = 0; i < todos.length; i++) {
            Billetera billetera = billeteras.get(claves.desencolar());
            todos[i] = new Conteo(billetera.getCodigo(), billetera.historialMovimientos().getSize());
        }
        return primeros(todos, n);
    }

    /** Transferencias y pagos programados completados, contados por el titular de la billetera origen. */
    public Conteo[] usuariosConMasTransferencias(String actor, int n) {
        exigirAdmin(actor, "Solo el administrador puede ver la analítica");
        TablaHash<String, Integer> cuentas = new TablaHash<>();
        for (NodeSimple<Transaccion> nodo = libro.getFirstNode(); nodo != null; nodo = nodo.getNext()) {
            Transaccion t = nodo.getData();
            boolean esTransferencia = t.getTipo() == TipoTransaccion.TRANSFERENCIA
                    || t.getTipo() == TipoTransaccion.PAGO_PROGRAMADO;
            if (esTransferencia && t.getEstado() == EstadoTransaccion.COMPLETADA) {
                String titular = buscarBilletera(t.getCodigoOrigen()).getIdentificacionTitular();
                cuentas.put(titular, (cuentas.containsKey(titular) ? cuentas.get(titular) : 0) + 1);
            }
        }
        return primeros(aConteos(cuentas), n);
    }

    /** Movimientos acumulados por categoría (tipo) de billetera: ahorros, corriente, digital. */
    public Conteo[] categoriasMasActivas(String actor) {
        exigirAdmin(actor, "Solo el administrador puede ver la analítica");
        TipoBilletera[] tipos = TipoBilletera.values();
        long[] movimientos = new long[tipos.length];
        Cola<String> claves = billeteras.claves();
        while (!claves.isEmpty()) {
            Billetera billetera = billeteras.get(claves.desencolar());
            movimientos[billetera.getTipo().ordinal()] += billetera.historialMovimientos().getSize();
        }
        Conteo[] todos = new Conteo[tipos.length];
        for (int i = 0; i < tipos.length; i++) {
            todos[i] = new Conteo(tipos[i].name(), movimientos[i]);
        }
        return primeros(todos, tipos.length);
    }

    /** Cuántas transacciones hay de cada tipo (recarga, retiro, transferencia, pago programado). */
    public Conteo[] frecuenciaPorTipo(String actor) {
        exigirAdmin(actor, "Solo el administrador puede ver la analítica");
        TipoTransaccion[] tipos = TipoTransaccion.values();
        long[] cuentas = new long[tipos.length];
        for (NodeSimple<Transaccion> nodo = libro.getFirstNode(); nodo != null; nodo = nodo.getNext()) {
            cuentas[nodo.getData().getTipo().ordinal()]++;
        }
        Conteo[] todos = new Conteo[tipos.length];
        for (int i = 0; i < tipos.length; i++) {
            todos[i] = new Conteo(tipos[i].name(), cuentas[i]);
        }
        return primeros(todos, tipos.length);
    }

    /** Suma de las transacciones completadas con fecha entre desde y hasta (ambas inclusive). */
    public MontoMovilizado montoMovilizado(String actor, LocalDateTime desde, LocalDateTime hasta) {
        exigirAdmin(actor, "Solo el administrador puede ver la analítica");
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new IllegalArgumentException("El rango de fechas no es válido");
        }
        int cantidad = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (NodeSimple<Transaccion> nodo = libro.getFirstNode(); nodo != null; nodo = nodo.getNext()) {
            Transaccion t = nodo.getData();
            if (t.getEstado() == EstadoTransaccion.COMPLETADA
                    && !t.getFecha().isBefore(desde) && !t.getFecha().isAfter(hasta)) {
                cantidad++;
                total = total.add(t.getValor());
            }
        }
        return new MontoMovilizado(desde, hasta, cantidad, total);
    }

    /** Pares de usuarios con más transferencias entre sí (aristas del grafo, de mayor a menor frecuencia). */
    public AristaRed[] paresFrecuentes(String actor, int n) {
        exigirAdmin(actor, "Solo el administrador puede ver la analítica");
        AristaRed[] aristas = redDeUsuarios(actor).aristas();
        for (int i = 1; i < aristas.length; i++) {
            AristaRed actual = aristas[i];
            int j = i - 1;
            while (j >= 0 && (aristas[j].cantidad() < actual.cantidad()
                    || aristas[j].cantidad() == actual.cantidad() && aristas[j].total().compareTo(actual.total()) < 0)) {
                aristas[j + 1] = aristas[j];
                j--;
            }
            aristas[j + 1] = actual;
        }
        int limite = Math.min(validarTop(n), aristas.length);
        AristaRed[] resultado = new AristaRed[limite];
        for (int i = 0; i < limite; i++) {
            resultado[i] = aristas[i];
        }
        return resultado;
    }

    private static int validarTop(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("La cantidad a mostrar debe ser al menos 1");
        }
        return n;
    }

    private static Conteo[] aConteos(TablaHash<String, Integer> cuentas) {
        Cola<String> claves = cuentas.claves();
        Conteo[] resultado = new Conteo[claves.getSize()];
        for (int i = 0; i < resultado.length; i++) {
            String clave = claves.desencolar();
            resultado[i] = new Conteo(clave, cuentas.get(clave));
        }
        return resultado;
    }

    /** Ordena de mayor a menor cantidad (empate: por clave) y devuelve los n primeros. */
    private static Conteo[] primeros(Conteo[] datos, int n) {
        int limite = Math.min(validarTop(n), datos.length);
        for (int i = 1; i < datos.length; i++) {
            Conteo actual = datos[i];
            int j = i - 1;
            while (j >= 0 && (datos[j].cantidad() < actual.cantidad()
                    || datos[j].cantidad() == actual.cantidad() && datos[j].clave().compareTo(actual.clave()) > 0)) {
                datos[j + 1] = datos[j];
                j--;
            }
            datos[j + 1] = actual;
        }
        Conteo[] resultado = new Conteo[limite];
        for (int i = 0; i < limite; i++) {
            resultado[i] = datos[i];
        }
        return resultado;
    }

    // ---------- Requisitos adicionales (solo administrador) ----------

    /**
     * Usuarios con más actividad entre dos fechas: cuenta las transacciones registradas en las que participan
     * (como titular de la billetera origen o destino, una vez por transacción; las revertidas también cuentan).
     * El primero del resultado es el más activo.
     */
    public Conteo[] usuariosMasActivos(String actor, LocalDateTime desde, LocalDateTime hasta, int n) {
        exigirAdmin(actor, "Solo el administrador puede ver la actividad de los usuarios");
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw new IllegalArgumentException("El rango de fechas no es válido");
        }
        TablaHash<String, Integer> cuentas = new TablaHash<>();
        for (NodeSimple<Transaccion> nodo = libro.getFirstNode(); nodo != null; nodo = nodo.getNext()) {
            Transaccion t = nodo.getData();
            if (t.getFecha().isBefore(desde) || t.getFecha().isAfter(hasta)) {
                continue;
            }
            String titularOrigen = t.getCodigoOrigen() == null ? null
                    : buscarBilletera(t.getCodigoOrigen()).getIdentificacionTitular();
            String titularDestino = t.getCodigoDestino() == null ? null
                    : buscarBilletera(t.getCodigoDestino()).getIdentificacionTitular();
            contar(cuentas, titularOrigen);
            if (titularDestino != null && !titularDestino.equals(titularOrigen)) {
                contar(cuentas, titularDestino);
            }
        }
        return primeros(aConteos(cuentas), n);
    }

    private static void contar(TablaHash<String, Integer> cuentas, String clave) {
        if (clave != null) {
            cuentas.put(clave, (cuentas.containsKey(clave) ? cuentas.get(clave) : 0) + 1);
        }
    }

    /** Ciclos de dinero (A -> B -> ... -> A) en la red de 'usuarios' o de 'billeteras'. */
    public RutaFrecuente[] ciclosDeTransferencias(String actor, String red) {
        exigirAdmin(actor, "Solo el administrador puede analizar la red de transferencias");
        Grafo<String> grafo = elegirRed(red);
        SimpleLinkedList<SimpleLinkedList<String>> ciclos = grafo.ciclos(MAX_CICLOS);
        RutaFrecuente[] resultado = new RutaFrecuente[ciclos.getSize()];
        int i = 0;
        for (NodeSimple<SimpleLinkedList<String>> nodo = ciclos.getFirstNode(); nodo != null; nodo = nodo.getNext()) {
            resultado[i++] = describirRecorrido(grafo, nodo.getData(), true);
        }
        return ordenarRutas(resultado);
    }

    /**
     * Rutas de 2 y 3 saltos por las que más veces ha circulado dinero, ordenadas por la arista menos usada
     * de la ruta (cuántas veces se ha podido completar) y luego por el dinero acumulado.
     */
    public RutaFrecuente[] rutasFrecuentes(String actor, String red, int n) {
        exigirAdmin(actor, "Solo el administrador puede analizar la red de transferencias");
        if (n < 1) {
            throw new IllegalArgumentException("La cantidad a mostrar debe ser al menos 1");
        }
        Grafo<String> grafo = elegirRed(red);
        SimpleLinkedList<SimpleLinkedList<String>> caminos = grafo.caminos(2, 3, MAX_RUTAS);
        RutaFrecuente[] todas = new RutaFrecuente[caminos.getSize()];
        int i = 0;
        for (NodeSimple<SimpleLinkedList<String>> nodo = caminos.getFirstNode(); nodo != null; nodo = nodo.getNext()) {
            todas[i++] = describirRecorrido(grafo, nodo.getData(), false);
        }
        ordenarRutas(todas);
        int limite = Math.min(n, todas.length);
        RutaFrecuente[] resultado = new RutaFrecuente[limite];
        for (int k = 0; k < limite; k++) {
            resultado[k] = todas[k];
        }
        return resultado;
    }

    private Grafo<String> elegirRed(String red) {
        if ("usuarios".equals(red)) {
            return redUsuarios;
        }
        if ("billeteras".equals(red)) {
            return redBilleteras;
        }
        throw new IllegalArgumentException("La red debe ser 'usuarios' o 'billeteras'");
    }

    private static RutaFrecuente describirRecorrido(Grafo<String> grafo, SimpleLinkedList<String> vertices, boolean cerrado) {
        String[] pasos = vertices.toArray(new String[0]);
        int frecuencia = Integer.MAX_VALUE;
        BigDecimal total = BigDecimal.ZERO;
        int aristas = cerrado ? pasos.length : pasos.length - 1;
        for (int i = 0; i < aristas; i++) {
            Arista<String> arista = grafo.getArista(pasos[i], pasos[(i + 1) % pasos.length]);
            frecuencia = Math.min(frecuencia, arista.getCantidad());
            total = total.add(arista.getTotal());
        }
        return new RutaFrecuente(pasos, frecuencia == Integer.MAX_VALUE ? 0 : frecuencia, total);
    }

    /** Orden: más frecuente, más dinero y, para que sea estable, por el texto del recorrido. */
    private static RutaFrecuente[] ordenarRutas(RutaFrecuente[] rutas) {
        for (int i = 1; i < rutas.length; i++) {
            RutaFrecuente actual = rutas[i];
            int j = i - 1;
            while (j >= 0 && antes(actual, rutas[j])) {
                rutas[j + 1] = rutas[j];
                j--;
            }
            rutas[j + 1] = actual;
        }
        return rutas;
    }

    private static boolean antes(RutaFrecuente a, RutaFrecuente b) {
        if (a.frecuencia() != b.frecuencia()) {
            return a.frecuencia() > b.frecuencia();
        }
        int porDinero = a.total().compareTo(b.total());
        if (porDinero != 0) {
            return porDinero > 0;
        }
        return String.join(">", a.vertices()).compareTo(String.join(">", b.vertices())) < 0;
    }

    /** Las n transacciones completadas de mayor valor, sacadas de un montículo (cola de prioridad). */
    public Transaccion[] transaccionesMayorValor(String actor, int n) {
        exigirAdmin(actor, "Solo el administrador puede ver las transacciones de mayor valor");
        if (n < 1) {
            throw new IllegalArgumentException("La cantidad a mostrar debe ser al menos 1");
        }
        MonticuloBinario<TransaccionPorValor> monticulo = new MonticuloBinario<>();
        for (NodeSimple<Transaccion> nodo = libro.getFirstNode(); nodo != null; nodo = nodo.getNext()) {
            if (nodo.getData().getEstado() == EstadoTransaccion.COMPLETADA) {
                monticulo.insertar(new TransaccionPorValor(nodo.getData()));
            }
        }
        Transaccion[] resultado = new Transaccion[Math.min(n, monticulo.size())];
        for (int i = 0; i < resultado.length; i++) {
            resultado[i] = monticulo.extraerMinimo().transaccion();
        }
        return resultado;
    }

    /** Compara estructuras propias y de Java en operaciones parecidas (ver ComparadorRendimiento). */
    public ResultadoRendimiento[] compararRendimiento(String actor, int elementos) {
        exigirAdmin(actor, "Solo el administrador puede ejecutar la comparación de rendimiento");
        return ComparadorRendimiento.comparar(elementos);
    }

    // ---------- Pila: deshacer operaciones ----------

    /**
     * Revierte la operación más reciente que aún se pueda deshacer (el tope de la pila del usuario).
     * Si no se puede revertir (por ejemplo, saldo insuficiente en el destino), queda en la pila y se informa el motivo.
     */
    public Transaccion deshacerUltima(String actor) {
        Stack<Transaccion> pila = buscarUsuario(actor).pilaReversibles();
        while (!pila.isEmpty() && pila.peek().getEstado() != EstadoTransaccion.COMPLETADA) {
            pila.pop();
        }
        if (pila.isEmpty()) {
            throw new IllegalStateException("No hay operaciones para deshacer");
        }
        return revertir(actor, pila.peek().getId());
    }

    /** Operaciones que aún se pueden deshacer, la más reciente primero. */
    public Transaccion[] listarReversibles(String actor) {
        return pilaAArreglo(buscarUsuario(actor).pilaReversibles());
    }

    /** Historial de deshacer: operaciones ya revertidas, la última revertida primero. */
    public Transaccion[] listarDeshechas(String actor) {
        return pilaAArreglo(buscarUsuario(actor).pilaDeshechas());
    }

    private Transaccion[] pilaAArreglo(Stack<Transaccion> pila) {
        Stack<Transaccion> copia = pila.clonStack();      // se recorre una copia para no vaciar la pila real
        SimpleLinkedList<Transaccion> lista = new SimpleLinkedList<>();
        while (!copia.isEmpty()) {
            lista.addLast(copia.pop());
        }
        return lista.toArray(new Transaccion[0]);
    }

    /** Cada operación entra a la pila de quien podría revertirla: el titular de la billetera origen o, en recargas, del destino. */
    private void apilarReversible(Transaccion transaccion) {
        String codigo = transaccion.getCodigoOrigen() != null ? transaccion.getCodigoOrigen() : transaccion.getCodigoDestino();
        usuarios.get(billeteras.get(codigo).getIdentificacionTitular()).pilaReversibles().push(transaccion);
    }

    // ---------- Árbol: ranking y reportes por puntos (solo administrador) ----------

    /** Usuarios de mayor a menor puntaje. */
    public Usuario[] rankingDePuntos(String actor) {
        exigirAdmin(actor, "Solo el administrador puede ver el ranking");
        return aUsuarios(ranking.inordenDescendente());
    }

    /** Usuarios con puntos entre minimo y maximo (incluidos), de menor a mayor. Búsqueda con poda en el árbol. */
    public Usuario[] usuariosPorRango(String actor, int minimo, int maximo) {
        exigirAdmin(actor, "Solo el administrador puede consultar usuarios por puntos");
        if (minimo < 0 || maximo < minimo) {
            throw new IllegalArgumentException("El rango de puntos no es válido");
        }
        return aUsuarios(ranking.enRango(ClavePuntos.limiteInferior(minimo), ClavePuntos.limiteSuperior(maximo)));
    }

    /** Cuántos usuarios hay en cada nivel, usando una búsqueda por rango por nivel. */
    public ResumenNivel[] resumenPorNiveles(String actor) {
        exigirAdmin(actor, "Solo el administrador puede ver el resumen por niveles");
        Nivel[] niveles = Nivel.values();
        ResumenNivel[] resumen = new ResumenNivel[niveles.length];
        for (int i = 0; i < niveles.length; i++) {
            int cantidad = ranking.enRango(ClavePuntos.limiteInferior(niveles[i].getMinimo()),
                    ClavePuntos.limiteSuperior(niveles[i].getMaximo())).getSize();
            Integer tope = niveles[i].getMaximo() == Integer.MAX_VALUE ? null : niveles[i].getMaximo();
            resumen[i] = new ResumenNivel(niveles[i], niveles[i].getMinimo(), tope, cantidad);
        }
        return resumen;
    }

    private Usuario[] aUsuarios(SimpleLinkedList<ClavePuntos> claves) {
        SimpleLinkedList<Usuario> lista = new SimpleLinkedList<>();
        for (NodeSimple<ClavePuntos> nodo = claves.getFirstNode(); nodo != null; nodo = nodo.getNext()) {
            lista.addLast(usuarios.get(nodo.getData().getIdentificacion()));
        }
        return lista.toArray(new Usuario[0]);
    }

    private static ClavePuntos clave(Usuario usuario) {
        return new ClavePuntos(usuario.getPuntos(), usuario.getIdentificacion());
    }

    /** Cambia los puntos de un usuario, lo recoloca en el árbol y guarda el cambio en la base. */
    private void cambiarPuntos(Usuario usuario, Runnable cambio) {
        if (usuario.esAdmin()) {
            cambio.run();
            persistirUsuario(usuario);
            return;
        }
        ranking.eliminar(clave(usuario));
        try {
            cambio.run();
        } finally {
            ranking.insertar(clave(usuario));       // vuelve a entrar con los puntos nuevos (o con los de antes si falló)
            persistirUsuario(usuario);
        }
    }

    // ---------- Grafo: red de transferencias (solo administrador) ----------

    public RedTransferencias redDeUsuarios(String actor) {
        exigirAdmin(actor, "Solo el administrador puede ver la red de transferencias");
        return aRed(redUsuarios);
    }

    public RedTransferencias redDeBilleteras(String actor) {
        exigirAdmin(actor, "Solo el administrador puede ver la red de transferencias");
        return aRed(redBilleteras);
    }

    /** Ruta con menos saltos por la que ha circulado dinero entre dos usuarios; arreglo vacío si no hay. */
    public String[] rutaDeDinero(String actor, String desde, String hasta) {
        exigirAdmin(actor, "Solo el administrador puede consultar rutas de dinero");
        if (!redUsuarios.existeVertice(desde) || !redUsuarios.existeVertice(hasta)) {
            throw new IllegalArgumentException("Los dos usuarios deben existir");
        }
        SimpleLinkedList<String> camino = redUsuarios.caminoMasCorto(desde, hasta);
        return camino == null ? new String[0] : camino.toArray(new String[0]);
    }

    private RedTransferencias aRed(Grafo<String> grafo) {
        SimpleLinkedList<String> vertices = new SimpleLinkedList<>();
        Cola<String> claves = grafo.vertices();
        while (!claves.isEmpty()) {
            vertices.addLast(claves.desencolar());
        }
        SimpleLinkedList<AristaRed> aristas = new SimpleLinkedList<>();
        for (NodeSimple<Arista<String>> nodo = grafo.aristas().getFirstNode(); nodo != null; nodo = nodo.getNext()) {
            Arista<String> a = nodo.getData();
            aristas.addLast(new AristaRed(a.getOrigen(), a.getDestino(), a.getCantidad(), a.getTotal()));
        }
        return new RedTransferencias(vertices.toArray(new String[0]), aristas.toArray(new AristaRed[0]));
    }

    /** Una transferencia suma a la arista billetera->billetera y, si cambia de dueño, a la arista usuario->usuario. */
    private void registrarEnLaRed(Billetera origen, Billetera destino, BigDecimal valor) {
        redBilleteras.agregarArista(origen.getCodigo(), destino.getCodigo(), valor);
        if (!origen.getIdentificacionTitular().equals(destino.getIdentificacionTitular())) {
            redUsuarios.agregarArista(origen.getIdentificacionTitular(), destino.getIdentificacionTitular(), valor);
        }
    }

    private void quitarDeLaRed(Billetera origen, Billetera destino, BigDecimal valor) {
        redBilleteras.restarArista(origen.getCodigo(), destino.getCodigo(), valor);
        if (!origen.getIdentificacionTitular().equals(destino.getIdentificacionTitular())) {
            redUsuarios.restarArista(origen.getIdentificacionTitular(), destino.getIdentificacionTitular(), valor);
        }
    }

    // ---------- Permisos ----------

    private boolean esAdmin(String actor) {
        Usuario usuario = actor == null ? null : usuarios.get(actor);
        return usuario != null && usuario.esAdmin();
    }

    private void exigirAdmin(String actor, String mensaje) {
        if (!esAdmin(actor)) {
            throw new AccesoDenegadoException(mensaje);
        }
    }

    /** true si la billetera existe y su titular es el actor. */
    private boolean esBilleteraDe(String actor, String codigoBilletera) {
        if (codigoBilletera == null) {
            return false;
        }
        Billetera billetera = billeteras.get(codigoBilletera);
        return billetera != null && billetera.getIdentificacionTitular().equals(actor);
    }

    /** Permite al administrador y al dueño de la billetera; a nadie más. */
    private void exigirBilleteraPropia(String actor, String codigoBilletera) {
        Billetera billetera = buscarBilletera(codigoBilletera);
        exigirPropietario(actor, billetera.getIdentificacionTitular(), "Esa billetera no es tuya");
    }

    private void exigirPropietario(String actor, String titular, String mensaje) {
        if (!esAdmin(actor) && !titular.equals(actor)) {
            throw new AccesoDenegadoException(mensaje);
        }
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
        exigirLimiteDelNivel(origen, valor);
        origen.retirar(valor);
        destino.recargar(valor);
        persistirBilletera(origen);
        persistirBilletera(destino);
        Transaccion transaccion = crearTransaccion(tipo, valor, codigoOrigen, codigoDestino);
        registrarEnLaRed(origen, destino, valor);
        premiar(origen.getIdentificacionTitular(), transaccion, puntosPor(valor, factorPuntos));
        evaluarRiesgo(transaccion, origen);
        registrarEvento(origen.getIdentificacionTitular(), tipo.name(), NivelRiesgo.BAJO,
                "Monto " + valor + " hacia " + codigoDestino);
        verificarSaldoBajo(origen);
        return transaccion;
    }

    private Transaccion crearTransaccion(TipoTransaccion tipo, BigDecimal valor,
                                         String codigoOrigen, String codigoDestino) {
        String id = "T" + (++secuencia);
        Transaccion transaccion = new Transaccion(id, ahora(), tipo, valor,
                codigoOrigen, codigoDestino);
        transacciones.put(id, transaccion);
        libro.addLast(transaccion);
        registrarEnHistoriales(transaccion, codigoOrigen, codigoDestino);
        apilarReversible(transaccion);
        repositorio.insertarTransaccion(id, transaccion.getFecha(), tipo, valor, codigoOrigen, codigoDestino);
        return transaccion;
    }

    /** Anota el movimiento en la billetera origen y destino y en el historial de cada usuario involucrado (una vez). */
    private void registrarEnHistoriales(Transaccion transaccion, String codigoOrigen, String codigoDestino) {
        Billetera origen = codigoOrigen == null ? null : billeteras.get(codigoOrigen);
        Billetera destino = codigoDestino == null ? null : billeteras.get(codigoDestino);
        if (origen != null) {
            origen.registrarMovimiento(transaccion);
            usuarios.get(origen.getIdentificacionTitular()).registrarMovimiento(transaccion);
        }
        if (destino != null) {
            destino.registrarMovimiento(transaccion);
            if (origen == null || !origen.getIdentificacionTitular().equals(destino.getIdentificacionTitular())) {
                usuarios.get(destino.getIdentificacionTitular()).registrarMovimiento(transaccion);
            }
        }
    }

    private void premiar(String identificacion, Transaccion transaccion, int puntos) {
        transaccion.setPuntosGenerados(puntos);
        persistirTransaccion(transaccion);
        if (puntos > 0) {
            sumarPuntos(identificacion, puntos);
        }
    }

    private void sumarPuntos(String identificacion, int puntos) {
        Usuario usuario = buscarUsuario(identificacion);
        Nivel anterior = usuario.getNivel();
        cambiarPuntos(usuario, () -> usuario.sumarPuntos(puntos));
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

    /** Guarda el evento en memoria y en la base de datos (auditoría). */
    private void registrarEvento(String identificacion, String evento, NivelRiesgo riesgo, String detalle) {
        LocalDateTime momento = ahora();
        eventos.encolar(new EventoAuditoria(momento, identificacion, evento, riesgo, detalle));
        repositorio.insertarEvento(momento, identificacion, evento, riesgo, detalle);
    }

    /**
     * Encola la alerta; si el usuario supera el tope de recientes, se descarta su notificación más antigua.
     * Las notificaciones no se guardan en la base de datos: se pierden al reiniciar.
     */
    private void notificar(String identificacion, TipoAlerta tipo, String mensaje) {
        notificaciones.encolar(new Notificacion(identificacion, tipo, mensaje, ahora()));
        int total = (conteoNotificaciones.containsKey(identificacion) ? conteoNotificaciones.get(identificacion) : 0) + 1;
        if (total > TOPE_NOTIFICACIONES) {
            descartarMasAntigua(identificacion);
            total = TOPE_NOTIFICACIONES;
        }
        conteoNotificaciones.put(identificacion, total);
    }

    private void descartarMasAntigua(String identificacion) {
        int cantidad = notificaciones.getSize();
        boolean descartada = false;
        for (int i = 0; i < cantidad; i++) {
            Notificacion actual = notificaciones.desencolar();
            if (!descartada && actual.getIdentificacionUsuario().equals(identificacion)) {
                descartada = true;
            } else {
                notificaciones.encolar(actual);
            }
        }
    }

    private static int puntosPor(BigDecimal valor, int factor) {
        return valor.divideToIntegralValue(MONTO_PARA_PUNTOS).intValue() * factor;
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}