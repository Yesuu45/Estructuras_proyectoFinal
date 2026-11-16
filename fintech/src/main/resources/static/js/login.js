// Pantalla de acceso. No usa comun.js: esa librería redirige aquí cuando no hay sesión.
const mensajes = document.getElementById('mensajes');

function avisar(texto, esError) {
    mensajes.textContent = texto;
    mensajes.className = 'mensaje ' + (esError ? 'error' : 'ok');
    mensajes.hidden = false;
}

async function enviar(ruta, datos) {
    const respuesta = await fetch('/api' + ruta, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(datos)
    });
    const cuerpo = await respuesta.json().catch(() => ({}));
    if (!respuesta.ok) {
        throw new Error(cuerpo.mensaje || 'Error ' + respuesta.status);
    }
    return cuerpo;
}

async function entrar(identificacion, clave) {
    await enviar('/auth/login', { identificacion, clave });
    location.href = 'index.html';
}

document.getElementById('form-login').addEventListener('submit', async evento => {
    evento.preventDefault();
    const datos = Object.fromEntries(new FormData(evento.target));
    try {
        await entrar(datos.identificacion, datos.clave);
    } catch (error) {
        avisar(error.message, true);
    }
});

document.getElementById('form-registro').addEventListener('submit', async evento => {
    evento.preventDefault();
    const datos = Object.fromEntries(new FormData(evento.target));
    try {
        await enviar('/usuarios', datos);
        await entrar(datos.identificacion, datos.clave);   // tras registrarse, entra directamente
    } catch (error) {
        avisar(error.message, true);
    }
});

// Si ya hay sesión, no tiene sentido mostrar el formulario
fetch('/api/auth/yo').then(r => { if (r.ok) location.href = 'index.html'; }).catch(() => {});
