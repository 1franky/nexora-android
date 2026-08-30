package com.nexora.android.data.category

import com.nexora.android.data.common.apiCall

class CategoryRepository(private val categoryApi: CategoryApi) {
    suspend fun listCategories(fallbackError: String): List<Category> =
        apiCall(fallbackError) { categoryApi.listCategories() }

    suspend fun createCategory(name: String, type: CategoryType, fallbackError: String): Category =
        apiCall(fallbackError) { categoryApi.createCategory(CreateCategoryRequest(name, type)) }
}
