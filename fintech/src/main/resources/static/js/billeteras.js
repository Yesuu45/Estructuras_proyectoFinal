async function cargarBilleteras() {
    const billeteras = await peticion('GET', '/billeteras');
    pintar('tabla-billeteras', billeteras.map(b =>
        `<tr><td>${escapar(b.codigo)}</td><td>${escapar(b.nombre)}</td><td>${escapar(b.tipo)}</td>
         <td>${escapar(b.identificacionTitular)}</td><td>${escapar(b.saldo)}</td>
         <td>${b.activa ? 'Activa' : 'Bloqueada'}</td></tr>`));
}

conectarFormulario('form-billetera', '/billeteras', datos => ({
    ...datos,
    saldoInicial: Number(datos.saldoInicial || 0),
    umbralSaldoBajo: Number(datos.umbralSaldoBajo || 0)
}), 'Billetera creada', cargarBilleteras);

cargarBilleteras().catch(err => mostrarMensaje(err.message, 'error'));