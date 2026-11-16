async function cargarProgramadas() {
    const programadas = await peticion('GET', '/programadas');
    pintar('tabla-programadas', programadas.map(p =>
        `<tr><td>${escapar(p.id)}</td><td>${escapar(p.fechaEjecucion)}</td><td>${escapar(p.tipo)}</td>
         <td>${escapar(p.valor)}</td><td>${escapar(p.codigoOrigen)}</td>
         <td>${escapar(p.codigoDestino)}</td><td>${escapar(p.estado)}</td></tr>`));
}

conectarFormulario('form-programada', '/programadas', null, 'Operación programada', cargarProgramadas);

document.getElementById('btn-ejecutar').addEventListener('click', async () => {
    try {
        await peticion('POST', '/programadas/ejecutar');
        mostrarMensaje('Operaciones pendientes ejecutadas');
        await cargarProgramadas();
    } catch (err) {
        mostrarMensaje(err.message, 'error');
    }
});

cargarProgramadas().catch(err => mostrarMensaje(err.message, 'error'));