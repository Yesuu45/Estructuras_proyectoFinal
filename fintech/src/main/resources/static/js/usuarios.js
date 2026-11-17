let usuariosCargados = [];
let editando = null;

async function cargarUsuarios() {
    usuariosCargados = await peticion('GET', '/usuarios');
    pintar('tabla-usuarios', usuariosCargados.map(u => {
        const id = escapar(u.identificacion);
        return `<tr><td>${id}</td><td>${escapar(u.nombre)}</td>
         <td>${escapar(u.correo)}</td><td>${u.puntos}</td><td>${escapar(u.nivel)}</td>
         <td><button type="button" data-editar="${id}">Editar</button>
             ${u.rol === 'ADMIN' ? '' : `<button type="button" class="secundario" data-eliminar="${id}">Eliminar</button>`}</td></tr>`;
    }));
}

function abrirEdicion(identificacion) {
    const usuario = usuariosCargados.find(u => u.identificacion === identificacion);
    if (!usuario) {
        return;
    }
    editando = identificacion;
    const formulario = document.getElementById('form-edicion');
    formulario.nombre.value = usuario.nombre;
    formulario.correo.value = usuario.correo || '';
    formulario.clave.value = '';
    document.getElementById('editando').textContent = '(' + identificacion + ')';
    document.getElementById('seccion-edicion').hidden = false;
    formulario.nombre.focus();
}

function cerrarEdicion() {
    editando = null;
    document.getElementById('seccion-edicion').hidden = true;
}

document.getElementById('tabla-usuarios').addEventListener('click', async evento => {
    const boton = evento.target.closest('button');
    if (!boton) {
        return;
    }
    if (boton.dataset.editar) {
        abrirEdicion(boton.dataset.editar);
    } else if (boton.dataset.eliminar) {
        const id = boton.dataset.eliminar;
        if (!confirm('¿Eliminar al usuario ' + id + '? Esta acción no se puede deshacer.')) {
            return;
        }
        try {
            await peticion('DELETE', '/usuarios/' + encodeURIComponent(id));
            if (editando === id) {
                cerrarEdicion();
            }
            mostrarMensaje('Usuario ' + id + ' eliminado');
            await cargarUsuarios();
        } catch (err) {
            mostrarMensaje(err.message, 'error');
        }
    }
});

document.getElementById('form-edicion').addEventListener('submit', async evento => {
    evento.preventDefault();
    const datos = Object.fromEntries(new FormData(evento.target));
    try {
        await peticion('PUT', '/usuarios/' + encodeURIComponent(editando), datos);
        mostrarMensaje('Usuario ' + editando + ' actualizado');
        cerrarEdicion();
        await cargarUsuarios();
    } catch (err) {
        mostrarMensaje(err.message, 'error');
    }
});

document.getElementById('cancelar-edicion').addEventListener('click', cerrarEdicion);

cargarUsuarios().catch(err => mostrarMensaje(err.message, 'error'));
