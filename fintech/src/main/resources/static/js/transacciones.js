async function cargarTransacciones() {
    const transacciones = await peticion('GET', '/transacciones');
    pintar('tabla-transacciones', transacciones.map(t =>
        `<tr><td>${escapar(t.id)}</td><td>${escapar(t.fecha)}</td><td>${escapar(t.tipo)}</td>
         <td>${escapar(t.valor)}</td><td>${escapar(t.codigoOrigen)}</td><td>${escapar(t.codigoDestino)}</td>
         <td>${escapar(t.estado)}</td>
         <td>${t.estado === 'COMPLETADA'
            ? `<button onclick="revertir('${escapar(t.id)}')">Revertir</button>`
            : ''}</td></tr>`));
}

async function revertir(id) {
    try {
        await peticion('POST', '/transacciones/' + encodeURIComponent(id) + '/revertir');
        mostrarMensaje('Transacción revertida');
        await cargarTransacciones();
    } catch (err) {
        mostrarMensaje(err.message, 'error');
    }
}

conectarFormulario('form-transaccion', '/transacciones', null, 'Transacción registrada', cargarTransacciones);
cargarTransacciones().catch(err => mostrarMensaje(err.message, 'error'));