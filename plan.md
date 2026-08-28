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

## 13. Repositorios relacionados

- `nexora-api` — backend/API central que esta app consume.
- `nexora-web` — aplicación web, comparte el mismo contrato de API.
