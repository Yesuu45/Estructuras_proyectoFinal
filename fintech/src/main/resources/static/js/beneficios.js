// Módulo de beneficios: el usuario elige de una lista; el servidor valida los puntos.
const RUTA_API = '/api';

const formulario = document.getElementById('form-beneficio');
const selectUsuario = document.getElementById('select-usuario');
const selectBeneficio = document.getElementById('select-beneficio');
const tablaCatalogo = document.querySelector('#tabla-catalogo tbody');
const tablaCanjes = document.querySelector('#tabla-beneficios tbody');

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

async function cargarUsuarios() {
    const usuarios = await llamar('GET', '/usuarios');
    selectUsuario.innerHTML = '<option value="">— elige un usuario —</option>' + usuarios.map(u =>
        `<option value="${escapar(u.identificacion)}">${escapar(u.nombre)} (${escapar(u.identificacion)}) · ${escapar(u.puntos)} pts</option>`
    ).join('');
}

async function cargarCatalogo() {
    const catalogo = await llamar('GET', '/beneficios/catalogo');
    selectBeneficio.innerHTML = '<option value="">— elige un beneficio —</option>' + catalogo.map(b =>
        `<option value="${escapar(b.id)}">${escapar(b.nombre)} · ${escapar(b.puntosCosto)} pts</option>`
    ).join('');
    tablaCatalogo.innerHTML = catalogo.map(b => `
        <tr>
            <td>${escapar(b.id)}</td>
            <td>${escapar(b.nombre)}</td>
            <td>${escapar(b.puntosCosto)}</td>
        </tr>`).join('');
}

async function cargarCanjes() {
    const canjes = await llamar('GET', '/beneficios');
    tablaCanjes.innerHTML = canjes.map(c => `
        <tr>
            <td>${escapar(c.fechaCanje)}</td>
            <td>${escapar(c.identificacionUsuario)}</td>
            <td>${escapar(c.descripcion)}</td>
            <td>${escapar(c.puntosCosto)}</td>
        </tr>`).join('');
}

async function recargarTodo() {
    await Promise.all([cargarUsuarios(), cargarCatalogo(), cargarCanjes()]);
}

formulario.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    const datos = Object.fromEntries(new FormData(formulario));
    try {
        await llamar('POST', '/beneficios', {
            identificacionUsuario: datos.identificacionUsuario,
            idBeneficio: datos.idBeneficio
        });
        mostrar('Beneficio canjeado', false);
        formulario.reset();
        await recargarTodo();
    } catch (error) {
        mostrar(error.message, true);
    }
});

recargarTodo().catch(error => mostrar(error.message, true));