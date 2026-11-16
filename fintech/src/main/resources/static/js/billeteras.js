// Módulo de billeteras. El código lo genera la aplicación; el titular es quien inició sesión.
const RUTA_API = '/api';

const formulario = document.getElementById('form-billetera');
const tablaCuerpo = document.querySelector('#tabla-billeteras tbody');

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
    tablaCuerpo.innerHTML = billeteras.map(b => `
        <tr>
            <td>${escapar(b.codigo)}</td>
            <td>${escapar(b.nombre)}</td>
            <td>${escapar(b.tipo)}</td>
            <td>${escapar(b.identificacionTitular)}</td>
            <td>${escapar(b.saldo)}</td>
            <td>${b.activa ? 'Activa' : 'Bloqueada'}</td>
        </tr>`).join('');
}

formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const datos = Object.fromEntries(new FormData(formulario));
    try {
        await llamar('POST', '/billeteras', {
            nombre: datos.nombre,
            tipo: datos.tipo,
            saldoInicial: Number(datos.saldoInicial),
            umbralSaldoBajo: Number(datos.umbralSaldoBajo)
        });
        mostrar('Billetera creada', false);
        formulario.reset();
        formulario.elements['saldoInicial'].value = 0;
        formulario.elements['umbralSaldoBajo'].value = 0;
        await cargarBilleteras();
    } catch (error) {
        mostrar(error.message, true);
    }
});

cargarBilleteras()
    .catch(error => mostrar(error.message, true));