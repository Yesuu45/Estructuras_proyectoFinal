// Módulo de transacciones: registra operaciones, lista billeteras en desplegables y permite revertir.
const RUTA_API = '/api';

const formulario = document.getElementById('form-transaccion');
const selectOrigen = document.getElementById('select-origen');
const selectDestino = document.getElementById('select-destino');
const tablaCuerpo = document.querySelector('#tabla-transacciones tbody');

function escapar(texto) {
    return String(texto ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function mostrar(texto, esError) {
    const aviso = document.getElementById('mensajes');
    aviso.textContent = texto;
    aviso.className = 'mensaje ' + (esError ? 'error' : 'ok');
    aviso.hidden = false;
}

async function llamar(metodo, ruta, datos) {
    const opciones = { method: metodo, headers: { 'Content-Type': 'application/json' } };
    if (datos !== undefined) {
        opciones.body = JSON.stringify(datos);
    }
    const respuesta = await fetch(RUTA_API + ruta, opciones);
    const cuerpo = await respuesta.json().catch(() => ({}));
    if (!respuesta.ok) {
        throw new Error(cuerpo.mensaje || 'Error en la operación');
    }
    return cuerpo;
}

async function cargarBilleteras() {
    const billeteras = await llamar('GET', '/billeteras');
    const opciones = billeteras.map(b =>
        `<option value="${escapar(b.codigo)}">${escapar(b.codigo)} · ${escapar(b.nombre)} (titular ${escapar(b.identificacionTitular)})</option>`
    ).join('');
    const vacia = '<option value="">— ninguna —</option>';
    selectOrigen.innerHTML = vacia + opciones;
    selectDestino.innerHTML = vacia + opciones;
}

async function cargarTransacciones() {
    const transacciones = await llamar('GET', '/transacciones');
    tablaCuerpo.innerHTML = transacciones.map(t => `
        <tr>
            <td>${escapar(t.id)}</td>
            <td>${escapar(t.fecha)}</td>
            <td>${escapar(t.tipo)}</td>
            <td>${escapar(t.valor)}</td>
            <td>${escapar(t.codigoOrigen ?? '—')}</td>
            <td>${escapar(t.codigoDestino ?? '—')}</td>
            <td>${escapar(t.estado)}</td>
            <td>${escapar(t.puntosGenerados)}</td>
            <td>${t.estado === 'COMPLETADA'
                ? `<button onclick="revertir('${escapar(t.id)}')">Revertir</button>`
                : ''}</td>
        </tr>`).join('');
}

async function recargarTodo() {
    await Promise.all([cargarBilleteras(), cargarTransacciones()]);
}

formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const datos = Object.fromEntries(new FormData(formulario));
    try {
        await llamar('POST', '/transacciones', {
            tipo: datos.tipo,
            codigoOrigen: datos.codigoOrigen || null,
            codigoDestino: datos.codigoDestino || null,
            valor: Number(datos.valor)
        });
        mostrar('Transacción registrada', false);
        formulario.reset();
        await cargarTransacciones();
    } catch (error) {
        mostrar(error.message, true);
    }
});

async function revertir(id) {
    try {
        await llamar('POST', `/transacciones/${encodeURIComponent(id)}/revertir`);
        mostrar('Transacción revertida', false);
        await cargarTransacciones();
    } catch (error) {
        mostrar(error.message, true);
    }
}

recargarTodo().catch(error => mostrar(error.message, true));