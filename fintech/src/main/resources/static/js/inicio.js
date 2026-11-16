async function cargarResumen() {
    const yo = await sesionActual;
    const esAdmin = yo && yo.rol === 'ADMIN';

    const [billeteras, programadas] = await Promise.all([
        peticion('GET', '/billeteras'),
        peticion('GET', '/programadas')
    ]);
    document.getElementById('total-billeteras').textContent = billeteras.length;
    document.getElementById('total-pendientes').textContent =
        programadas.filter(p => p.estado === 'PENDIENTE').length;

    const tarjetaUsuarios = document.getElementById('total-usuarios').parentElement;
    if (esAdmin) {
        const usuarios = await peticion('GET', '/usuarios');
        document.getElementById('total-usuarios').textContent = usuarios.length;
    } else {
        tarjetaUsuarios.remove();
    }
}

cargarResumen().catch(err => mostrarMensaje(err.message, 'error'));
