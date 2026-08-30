package com.nexora.android.data.category

import kotlinx.serialization.Serializable

/** Espejo de com.nexora.api.category.web (nexora-api). Solo lo que necesita el formulario de movimientos (A3): listar y crear al vuelo. */

@Serializable
enum class CategoryType { INCOME, EXPENSE }

@Serializable
enum class CategoryStatus { ACTIVE, ARCHIVED }

@Serializable
data class Category(
    val id: String,
    val name: String,
    val type: CategoryType,
    val status: CategoryStatus,
)

@Serializable
data class CreateCategoryRequest(val name: String, val type: CategoryType)
