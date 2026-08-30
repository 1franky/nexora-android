# nexora-android

Aplicación móvil Android de **Nexora**, una plataforma de administración de finanzas personales inspirada en conceptos de herramientas como Firefly III.

Consume la API central [`nexora-api`](https://github.com/1franky/nexora-api). No implementa reglas financieras propias: toda la lógica y cálculos importantes viven en el backend. Se distribuye como **APK**.

```text
nexora-android  ──►  nexora-api  ──►  PostgreSQL
```

## Funcionalidad

- Cuentas: débito/corriente, ahorro, tarjeta de crédito, AFORE, PPR.
- Registro de ingresos, egresos y transferencias.
- Compras con tarjeta de crédito, incluyendo MSI y MCI.
- Fechas de corte y fecha límite de pago.
- Notificaciones en la app (bandeja) de pagos y cuotas por vencer.
- Dashboard con métricas resumidas y accesos rápidos.
- Funciona sin conexión: consulta lo último visto y encola movimientos, compras y pagos para sincronizarlos solos al reconectar.

## Stack

- Kotlin
- Jetpack Compose
- Navigation Compose
- Retrofit
- Kotlin Serialization
- Room (caché de lecturas + cola de escritura offline)
- WorkManager (sincronización en segundo plano)

Sin Hilt ni Firebase Cloud Messaging: la inyección de dependencias es manual
(ver `AppContainer`, comentario en el propio archivo) y las notificaciones
son una bandeja in-app (`GET /api/v1/notifications`), no push — el backend
todavía no envía por FCM (ver B6 en `nexora-api`).

## Idioma

Disponible inicialmente en **español**, con arquitectura preparada para internacionalización posterior (recursos `strings.xml` / equivalente en Compose).

## Tema visual

Modo claro, oscuro y según sistema, siguiendo **Material Design 3**, con la preferencia persistida entre sesiones.

## Dashboard

Pantalla principal con disponible, deuda, próximo pago y gastos del mes, más accesos rápidos para registrar ingreso, gasto, transferencia, compra y pago de tarjeta.

## Modo offline

Room cachea lo último visto (cuentas, tarjetas, movimientos, categorías,
dashboard, planes MSI/MCI) para poder consultarlo sin conexión. Toda
escritura (alta de cuenta/tarjeta, movimientos, transferencias, compras,
pagos, planes MSI/MCI, marcar cuota pagada) se puede hacer sin conexión: si
falla por falta de red, queda encolada localmente y un `Worker` de
WorkManager la reintenta sola en cuanto vuelve la conexión, usando una
`Idempotency-Key` por operación para que un reintento nunca duplique el
movimiento del lado del servidor (ver B8 en `nexora-api`).

A propósito, mientras una operación está encolada la app no inventa el
resultado que calcularía el backend (saldo nuevo, interés de un plan
MSI/MCI...) — mostrar una cifra financiera que luego resulte incorrecta es
peor que un banner de "sincronizando". La app solo muestra que hay cambios
pendientes; los números se actualizan de verdad cuando el backend los
procesa.

## Testing

Tests unitarios JVM (`app/src/test`, sin Robolectric/emulador): la lógica
más riesgosa de probar sin red real — la decisión online/offline de
`cachedApiCall`/`writeCall`, y el despacho de `OperationDispatcher` por
tipo de operación — más formatters. Corren en CI en cada push/PR
(`.github/workflows/ci.yml`).

## Release

`.github/workflows/release.yml` compila, firma y publica un
[Release de GitHub](https://github.com/1franky/nexora-android/releases)
con el APK cada vez que se sube un tag `vX.Y.Z` (no dispara con los tags
`vX.Y.Z-debug` de builds sin firmar de antes de A9). La clave de firma
vive como GitHub Secret (`RELEASE_KEYSTORE_BASE64` + contraseñas) — nunca
en el repo. Para compilar `assembleRelease` en local hace falta un
`keystore.properties` propio (ver `keystore.properties.example`,
gitignored).

## Estado del proyecto

Roadmap completo (A1-A9, ver [`plan.md`](./plan.md)): base, login/registro,
dashboard, cuentas, movimientos, tarjetas, MSI/MCI, notificaciones, modo
offline, testing y release firmado. Consume la API real desplegada en
`https://nexora-api.franciscolopez.uk`.

## Repositorios relacionados

- [`nexora-api`](https://github.com/1franky/nexora-api) — backend/API central que esta app consume
- [`nexora-web`](https://github.com/1franky/nexora-web) — aplicación web, comparte el mismo contrato de API
