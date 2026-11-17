# Plataforma Fintech de Billeteras Digitales

Proyecto final de **Estructura de Datos** (Ingeniería de Sistemas y Computación, Universidad del Quindío).

**Autores:** Diego Alejandro Florez Serna y Yesuu Esteban Aros Tique
**Profesor:** Robinson Arias Muñoz

Aplicación web en la que los usuarios administran billeteras, mueven dinero, programan operaciones, reciben alertas y ganan beneficios según su nivel. Las siete estructuras de datos principales están implementadas desde cero, sin usar colecciones de `java.util` en la lógica de la aplicación.

## Funcionalidades

- **Usuarios y billeteras:** registro, edición y eliminación; varias billeteras por usuario (ahorros, corriente o digital).
- **Operaciones:** recargar, retirar y transferir, con consulta de saldo e historial.
- **Reversión y deshacer:** reversión de operaciones válidas con recálculo de puntos y deshacer la última operación.
- **Operaciones programadas:** únicas o periódicas (diaria, semanal, mensual), ejecutadas por prioridad de fecha, con un reloj simulado para probarlas.
- **Recompensas:** puntos por transacción, niveles Bronce, Plata, Oro y Platino, límites por nivel y canje de beneficios.
- **Alertas:** saldo bajo, programada próxima, operación rechazada, ascenso de nivel y canje, con las 10 notificaciones más recientes por usuario.
- **Analítica:** billeteras más usadas, usuarios con más transferencias, categorías activas, frecuencia por tipo, monto movilizado, usuario más activo, ciclos y rutas frecuentes y transacciones de mayor valor.
- **Comportamiento inusual:** detección de ráfagas, montos atípicos, envíos repetidos, fragmentación y horarios no habituales, con nivel de riesgo y auditoría consultable.
- **Comparación de rendimiento:** mide las estructuras propias frente a las de Java.

## Estructuras de datos

| Estructura | Clase | Uso principal |
| --- | --- | --- |
| Lista enlazada | `SimpleLinkedList` | Libro de transacciones, historiales y canjes |
| Pila | `Stack` | Deshacer operaciones y recorrido en profundidad |
| Cola | `Cola` | Auditoría, notificaciones y recorrido en amplitud |
| Montículo binario | `MonticuloBinario` | Operaciones programadas por fecha y transacciones de mayor valor |
| Árbol binario de búsqueda | `ArbolBinario` | Ranking de usuarios por puntos y búsqueda por rango |
| Tabla hash | `TablaHash` | Usuarios, billeteras, transacciones y catálogo por clave |
| Grafo dirigido ponderado | `Grafo` | Red de transferencias, rutas y ciclos |

## Arquitectura

Aplicación de tres capas con Java 21, Spring Boot y PostgreSQL. La interfaz es HTML, CSS y JavaScript servidos como páginas estáticas que consumen una API REST.

```
fintech/src/main/java/co/edu/uniquindio/poo/
├── App/            Clase principal
├── Estructuras/    Las estructuras de datos propias
├── modelo/         Entidades y enumeraciones
├── servicio/       PlataformaService: reglas de negocio y analítica
├── persistencia/   RepositorioPlataforma: acceso a PostgreSQL
├── seguridad/      Contraseñas (PBKDF2) y cuenta de administrador
└── ui/             Controladores REST, filtro de sesión y errores
```

Los datos se guardan en PostgreSQL con cada cambio y se reconstruyen en memoria al arrancar. Las notificaciones y el reloj simulado no se guardan.

## Requisitos

- Java 21
- PostgreSQL con una base de datos llamada `fintech`

## Cómo ejecutarlo

1. Crear la base de datos y un usuario con acceso:

   ```sql
   CREATE USER fintech WITH PASSWORD 'fintech';
   CREATE DATABASE fintech OWNER fintech;
   ```

2. Si los datos de conexión son otros, definir las variables de entorno `DB_URL`, `DB_USER` y `DB_PASSWORD`. Por defecto se usa `jdbc:postgresql://localhost:5432/fintech` con usuario y clave `fintech`.

3. Desde la carpeta `fintech`, iniciar la aplicación:

   ```bash
   ./mvnw spring-boot:run
   ```

   En Windows: `mvnw.cmd spring-boot:run`.

4. Abrir <http://localhost:8083> e iniciar sesión. Las tablas se crean solas con `schema.sql`.

## Cuentas de prueba

| Rol | Identificación | Clave |
| --- | --- | --- |
| Administrador | `admin` | `admin123` |
| Usuario | `1001` a `1005` (Laura, Carlos, Daniela, Esteban y Valentina) | `clave123` |

Los usuarios de demostración se cargan al arrancar y se pueden desactivar con `app.datos-demo=false` en `application.properties`. La clave del administrador debe cambiarse antes de un uso real.

## Pruebas

```bash
./mvnw test
```

Son 27 pruebas con JUnit que cubren las estructuras de datos y las reglas del servicio (saldos, límites por nivel, reversión, deshacer y operaciones programadas). Las del servicio usan un repositorio simulado con Mockito y no necesitan la base de datos. `FintechApplicationTests` levanta la aplicación completa y sí la necesita.
