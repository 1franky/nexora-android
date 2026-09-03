# Plan de desarrollo — nexora-android

## 1. Visión general

`nexora-android` es la aplicación móvil Android de **Nexora**, una plataforma de administración de finanzas personales inspirada en conceptos de herramientas como Firefly III. Consume la API central (`nexora-api`) y no debe duplicar reglas de negocio financieras: toda la lógica y cálculos importantes viven en el backend. Se distribuye como **APK**.

Funcionalidad que la app debe exponer al usuario:

- Cuentas de ahorro, débito/corriente, tarjetas de crédito, AFORE y PPR.
- Registro de ingresos y egresos.
- Transferencias entre cuentas.
- Compras con tarjeta de crédito, incluyendo MSI y MCI.
- Fechas de corte y fechas límite de pago.
- Notificaciones de pagos de tarjetas.
- Dashboard con métricas personalizables.

```text
nexora-android  ──►  nexora-api  ──►  PostgreSQL
```

---

## 2. Conceptos de dominio a reflejar en la app

Estos conceptos los calcula y valida el backend; la app solo los presenta y permite capturarlos.

- **Tipos de cuenta**: débito/corriente, ahorro, tarjeta de crédito, AFORE, PPR (un usuario puede tener más de un PPR).
- **Inclusión en métricas**: cada cuenta puede incluirse/excluirse del saldo disponible y del patrimonio neto.
- **Dinero disponible** vs. **Patrimonio neto**: métricas distintas, deben mostrarse como tal.
- **Movimientos**: `INCOME`, `EXPENSE`, `TRANSFER`, `CREDIT_CARD_PURCHASE`, `CREDIT_CARD_PAYMENT`, `REFUND`, `ADJUSTMENT`.
  - Una transferencia nunca se muestra como ingreso + gasto.
  - Un pago de tarjeta nunca se muestra como gasto adicional.
- **Tarjetas de crédito**: nombre, banco, últimos 4 dígitos, límite, día de corte, día límite de pago, saldo utilizado, crédito disponible.
- **MSI/MCI**: compra financiada relacionada con un `InstallmentPlan` y sus `Installment`; la app debe permitir ver cuotas pagadas/pendientes, saldo financiado, próxima cuota y fecha de finalización.

---

## 3. Stack propuesto

- Kotlin.
- Jetpack Compose.
- Navigation Compose.
- Retrofit.
- Kotlin Serialization.
- Room.
- Hilt.
- WorkManager.
- Firebase Cloud Messaging (notificaciones push).

Distribución: **APK**.

---

## 4. Idioma

Disponible inicialmente en **español**, con arquitectura preparada para internacionalización posterior. Evitar textos escritos directamente en las pantallas; usar `strings.xml` o el mecanismo equivalente de recursos de Compose.

---

## 5. Tema visual

Implementar modo claro, modo oscuro y modo según sistema:

```text
Tema
├── Claro
├── Oscuro
└── Sistema
```

La selección del usuario debe persistirse. Considerar correctamente: contraste, legibilidad, estados de botones, gráficas en ambos temas, formularios, diálogos, notificaciones y accesibilidad. Seguir **Material Design 3 / Material 3**.

---

## 6. Dashboard

La pantalla principal debe mostrar de forma resumida:

```text
Disponible
$15,000

Deuda
$8,500

Próximo pago
BBVA
$4,850

Gastos del mes
$18,300
```

Y accesos rápidos:

```text
+ Ingreso
+ Gasto
+ Transferencia
+ Compra tarjeta
+ Pago tarjeta
```

---

## 7. Modo offline

La aplicación debe diseñarse para poder evolucionar hacia funcionamiento offline:

```text
Android
   |
   v
Room
   |
   v
Sin conexión
   |
   v
Registrar movimiento
   |
   v
Internet disponible
   |
   v
Sincronización
   |
   v
API
```

Para esto, `nexora-api` deberá considerar idempotencia, identificadores únicos, sincronización y manejo de conflictos. El soporte offline completo puede dejarse para una fase posterior al MVP.

---

## 8. Notificaciones

Eventos soportados por el backend que la app debe poder recibir/mostrar (vía Firebase Cloud Messaging):

```text
PAYMENT_DUE
PAYMENT_DUE_SOON
PAYMENT_OVERDUE
INSTALLMENT_DUE
BUDGET_EXCEEDED
UNUSUAL_EXPENSE
```

Ejemplo: *"Tu tarjeta BBVA vence en 3 días. Pago estimado: $4,850."*

---

## 9. Roadmap

### A1 — Base

- Proyecto Kotlin.
- Jetpack Compose.
- Material 3.
- Login.
- Navegación.
- Arquitectura base.
- Español.
- Tema claro/oscuro/sistema.

### A2 — Dashboard

- Dashboard inicial.

### A3 — Movimientos

- Registro y consulta de movimientos.

### A4 — Cuentas

- Cuentas.

### A5 — Tarjetas

- Consulta y gestión de tarjetas.

### A6 — MSI/MCI

- MSI/MCI.

### A7 — Notificaciones

- Integración con Firebase Cloud Messaging.

### A8 — Offline/sincronización

- Room + sincronización con la API.

### A9 — Release

- Testing.
- Generación de APK.
- Firma.
- Release.

### A10 — Bloqueo de la app con huella digital

Ver sección 13 para el diseño completo.

- `AppLockManager` (estado en memoria `isUnlocked` + preferencia persistida `lockEnabled`, DataStore).
- Hook de foreground/background vía `ProcessLifecycleOwner` — re-bloquea al pasar la app a segundo plano.
- Pantalla de bloqueo + integración de `BiometricPrompt` (biometría con reintento en credencial del dispositivo).
- Toggle en Ajustes para activar/desactivar el bloqueo.
- `FLAG_SECURE` en la Activity principal.
- Migrar `TokenStore` de DataStore plano a `EncryptedFile`/Jetpack Security (Keystore).

---

## 10. MVP Android

- Español.
- Modo claro / oscuro / sistema.
- Dashboard (disponible, deuda, próximo pago, gastos del mes).
- Registro de movimientos (ingreso, gasto, transferencia).
- Consulta de tarjetas.
- Notificaciones.

---

## 11. Funcionalidades posteriores al MVP

```text
Presupuestos
Metas de ahorro
Reportes avanzados
Gráficas
Histórico de AFORE
Multi-moneda avanzada
Transacciones recurrentes
Exportación
Modo offline completo
2FA
```

---

## 12. Reglas importantes para esta app

1. No implementar reglas financieras duplicadas: los cálculos importantes viven en `nexora-api`.
2. Las transferencias no se contabilizan como ingresos y gastos.
3. El pago de una tarjeta no se contabiliza como gasto adicional.
4. El saldo disponible y el patrimonio neto deben mostrarse como métricas distintas.
5. Debe soportar español desde el MVP, con arquitectura preparada para más idiomas.
6. Debe soportar modo claro, oscuro y sistema desde el MVP, persistiendo la preferencia del usuario.
7. Debe consumir el mismo contrato de API (OpenAPI) que `nexora-web`.
8. No se deben almacenar ni mostrar datos sensibles de tarjetas (CVV, NIP, número completo).

---

## 13. Sesión persistente y bloqueo con huella digital (A10)

### 14.1 Punto de partida — qué ya existe hoy

La **persistencia de sesión** ya está implementada desde A1 y no requiere trabajo nuevo: `TokenStore` (`data/auth/TokenStore.kt`) guarda el par access/refresh token en DataStore, y `AuthRepository.isAuthenticated` (`Flow<Boolean>`) es la fuente única de verdad que usa `NexoraNavHost` para decidir si mostrar Login o Dashboard — la sesión sobrevive a cerrar y reabrir la app, sin volver a pedir usuario/contraseña.

Lo que **no existe** y es el objeto real de esta fase es un **bloqueo de la app** encima de esa sesión ya persistida: aunque el usuario siga autenticado, la primera pantalla al abrir (o volver a) la app debe pedir desbloquear con huella digital — igual que una app bancaria, donde estar "logueado" y estar "desbloqueado" son dos estados distintos.

### 14.2 Diseño

```text
isAuthenticated (AuthRepository, ya existe)
        │
        ▼
   ¿lockEnabled?  ──no──► mostrar NavHost normal (comportamiento actual)
        │ sí
        ▼
   ¿isUnlocked?   ──sí──► mostrar NavHost normal
        │ no
        ▼
   LockScreen (pantalla de bloqueo)
        │  botón "Desbloquear"
        ▼
   BiometricPrompt (androidx.biometric)
        │
   ┌────┴────┐
 éxito     cancelar/fallar
   │            │
isUnlocked=true  se queda en LockScreen
                 (con opción de "Cerrar sesión" como escape hatch)
```

- **`AppLockManager`** (nuevo, `data/lock/`): expone
  - `lockEnabled: Flow<Boolean>` — preferencia del usuario, persistida en DataStore (clave nueva, mismo mecanismo que `TokenStore`), **desactivada por defecto** (opt-in desde Ajustes, no se fuerza en el onboarding — más simple para el MVP de esta fase y evita bloquear a quien use un dispositivo sin biometría configurada).
  - `isUnlocked: StateFlow<Boolean>` — **solo en memoria**, nunca persistido. Arranca en `false` en cada proceso nuevo. Pasa a `true` tras un `BiometricPrompt` exitoso.
  - `fun lock()` / `fun reportUnlocked()`.
- **Disparo del bloqueo**: `ProcessLifecycleOwner` (requiere agregar `androidx.lifecycle:lifecycle-process`) observado una sola vez a nivel de aplicación (no por Activity, para que cambiar de pantalla dentro de la app no dispare falsos positivos). En `ON_STOP` (la app pasa a segundo plano) → `AppLockManager.lock()`. Política MVP: **bloqueo inmediato** al pasar a background (no hay temporizador de gracia) — más simple de razonar y más seguro por defecto; un "desbloquear de nuevo solo si pasaron N minutos" queda como posible ajuste futuro, no MVP.
- **Punto de integración**: en `NexoraNavHost` (o un wrapper nuevo alrededor de él), junto al `when (isAuthenticated)` ya existente, se agrega el gate de `lockEnabled && !isUnlocked` → renderizar `LockScreen` en vez del `Scaffold`/`NavHost` normal. No es una ruta más del `NavHost` (evita que quede en el back stack o que un `popBackStack` la esquive) — es una decisión de renderizado al mismo nivel que la de auth.
- **`LockScreen`**: pantalla simple (logo, nombre de la app, botón "Desbloquear" que dispara el `BiometricPrompt`, mensaje de error si el usuario cancela o falla, y una opción secundaria "Cerrar sesión" — necesaria como salida si el dispositivo cambió de dueño temporal o el usuario no puede autenticarse por biometría: cierra sesión de verdad, vía `AuthRepository.logout()`, en vez de dejar a alguien atascado).
- **`BiometricPrompt`** (librería `androidx.biometric`, agregar a `libs.versions.toml`): `setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)` — Android resuelve solo el fallback a PIN/patrón/contraseña del dispositivo si la huella falla o no hay biometría enrolada; **no hay que construir una pantalla de PIN propia**. Si el dispositivo no tiene ningún método de desbloqueo seguro configurado (`BiometricManager.canAuthenticate()` devuelve `BIOMETRIC_ERROR_NO_HARDWARE`/`NO_DEVICE_CREDENTIAL`), la opción de activar el bloqueo en Ajustes se deshabilita con una explicación — no tiene sentido ofrecerlo si no puede funcionar.
- **Ajustes**: requiere una pantalla de Ajustes nueva (no existe hoy) con al menos el toggle "Bloquear con huella digital". Puede ser tan simple como una sola fila por ahora; no es objeto de esta fase construir una sección de Ajustes completa.

### 14.3 Endurecimiento de seguridad (mismo momento, por estar ya tocando esta área)

- **`FLAG_SECURE`** en la `Activity` principal: evita que el contenido de la app aparezca en capturas de pantalla o en la miniatura del selector de apps recientes — relevante para una app financiera, y trivial de agregar junto con el bloqueo.
- **Migrar `TokenStore`** de DataStore plano a almacenamiento cifrado (Jetpack Security — `androidx.security:security-crypto`, clave respaldada por Android Keystore). Hoy los tokens ya están protegidos por el sandbox de la app (otro proceso no puede leerlos sin root), pero cifrarlos en reposo es la práctica esperada para una app que además presume de tener "bloqueo" — deja de tener sentido bloquear la UI si el token que abre la sesión real sigue en texto plano en disco. Se puede hacer en un PR aparte, no bloquea el resto de A10.
- **Fuera de alcance de A10** (anotado para no perderlo, no para resolverlo ahora): atar el `BiometricPrompt` a un `CryptoObject` respaldado por una clave de Keystore que efectivamente cifre/descifre el token (en vez de ser solo un gate de presencia) — así una huella nueva agregada al dispositivo invalidaría automáticamente el desbloqueo. Requiere que el token ya esté en almacenamiento cifrado (punto anterior) primero.

### 14.4 Decisiones que quedan abiertas (a confirmar con el usuario antes de implementar)

1. **Default de `lockEnabled`**: ¿activado por defecto tras el primer login, o el usuario debe activarlo desde Ajustes? Este plan asume opt-in (desactivado por defecto).
2. **Política de re-bloqueo**: ¿siempre inmediato al pasar a segundo plano (lo que asume este plan), o con un margen de gracia (p.ej. no volver a pedirlo si pasaron menos de 30 segundos, para no interrumpir un cambio rápido a otra app y volver)?
3. **Ubicación del toggle**: si no existe aún una pantalla de Ajustes, ¿se crea una nueva (mínima, solo con esto) o se agrega al menú donde hoy está "Cerrar sesión" (ícono en el Dashboard)?

---

## 14. Repositorios relacionados

- `nexora-api` — backend/API central que esta app consume.
- `nexora-web` — aplicación web, comparte el mismo contrato de API.
