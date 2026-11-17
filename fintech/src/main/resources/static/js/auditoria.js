function formatearFecha(texto) {
    return texto ? String(texto).replace('T', ' ').substring(0, 19) : '';
}

async function cargarInusuales() {
    const filtro = document.getElementById('filtro-riesgo').value;
    const eventos = await peticion('GET', '/auditoria/inusuales' + (filtro ? '?minimo=' + filtro : ''));
    pintar('tabla-inusuales', eventos.length
        ? eventos.map(e =>
            `<tr><td>${escapar(formatearFecha(e.fecha))}</td><td>${escapar(e.identificacionUsuario)}</td>
             <td><span class="riesgo riesgo-${escapar(e.nivelRiesgo)}">${escapar(e.nivelRiesgo)}</span></td>
             <td>${escapar(e.detalle)}</td></tr>`)
        : ['<tr><td colspan="4">Sin eventos inusuales registrados</td></tr>']);
}

async function cargarNotificaciones() {
    const notificaciones = await peticion('GET', '/auditoria/notificaciones');
    pintar('tabla-notificaciones', notificaciones.map(n =>
        `<tr><td>${escapar(formatearFecha(n.fecha))}</td><td>${escapar(n.identificacionUsuario)}</td>
         <td>${escapar(n.tipo)}</td><td>${escapar(n.mensaje)}</td>
         <td>${n.despachada ? 'Despachada' : 'Pendiente'}</td></tr>`));
}

async function cargarAuditoria() {
    const yo = await sesionActual;
    if (yo && yo.rol === 'ADMIN') {
        const eventos = await peticion('GET', '/auditoria/eventos');
        pintar('tabla-eventos', eventos.map(e =>
            `<tr><td>${escapar(formatearFecha(e.fecha))}</td><td>${escapar(e.identificacionUsuario)}</td>
             <td>${escapar(e.evento)}</td><td>${escapar(e.nivelRiesgo)}</td>
             <td>${escapar(e.detalle)}</td></tr>`));
    } else {
        document.getElementById('bloque-eventos').remove();
    }
    await Promise.all([cargarInusuales(), cargarNotificaciones()]);
}

document.getElementById('filtro-riesgo').addEventListener('change', () =>
    cargarInusuales().catch(err => mostrarMensaje(err.message, 'error')));

document.getElementById('btn-despachar').addEventListener('click', async () => {
    try {
        const r = await peticion('POST', '/auditoria/notificaciones/despachar');
        mostrarMensaje(`Notificaciones despachadas: ${r.despachadas}`);
        await cargarNotificaciones();
    } catch (err) {
        mostrarMensaje(err.message, 'error');
    }
});

cargarAuditoria().catch(err => mostrarMensaje(err.message, 'error'));
