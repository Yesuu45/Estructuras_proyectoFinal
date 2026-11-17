const NOMBRES_PERIODICIDAD = { UNICA: 'Una vez', DIARIA: 'Diaria', SEMANAL: 'Semanal', MENSUAL: 'Mensual' };

function formatearFecha(texto) {
    return texto ? String(texto).replace('T', ' ').substring(0, 16) : '';
}

async function cargarProgramadas() {
    const programadas = await peticion('GET', '/programadas');
    pintar('tabla-programadas', programadas.map(p => {
        const repeticion = p.periodicidad === 'UNICA'
            ? NOMBRES_PERIODICIDAD.UNICA
            : `${NOMBRES_PERIODICIDAD[p.periodicidad]} (faltan ${p.repeticionesRestantes} después de esta)`;
        return `<tr><td>${escapar(p.id)}</td><td>${escapar(formatearFecha(p.fechaEjecucion))}</td><td>${escapar(p.tipo)}</td>
         <td>${escapar(p.valor)}</td><td>${escapar(p.codigoOrigen ?? '—')}</td>
         <td>${escapar(p.codigoDestino)}</td><td>${escapar(repeticion)}</td><td>${escapar(p.estado)}</td></tr>`;
    }));
}

async function cargarReloj() {
    const reloj = await peticion('GET', '/reloj');
    document.getElementById('reloj-hora').textContent = formatearFecha(reloj.ahora);
    document.getElementById('reloj-tipo').textContent = reloj.simulado ? '(hora simulada)' : '(hora real)';
}

// Recarga automática: no lleva billetera origen. Sin repetición: una sola ocurrencia.
const selectTipo = document.getElementById('select-tipo-programada');
const campoOrigen = document.getElementById('campo-origen-programada');
const selectPeriodicidad = document.getElementById('select-periodicidad');
const campoOcurrencias = document.getElementById('campo-ocurrencias');

function ajustarFormulario() {
    const esRecarga = selectTipo.value === 'RECARGA';
    campoOrigen.disabled = esRecarga;
    campoOrigen.required = !esRecarga;
    campoOrigen.hidden = esRecarga;
    const repite = selectPeriodicidad.value !== 'UNICA';
    campoOcurrencias.disabled = !repite;
    campoOcurrencias.value = repite ? Math.max(2, Number(campoOcurrencias.value) || 2) : 1;
}
selectTipo.addEventListener('change', ajustarFormulario);
selectPeriodicidad.addEventListener('change', ajustarFormulario);

conectarFormulario('form-programada', '/programadas', datos => ({
    ...datos,
    codigoOrigen: datos.tipo === 'RECARGA' ? null : datos.codigoOrigen,
    ocurrencias: datos.periodicidad === 'UNICA' ? 1 : Number(datos.ocurrencias)
}), 'Operación programada', async () => {
    ajustarFormulario();
    await cargarProgramadas();
});

async function avanzar(minutos) {
    try {
        const ejecutar = document.getElementById('reloj-ejecutar').checked;
        const r = await peticion('POST', '/reloj/avanzar', { minutos, ejecutar });
        mostrarMensaje(`Reloj adelantado. Ejecutadas: ${r.ejecutadas}. Avisos de próximas: ${r.avisadas}.`);
        await Promise.all([cargarReloj(), cargarProgramadas()]);
    } catch (err) {
        mostrarMensaje(err.message, 'error');
    }
}

async function prepararAdministrador() {
    const yo = await sesionActual;
    if (!yo || yo.rol !== 'ADMIN') {
        return;
    }
    document.getElementById('controles-reloj').hidden = false;
    document.getElementById('btn-ejecutar').hidden = false;
    document.querySelectorAll('[data-minutos]').forEach(boton =>
        boton.addEventListener('click', () => avanzar(Number(boton.dataset.minutos))));
    document.getElementById('reloj-restablecer').addEventListener('click', async () => {
        try {
            await peticion('POST', '/reloj/restablecer');
            mostrarMensaje('Reloj en hora real');
            await cargarReloj();
        } catch (err) {
            mostrarMensaje(err.message, 'error');
        }
    });
    document.getElementById('btn-ejecutar').addEventListener('click', async () => {
        try {
            const r = await peticion('POST', '/programadas/ejecutar');
            mostrarMensaje(`Pendientes vencidas ejecutadas: ${r.ejecutadas}`);
            await cargarProgramadas();
        } catch (err) {
            mostrarMensaje(err.message, 'error');
        }
    });
}

ajustarFormulario();
prepararAdministrador().catch(() => {});
Promise.all([cargarReloj(), cargarProgramadas()]).catch(err => mostrarMensaje(err.message, 'error'));
