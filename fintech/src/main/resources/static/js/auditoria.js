async function cargarAuditoria() {
    const eventos = await peticion('GET', '/auditoria/eventos');
    pintar('tabla-eventos', eventos.map(e =>
        `<tr><td>${escapar(e.fecha)}</td><td>${escapar(e.identificacionUsuario)}</td>
         <td>${escapar(e.evento)}</td><td>${escapar(e.nivelRiesgo)}</td>
         <td>${escapar(e.detalle)}</td></tr>`));

    const notificaciones = await peticion('GET', '/auditoria/notificaciones');
    pintar('tabla-notificaciones', notificaciones.map(n =>
        `<tr><td>${escapar(n.fecha)}</td><td>${escapar(n.identificacionUsuario)}</td>
         <td>${escapar(n.tipo)}</td><td>${escapar(n.mensaje)}</td>
         <td>${n.despachada ? 'Despachada' : 'Pendiente'}</td></tr>`));
}

cargarAuditoria().catch(err => mostrarMensaje(err.message, 'error'));