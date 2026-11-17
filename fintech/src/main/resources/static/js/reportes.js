// Reportes del administrador: ranking y rangos (árbol) y red de transferencias (grafo).
let nombres = {};

function filaUsuario(u) {
    return `<td>${escapar(u.identificacion)}</td><td>${escapar(u.nombre)}</td><td>${u.puntos}</td><td>${escapar(u.nivel)}</td>`;
}

async function cargarRanking() {
    const usuarios = await peticion('GET', '/reportes/ranking');
    pintar('tabla-ranking', usuarios.map((u, i) => `<tr><td>${i + 1}</td>${filaUsuario(u)}</tr>`));
}

async function cargarNiveles() {
    const niveles = await peticion('GET', '/reportes/niveles');
    pintar('tabla-niveles', niveles.map(n => {
        const rango = n.maximo === null ? `${n.minimo} en adelante` : `${n.minimo} – ${n.maximo}`;
        return `<tr><td>${escapar(n.nivel)}</td><td>${rango}</td><td>${n.cantidad}</td></tr>`;
    }));
}

function etiqueta(id) {
    return nombres[id] ? `${id} · ${nombres[id]}` : id;
}

function filasRed(tabla, aristas) {
    pintar(tabla, aristas.length === 0
        ? ['<tr><td colspan="4">Todavía no hay transferencias</td></tr>']
        : aristas.map(a => `<tr><td>${escapar(a.origen)}</td><td>${escapar(a.destino)}</td><td>${a.cantidad}</td><td>${escapar(a.total)}</td></tr>`));
}

// Dibuja los vértices en círculo y las aristas como flechas curvas (así A->B y B->A no se pisan)
function dibujarRed(vertices, aristas) {
    const ancho = 560, alto = 380, cx = ancho / 2, cy = alto / 2, r = Math.min(ancho, alto) / 2 - 70, radioNodo = 26;
    const ordenados = [...vertices].sort();
    const pos = {};
    ordenados.forEach((v, i) => {
        const ang = (2 * Math.PI * i) / ordenados.length - Math.PI / 2;
        pos[v] = { x: cx + r * Math.cos(ang), y: cy + r * Math.sin(ang) };
    });

    const flechas = aristas.map(a => {
        const p = pos[a.origen], q = pos[a.destino];
        const dx = q.x - p.x, dy = q.y - p.y, largo = Math.hypot(dx, dy) || 1;
        const ux = dx / largo, uy = dy / largo;
        const x1 = p.x + ux * radioNodo, y1 = p.y + uy * radioNodo;
        const x2 = q.x - ux * (radioNodo + 6), y2 = q.y - uy * (radioNodo + 6);
        const mx = (x1 + x2) / 2 - uy * 28, my = (y1 + y2) / 2 + ux * 28;   // punto de control desviado
        const grosor = 1.5 + Math.min(a.cantidad, 5) * 0.6;
        const tx = 0.25 * x1 + 0.5 * mx + 0.25 * x2, ty = 0.25 * y1 + 0.5 * my + 0.25 * y2;
        return `<path d="M${x1},${y1} Q${mx},${my} ${x2},${y2}" fill="none" stroke="var(--selva)" stroke-width="${grosor}" marker-end="url(#flecha)">
                    <title>${escapar(a.origen)} → ${escapar(a.destino)}: ${a.cantidad} veces, total ${escapar(a.total)}</title></path>
                <text x="${tx}" y="${ty - 4}" text-anchor="middle" font-size="12" fill="var(--guayaba)" font-weight="700">${a.cantidad}×</text>`;
    }).join('');

    const nodos = ordenados.map(v => `
        <circle cx="${pos[v].x}" cy="${pos[v].y}" r="${radioNodo}" fill="var(--superficie)" stroke="var(--selva-oscura)" stroke-width="2"/>
        <text x="${pos[v].x}" y="${pos[v].y + 4}" text-anchor="middle" font-size="12" font-weight="700" fill="var(--tinta)">${escapar(v)}</text>
        <text x="${pos[v].x}" y="${pos[v].y + radioNodo + 15}" text-anchor="middle" font-size="11" fill="var(--tinta-suave)">${escapar((nombres[v] || '').split(' ')[0])}</text>`).join('');

    document.getElementById('dibujo-red').innerHTML = `
        <svg viewBox="0 0 ${ancho} ${alto}" role="img" aria-label="Red de transferencias entre usuarios" style="width:100%;max-width:${ancho}px">
            <defs>
                <marker id="flecha" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
                    <path d="M0,0 L10,5 L0,10 z" fill="var(--selva)"/>
                </marker>
            </defs>
            ${flechas}${nodos}
        </svg>`;
}

async function cargarRedUsuarios() {
    const red = await peticion('GET', '/reportes/red-usuarios');
    dibujarRed(red.vertices, red.aristas);
    filasRed('tabla-red-usuarios', red.aristas);
    const opciones = [...red.vertices].sort().map(v => `<option value="${escapar(v)}">${escapar(etiqueta(v))}</option>`).join('');
    document.getElementById('ruta-desde').innerHTML = '<option value="">Desde…</option>' + opciones;
    document.getElementById('ruta-hasta').innerHTML = '<option value="">Hasta…</option>' + opciones;
}

async function cargarRedBilleteras() {
    const red = await peticion('GET', '/reportes/red-billeteras');
    filasRed('tabla-red-billeteras', red.aristas);
}

document.getElementById('form-rango').addEventListener('submit', async evento => {
    evento.preventDefault();
    const datos = Object.fromEntries(new FormData(evento.target));
    try {
        const usuarios = await peticion('GET',
            `/reportes/usuarios-por-puntos?minimo=${encodeURIComponent(datos.minimo)}&maximo=${encodeURIComponent(datos.maximo)}`);
        pintar('tabla-rango', usuarios.length === 0
            ? ['<tr><td colspan="4">Ningún usuario tiene puntos en ese rango</td></tr>']
            : usuarios.map(u => `<tr>${filaUsuario(u)}</tr>`));
        document.getElementById('resultado-rango').hidden = false;
    } catch (err) {
        mostrarMensaje(err.message, 'error');
    }
});

document.getElementById('form-ruta').addEventListener('submit', async evento => {
    evento.preventDefault();
    const datos = Object.fromEntries(new FormData(evento.target));
    const salida = document.getElementById('resultado-ruta');
    try {
        const camino = await peticion('GET',
            `/reportes/ruta?desde=${encodeURIComponent(datos.desde)}&hasta=${encodeURIComponent(datos.hasta)}`);
        salida.textContent = camino.length === 0
            ? `No ha circulado dinero de ${etiqueta(datos.desde)} hacia ${etiqueta(datos.hasta)}.`
            : `Ruta más corta (${camino.length - 1} ${camino.length === 2 ? 'salto' : 'saltos'}): ${camino.map(etiqueta).join('  →  ')}`;
    } catch (err) {
        mostrarMensaje(err.message, 'error');
    }
});

// ---------- Analítica ----------

function filasConteo(tabla, conteos, conPosicion, conNombre) {
    pintar(tabla, conteos.length === 0
        ? ['<tr><td colspan="3">Sin datos todavía</td></tr>']
        : conteos.map((c, i) => `<tr>${conPosicion ? `<td>${i + 1}</td>` : ''}
            <td>${escapar(conNombre ? etiqueta(c.clave) : c.clave)}</td><td>${c.cantidad}</td></tr>`));
}

async function cargarAnalitica() {
    const [billeteras, usuarios, categorias, tipos, pares] = await Promise.all([
        peticion('GET', '/reportes/analitica/billeteras-usadas?n=5'),
        peticion('GET', '/reportes/analitica/usuarios-transferencias?n=5'),
        peticion('GET', '/reportes/analitica/categorias'),
        peticion('GET', '/reportes/analitica/frecuencia-tipos'),
        peticion('GET', '/reportes/analitica/pares-frecuentes?n=5')
    ]);
    filasConteo('tabla-billeteras-usadas', billeteras, true, false);
    filasConteo('tabla-usuarios-transferencias', usuarios, true, true);
    filasConteo('tabla-categorias', categorias, false, false);
    filasConteo('tabla-frecuencia-tipos', tipos, false, false);
    pintar('tabla-pares', pares.length === 0
        ? ['<tr><td colspan="5">Todavía no hay transferencias entre usuarios</td></tr>']
        : pares.map((a, i) => `<tr><td>${i + 1}</td><td>${escapar(etiqueta(a.origen))}</td>
            <td>${escapar(etiqueta(a.destino))}</td><td>${a.cantidad}</td><td>${escapar(a.total)}</td></tr>`));
}

document.getElementById('form-monto').addEventListener('submit', async evento => {
    evento.preventDefault();
    const datos = Object.fromEntries(new FormData(evento.target));
    try {
        const r = await peticion('GET',
            `/reportes/analitica/monto?desde=${encodeURIComponent(datos.desde)}&hasta=${encodeURIComponent(datos.hasta)}`);
        document.getElementById('resultado-monto').textContent =
            `Entre ${datos.desde} y ${datos.hasta} se movilizaron ${r.total} en ${r.transacciones} transacciones completadas.`;
    } catch (err) {
        mostrarMensaje(err.message, 'error');
    }
});

// ---------- Requisitos adicionales ----------

function recorrido(vertices, cerrado) {
    const pasos = vertices.map(etiqueta);
    return (cerrado ? [...pasos, pasos[0]] : pasos).join(' → ');
}

document.getElementById('form-actividad').addEventListener('submit', async evento => {
    evento.preventDefault();
    const datos = Object.fromEntries(new FormData(evento.target));
    try {
        const top = await peticion('GET',
            `/reportes/actividad?desde=${encodeURIComponent(datos.desde)}&hasta=${encodeURIComponent(datos.hasta)}&n=5`);
        document.getElementById('tabla-actividad-contenedor').hidden = top.length === 0;
        document.getElementById('resultado-actividad').textContent = top.length === 0
            ? 'No hubo transacciones en ese período.'
            : `El usuario con mayor actividad entre ${datos.desde} y ${datos.hasta} es ${etiqueta(top[0].clave)} con ${top[0].cantidad} transacciones.`;
        pintar('tabla-actividad', top.map((c, i) =>
            `<tr><td>${i + 1}</td><td>${escapar(etiqueta(c.clave))}</td><td>${c.cantidad}</td></tr>`));
    } catch (err) {
        mostrarMensaje(err.message, 'error');
    }
});

async function cargarCiclosYRutas() {
    const red = document.getElementById('select-red').value;
    const [ciclos, rutas] = await Promise.all([
        peticion('GET', '/reportes/ciclos?red=' + red),
        peticion('GET', '/reportes/rutas-frecuentes?n=5&red=' + red)
    ]);
    pintar('tabla-ciclos', ciclos.length === 0
        ? ['<tr><td colspan="3">No hay ciclos: el dinero nunca regresa a quien lo envió</td></tr>']
        : ciclos.map(c => `<tr><td>${escapar(recorrido(c.vertices, true))}</td><td>${c.frecuencia}</td><td>${escapar(c.total)}</td></tr>`));
    pintar('tabla-rutas', rutas.length === 0
        ? ['<tr><td colspan="4">Todavía no hay rutas de 2 o más saltos</td></tr>']
        : rutas.map((r, i) => `<tr><td>${i + 1}</td><td>${escapar(recorrido(r.vertices, false))}</td><td>${r.frecuencia}</td><td>${escapar(r.total)}</td></tr>`));
}
document.getElementById('select-red').addEventListener('change', () =>
    cargarCiclosYRutas().catch(err => mostrarMensaje(err.message, 'error')));

async function cargarMayorValor() {
    const n = document.getElementById('select-mayor-valor').value;
    const transacciones = await peticion('GET', '/reportes/mayor-valor?n=' + n);
    pintar('tabla-mayor-valor', transacciones.length === 0
        ? ['<tr><td colspan="7">Todavía no hay transacciones</td></tr>']
        : transacciones.map((t, i) => `<tr><td>${i + 1}</td><td>${escapar(t.id)}</td>
            <td>${escapar(String(t.fecha).replace('T', ' ').substring(0, 16))}</td><td>${escapar(t.tipo)}</td>
            <td>${escapar(t.valor)}</td><td>${escapar(t.codigoOrigen ?? '—')}</td><td>${escapar(t.codigoDestino ?? '—')}</td></tr>`));
}
document.getElementById('select-mayor-valor').addEventListener('change', () =>
    cargarMayorValor().catch(err => mostrarMensaje(err.message, 'error')));

function formatearTiempo(nanos) {
    if (nanos >= 1e6) return (nanos / 1e6).toFixed(2) + ' ms';
    if (nanos >= 1e3) return (nanos / 1e3).toFixed(1) + ' µs';
    return nanos + ' ns';
}

// Una tabla por operación; la barra compara el tiempo total de cada estructura dentro de esa operación
function pintarRendimiento(resultados) {
    const grupos = [];
    resultados.forEach(r => {
        let grupo = grupos.find(g => g.operacion === r.operacion);
        if (!grupo) {
            grupo = { operacion: r.operacion, filas: [] };
            grupos.push(grupo);
        }
        grupo.filas.push(r);
    });
    document.getElementById('resultado-rendimiento').innerHTML = grupos.map(g => {
        const maximo = Math.max(...g.filas.map(f => f.nanosTotal), 1);
        return `<h3>${escapar(g.operacion)}</h3>
        <table>
            <thead><tr><th>Estructura</th><th></th><th>Tiempo total</th><th>Por operación</th><th>Costo teórico</th></tr></thead>
            <tbody>${g.filas.map(f => `<tr>
                <td>${escapar(f.estructura)} <span class="nota">${escapar(f.origen)}</span></td>
                <td style="width:30%"><div style="height:10px;border-radius:3px;background:${f.origen === 'Propia' ? 'var(--selva)' : 'var(--guayaba)'};width:${Math.max(2, Math.round(100 * f.nanosTotal / maximo))}%"></div></td>
                <td>${formatearTiempo(f.nanosTotal)}</td><td>${formatearTiempo(f.nanosPorOperacion)}</td>
                <td>${escapar(f.complejidad)}</td></tr>`).join('')}</tbody>
        </table>`;
    }).join('');
}

document.getElementById('form-rendimiento').addEventListener('submit', async evento => {
    evento.preventDefault();
    const boton = document.getElementById('btn-rendimiento');
    boton.disabled = true;
    boton.textContent = 'Midiendo...';
    try {
        const resultados = await peticion('GET', '/reportes/rendimiento?elementos=' + document.getElementById('select-elementos').value);
        pintarRendimiento(resultados);
    } catch (err) {
        mostrarMensaje(err.message, 'error');
    } finally {
        boton.disabled = false;
        boton.textContent = 'Ejecutar comparación';
    }
});

async function iniciar() {
    const usuarios = await peticion('GET', '/usuarios');
    usuarios.forEach(u => { nombres[u.identificacion] = u.nombre; });
    await Promise.all([cargarRanking(), cargarNiveles(), cargarRedUsuarios(), cargarRedBilleteras(), cargarAnalitica(), cargarCiclosYRutas(), cargarMayorValor()]);
}

iniciar().catch(err => mostrarMensaje(err.message, 'error'));
