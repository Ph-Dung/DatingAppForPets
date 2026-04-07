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

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val token = runBlocking {
            context.dataStore.data.map { prefs ->
                prefs[stringPreferencesKey(Constants.TOKEN_KEY)]
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
    private var retrofit: Retrofit? = null

    fun getInstance(context: Context): Retrofit {
        if (retrofit == null) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(context))
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }

    fun petApi(context: Context): PetApi = getInstance(context).create(PetApi::class.java)
    fun matchApi(context: Context): MatchApi = getInstance(context).create(MatchApi::class.java)
    fun interactionApi(context: Context): InteractionApi = getInstance(context).create(InteractionApi::class.java)
    fun authApi(context: Context): AuthApi = getInstance(context).create(AuthApi::class.java)
    fun userApi(context: Context): UserApi = getInstance(context).create(UserApi::class.java)
    fun chatbotApi(context: Context): ChatbotApi = getInstance(context).create(ChatbotApi::class.java)
    fun chatApi(context: Context): ChatApi = getInstance(context).create(ChatApi::class.java)
    fun callApi(context: Context): CallApi = getInstance(context).create(CallApi::class.java)
    fun appointmentApi(context: Context): AppointmentApi = getInstance(context).create(AppointmentApi::class.java)
    fun reviewApi(context: Context): ReviewApi = getInstance(context).create(ReviewApi::class.java)
    fun groupChatApi(context: Context): GroupChatApi = getInstance(context).create(GroupChatApi::class.java)
}

