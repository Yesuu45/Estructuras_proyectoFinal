async function cargarUsuarios() {
    const usuarios = await peticion('GET', '/usuarios');
    pintar('tabla-usuarios', usuarios.map(u =>
        `<tr><td>${escapar(u.identificacion)}</td><td>${escapar(u.nombre)}</td>
         <td>${escapar(u.correo)}</td><td>${u.puntos}</td><td>${escapar(u.nivel)}</td></tr>`));
}

cargarUsuarios().catch(err => mostrarMensaje(err.message, 'error'));