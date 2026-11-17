package co.edu.uniquindio.poo.servicio;

import co.edu.uniquindio.poo.Estructuras.*;
import co.edu.uniquindio.poo.modelo.*;
import co.edu.uniquindio.poo.seguridad.AccesoDenegadoException;
import co.edu.uniquindio.poo.seguridad.ClaveSegura;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private int secuencia = 0;

    // ---------- Usuarios ----------

    public Usuario registrarUsuario(String identificacion, String nombre, String correo, String clave) {
        if (usuarios.containsKey(identificacion)) {
            throw new IllegalStateException("Ya existe un usuario con identificación " + identificacion);
        }
        Usuario usuario = new Usuario(identificacion, nombre, correo, ClaveSegura.generarHash(clave));
        usuarios.put(usuario.getIdentificacion(), usuario);
        ranking.insertar(clave(usuario));
        redUsuarios.agregarVertice(usuario.getIdentificacion());
        return usuario;
    }

    /** Crea la cuenta de administrador si no existe. La llama InicializadorAdmin al arrancar. */
    public void sembrarAdministrador(String identificacion, String nombre, String clave) {
        if (usuarios.containsKey(identificacion)) {
            return;
        }
        usuarios.put(identificacion,
                new Usuario(identificacion, nombre, "", ClaveSegura.generarHash(clave), Rol.ADMIN));
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
        }
        if (origen != null) {
            origen.recargar(transaccion.getValor());
        }
        cambiarPuntos(usuarioTitular, () -> usuarioTitular.restarPuntos(transaccion.getPuntosGenerados()));
        transaccion.marcarRevertida();
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
        if (tipo != TipoTransaccion.TRANSFERENCIA && tipo != TipoTransaccion.PAGO_PROGRAMADO) {
            throw new IllegalArgumentException("Solo se pueden programar transferencias o pagos");
        }
        String origen = normalizar(codigoOrigen);
        String destino = normalizar(codigoDestino);
        exigirBilleteraPropia(actor, origen);
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

    /** Un usuario normal ve solo las operaciones programadas desde sus billeteras. */
    public OperacionProgramada[] listarProgramadas(String actor) {
        boolean admin = esAdmin(actor);
        Cola<String> claves = programadas.claves();
        Cola<OperacionProgramada> visibles = new Cola<>();
        while (!claves.isEmpty()) {
            OperacionProgramada operacion = programadas.get(claves.desencolar());
            if (admin || esBilleteraDe(actor, operacion.getCodigoOrigen())) {
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
                oferta.getPuntosCosto(), LocalDateTime.now());
        beneficios.addLast(beneficio);
        usuario.registrarCanje(beneficio);
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

    /** Cambia los puntos de un usuario y lo recoloca en el árbol (su clave depende de los puntos). */
    private void cambiarPuntos(Usuario usuario, Runnable cambio) {
        if (usuario.esAdmin()) {
            cambio.run();
            return;
        }
        ranking.eliminar(clave(usuario));
        try {
            cambio.run();
        } finally {
            ranking.insertar(clave(usuario));       // vuelve a entrar con los puntos nuevos (o con los de antes si falló)
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
        origen.retirar(valor);
        destino.recargar(valor);
        Transaccion transaccion = crearTransaccion(tipo, valor, codigoOrigen, codigoDestino);
        registrarEnLaRed(origen, destino, valor);
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
        libro.addLast(transaccion);
        registrarEnHistoriales(transaccion, codigoOrigen, codigoDestino);
        apilarReversible(transaccion);
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