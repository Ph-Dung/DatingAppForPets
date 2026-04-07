package com.petmatch.mobile.data.api

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.petmatch.mobile.Constants
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.DATASTORE_NAME)

class AuthInterceptor(
    private val context: Context,
    private val tokenKey: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val token = runBlocking {
            context.dataStore.data.map { prefs ->
                prefs[stringPreferencesKey(tokenKey)]
            }.firstOrNull()
        }
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}

object RetrofitClient {
    private var userRetrofit: Retrofit? = null
    private var adminRetrofit: Retrofit? = null

    private fun buildRetrofit(context: Context, tokenKey: String): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context, tokenKey))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getUserInstance(context: Context): Retrofit {
        if (userRetrofit == null) {
            userRetrofit = buildRetrofit(context, Constants.TOKEN_KEY)
        }
        return userRetrofit!!
    }

    fun getAdminInstance(context: Context): Retrofit {
        if (adminRetrofit == null) {
            adminRetrofit = buildRetrofit(context, Constants.ADMIN_TOKEN_KEY)
        }
        return adminRetrofit!!
    }

    fun petApi(context: Context): PetApi = getUserInstance(context).create(PetApi::class.java)
    fun matchApi(context: Context): MatchApi = getUserInstance(context).create(MatchApi::class.java)
    fun interactionApi(context: Context): InteractionApi = getUserInstance(context).create(InteractionApi::class.java)
    fun authApi(context: Context): AuthApi = getUserInstance(context).create(AuthApi::class.java)
    fun userApi(context: Context): UserApi = getUserInstance(context).create(UserApi::class.java)
    fun chatbotApi(context: Context): ChatbotApi = getUserInstance(context).create(ChatbotApi::class.java)

    fun adminAuthApi(context: Context): AuthApi = getAdminInstance(context).create(AuthApi::class.java)
    fun adminApi(context: Context): AdminApi = getAdminInstance(context).create(AdminApi::class.java)
    fun adminUserApi(context: Context): UserApi = getAdminInstance(context).create(UserApi::class.java)
}

