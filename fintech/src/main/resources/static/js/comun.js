const API = '/api';

function mostrarMensaje(texto, tipo = 'ok') {
    const caja = document.getElementById('mensajes');
    caja.textContent = texto;
    caja.className = 'mensaje ' + tipo;
    caja.hidden = false;
}

async function peticion(metodo, ruta, datos) {
    const opciones = { method: metodo, headers: { 'Content-Type': 'application/json' } };
    if (datos !== undefined) {
        opciones.body = JSON.stringify(datos);
    }
    const respuesta = await fetch(API + ruta, opciones);
    const cuerpo = await respuesta.json().catch(() => null);
    if (!respuesta.ok) {
        throw new Error((cuerpo && cuerpo.mensaje) || 'Error ' + respuesta.status);
    }
    return cuerpo;
}

function escapar(valor) {
    const div = document.createElement('div');
    div.textContent = valor ?? '';
    return div.innerHTML;
}

function pintar(idTabla, filas) {
    document.getElementById(idTabla).innerHTML = filas.join('');
}

function conectarFormulario(idFormulario, ruta, transformar, textoOk, recargar) {
    document.getElementById(idFormulario).addEventListener('submit', async evento => {
        evento.preventDefault();
        const formulario = evento.target;
        const datos = Object.fromEntries(new FormData(formulario));
        try {
            await peticion('POST', ruta, transformar ? transformar(datos) : datos);
            formulario.reset();
            mostrarMensaje(textoOk);
            await recargar();
        } catch (err) {
            mostrarMensaje(err.message, 'error');
        }
    });
}

function marcarPaginaActiva() {
    const actual = location.pathname.split('/').pop() || 'index.html';
    document.querySelectorAll('nav a').forEach(enlace => {
        enlace.classList.toggle('activo', enlace.getAttribute('href') === actual);
    });
}

marcarPaginaActiva();