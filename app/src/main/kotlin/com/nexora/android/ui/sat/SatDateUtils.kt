package com.nexora.android.ui.sat

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * SatController espera `desde`/`hasta` como date-time ISO (ver
 * plan-integracion-sat.md sección 7.4) — la UI solo captura el día (LocalDate,
 * mismo DatePicker que el resto de la app), así que arma el inicio/fin de ese
 * día en hora local antes de mandarlo, para no dejar fuera el día completo de
 * [hasta] al comparar por instante.
 */
fun isoStartOfDay(date: LocalDate): String = date.atTime(LocalTime.MIN).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
fun isoEndOfDay(date: LocalDate): String = date.atTime(LocalTime.of(23, 59, 59)).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
