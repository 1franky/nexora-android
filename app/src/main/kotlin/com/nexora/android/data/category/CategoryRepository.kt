package com.nexora.android.data.category

import com.nexora.android.data.common.apiCall
import com.nexora.android.data.offline.OfflineCache
import com.nexora.android.data.offline.cachedApiCall

private const val CACHE_KEY_CATEGORIES = "categories"

class CategoryRepository(
    private val categoryApi: CategoryApi,
    private val offlineCache: OfflineCache,
) {
    suspend fun listCategories(fallbackError: String): List<Category> =
        cachedApiCall(offlineCache, CACHE_KEY_CATEGORIES, fallbackError) { categoryApi.listCategories() }

    /**
     * Sin caché ni cola offline a propósito: la categoría recién creada se
     * usa de inmediato como categoryId del movimiento que se está armando
     * en el mismo formulario, y sin conexión no hay forma de tener ya un id
     * real del servidor para eso. Si el usuario está sin conexión, esta
     * llamada falla con el error normal de red — puede seguir eligiendo
     * entre las categorías ya existentes, que sí vienen del caché.
     */
    suspend fun createCategory(name: String, type: CategoryType, fallbackError: String): Category =
        apiCall(fallbackError) { categoryApi.createCategory(CreateCategoryRequest(name, type)) }
}
