// Módulo de transacciones.
// Recarga: solo cuenta de destino. Retiro: solo cuenta de origen. Transferencia: origen y destino.
const RUTA_API = '/api';

const formulario = document.getElementById('form-transaccion');
const selectTipo = document.getElementById('select-tipo');
const campoOrigen = document.getElementById('campo-origen');
const campoDestino = document.getElementById('campo-destino');
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

// Muestra solo los campos que el tipo de operación necesita.
// Los campos ocultos quedan deshabilitados, así no se envían.
function ajustarCampos() {
    const tipo = selectTipo.value;
    const usaOrigen = tipo !== 'RECARGA';
    const usaDestino = tipo !== 'RETIRO';

    campoOrigen.hidden = !usaOrigen;
    campoDestino.hidden = !usaDestino;
    selectOrigen.disabled = !usaOrigen;
    selectDestino.disabled = !usaDestino;
    selectOrigen.required = usaOrigen;
    selectDestino.required = usaDestino;
}

async function cargarBilleteras() {
    const billeteras = await llamar('GET', '/billeteras');
    const opciones = billeteras.map(b =>
        `<option value="${escapar(b.codigo)}">${escapar(b.codigo)} · ${escapar(b.nombre)} (titular ${escapar(b.identificacionTitular)})</option>`
    ).join('');
    selectOrigen.innerHTML = '<option value="">— elige una cuenta —</option>' + opciones;
    // El destino se escribe por código (puede ser de otra persona); estas son solo sugerencias
    document.getElementById('mis-billeteras').innerHTML = billeteras.map(b =>
        `<option value="${escapar(b.codigo)}">${escapar(b.nombre)}</option>`
    ).join('');
}

async function cargarTransacciones() {
    const desde = document.getElementById('filtro-desde').value;
    const hasta = document.getElementById('filtro-hasta').value;
    const parametros = [];
    if (desde) parametros.push('desde=' + encodeURIComponent(desde));
    if (hasta) parametros.push('hasta=' + encodeURIComponent(hasta.length === 16 ? hasta + ':59' : hasta));
    const transacciones = await llamar('GET', '/transacciones' + (parametros.length ? '?' + parametros.join('&') : ''));
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

// Las dos pilas (la más reciente arriba): lo que aún se puede deshacer y lo ya deshecho
async function cargarPilas() {
    const [reversibles, deshechas] = await Promise.all([
        llamar('GET', '/transacciones/reversibles'),
        llamar('GET', '/transacciones/deshechas')
    ]);
    pintarPila('#tabla-reversibles tbody', reversibles, 'No hay operaciones para deshacer');
    pintarPila('#tabla-deshechas tbody', deshechas, 'Todavía no has deshecho nada');
}

function pintarPila(selector, pila, textoVacio) {
    document.querySelector(selector).innerHTML = pila.length === 0
        ? `<tr><td colspan="6">${textoVacio}</td></tr>`
        : pila.map((t, i) => `
            <tr>
                <td>${i === 0 ? 'Tope ▸' : ''}</td>
                <td>${escapar(t.id)}</td>
                <td>${escapar(t.tipo)}</td>
                <td>${escapar(t.valor)}</td>
                <td>${escapar(t.codigoOrigen ?? '—')}</td>
                <td>${escapar(t.codigoDestino ?? '—')}</td>
            </tr>`).join('');
}

async function refrescar() {
    await Promise.all([cargarBilleteras(), cargarTransacciones(), cargarPilas()]);
}

async function recargarTodo() {
    await refrescar();
}

selectTipo.addEventListener('change', ajustarCampos);

document.getElementById('form-periodo').addEventListener('submit', async (evento) => {
    evento.preventDefault();
    try {
        await cargarTransacciones();
    } catch (error) {
        mostrar(error.message, true);
    }
});

document.getElementById('quitar-filtro').addEventListener('click', async () => {
    document.getElementById('form-periodo').reset();
    try {
        await cargarTransacciones();
    } catch (error) {
        mostrar(error.message, true);
    }
});

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
        ajustarCampos();
        await refrescar();
    } catch (error) {
        mostrar(error.message, true);
    }
});

async function revertir(id) {
    try {
        await llamar('POST', `/transacciones/${encodeURIComponent(id)}/revertir`);
        mostrar('Transacción revertida', false);
        await refrescar();
    } catch (error) {
        mostrar(error.message, true);
    }
}

document.getElementById('btn-deshacer').addEventListener('click', async () => {
    try {
        const deshecha = await llamar('POST', '/transacciones/deshacer');
        mostrar('Se deshizo la operación ' + deshecha.id, false);
        await refrescar();
    } catch (error) {
        mostrar(error.message, true);
    }
});

ajustarCampos();
recargarTodo().catch(error => mostrar(error.message, true));