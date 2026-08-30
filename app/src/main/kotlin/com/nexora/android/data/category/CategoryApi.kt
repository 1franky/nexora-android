package com.nexora.android.data.category

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CategoryApi {
    @GET("categories")
    suspend fun listCategories(): List<Category>

    @POST("categories")
    suspend fun createCategory(@Body request: CreateCategoryRequest): Category
}
