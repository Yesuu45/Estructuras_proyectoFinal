// Si el servidor responde 401 (sin sesión) en cualquier llamada, vuelve a la pantalla de acceso.
// Se envuelve fetch aquí porque varias páginas tienen su propia función de llamadas.
const fetchOriginal = window.fetch.bind(window);
window.fetch = async (...argumentos) => {
    const respuesta = await fetchOriginal(...argumentos);
    if (respuesta.status === 401) {
        location.href = 'login.html';
    }
    return respuesta;
};

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

// Barra de sesión: muestra quién entró y permite salir. Sin sesión, el fetch envuelto redirige a login.html
async function mostrarSesion() {
    const respuesta = await fetch(API + '/auth/yo');
    if (!respuesta.ok) {
        return;
    }
    const usuario = await respuesta.json();
    const nav = document.querySelector('nav');
    if (!nav) {
        return;
    }
    const caja = document.createElement('div');
    caja.className = 'sesion';
    caja.innerHTML = '<span>' + escapar(usuario.nombre) + ' · ' + escapar(usuario.nivel) + '</span>'
        + '<a href="../login.html" id="cerrar-sesion">Cerrar sesión</a>';
    nav.appendChild(caja);
    document.getElementById('cerrar-sesion').addEventListener('click', async evento => {
        evento.preventDefault();
        await fetchOriginal(API + '/auth/logout', { method: 'POST' });
        location.href = 'login.html';
    });
}

mostrarSesion().catch(() => {});
