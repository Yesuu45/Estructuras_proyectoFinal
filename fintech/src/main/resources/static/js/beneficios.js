async function cargarBeneficios() {
    const beneficios = await peticion('GET', '/beneficios');
    pintar('tabla-beneficios', beneficios.map(b =>
        `<tr><td>${escapar(b.identificacionUsuario)}</td><td>${escapar(b.descripcion)}</td>
         <td>${b.puntosCosto}</td><td>${escapar(b.fechaCanje)}</td></tr>`));
}

conectarFormulario('form-beneficio', '/beneficios', datos => ({
    ...datos,
    puntos: Number(datos.puntos)
}), 'Beneficio canjeado', cargarBeneficios);

cargarBeneficios().catch(err => mostrarMensaje(err.message, 'error'));