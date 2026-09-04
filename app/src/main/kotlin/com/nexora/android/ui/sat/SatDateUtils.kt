package com.nexora.android.ui.sat

import java.time.LocalDate
import java.time.ZoneId

/**
 * SatController espera `desde`/`hasta` como `java.time.Instant` (con offset —
 * ver SatDtos.kt en nexora-api), pero la UI solo captura el día (LocalDate,
 * mismo DatePicker que el resto de la app), así que arma el inicio/fin de ese
 * día en la zona horaria del dispositivo antes de mandarlo, para no dejar
 * fuera el día completo de [hasta] al comparar por instante.
 *
 * Antes se formateaba con DateTimeFormatter.ISO_LOCAL_DATE_TIME (p. ej.
 * "2026-08-01T00:00:00", sin offset) — Jackson no puede deserializar eso como
 * Instant y el backend respondía 400 (DateTimeParseException), por lo que la
 * sync por rango de fechas nunca llegaba a ejecutarse. Instant.toString() sí
 * produce el formato ISO-8601 con offset ("Z") que el backend espera.
 */
fun isoStartOfDay(date: LocalDate): String = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
fun isoEndOfDay(date: LocalDate): String = date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toString()
