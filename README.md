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
- Notificaciones push de pagos de tarjetas.
- Dashboard con métricas resumidas y accesos rápidos.

## Stack

- Kotlin
- Jetpack Compose
- Navigation Compose
- Retrofit
- Kotlin Serialization
- Room
- Hilt
- WorkManager
- Firebase Cloud Messaging

## Idioma

Disponible inicialmente en **español**, con arquitectura preparada para internacionalización posterior (recursos `strings.xml` / equivalente en Compose).

## Tema visual

Modo claro, oscuro y según sistema, siguiendo **Material Design 3**, con la preferencia persistida entre sesiones.

## Dashboard

Pantalla principal con disponible, deuda, próximo pago y gastos del mes, más accesos rápidos para registrar ingreso, gasto, transferencia, compra y pago de tarjeta.

## Modo offline

Diseñada para evolucionar hacia funcionamiento offline con Room y sincronización posterior contra la API (idempotencia, identificadores únicos, manejo de conflictos).

## Estado del proyecto

En fase de diseño / arranque. Ver [`plan.md`](./plan.md) para el plan de desarrollo completo (roadmap, MVP y reglas de la app).

## Repositorios relacionados

- [`nexora-api`](https://github.com/1franky/nexora-api) — backend/API central que esta app consume
- [`nexora-web`](https://github.com/1franky/nexora-web) — aplicación web, comparte el mismo contrato de API
