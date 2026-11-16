async function cargarResumen() {
    const [usuarios, billeteras, programadas] = await Promise.all([
        peticion('GET', '/usuarios'),
        peticion('GET', '/billeteras'),
        peticion('GET', '/programadas')
    ]);
    document.getElementById('total-usuarios').textContent = usuarios.length;
    document.getElementById('total-billeteras').textContent = billeteras.length;
    document.getElementById('total-pendientes').textContent =
        programadas.filter(p => p.estado === 'PENDIENTE').length;
}

cargarResumen().catch(err => mostrarMensaje(err.message, 'error'));