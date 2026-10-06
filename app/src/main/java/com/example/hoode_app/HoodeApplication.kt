package com.example.hoode_app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.hoodeconnect.backend.BackendSession
import okhttp3.OkHttpClient

class HoodeApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        BackendSession.initialize(this, BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)
        com.example.hoode_app.data.repository.HoodeRepository.init(this)
    }
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient(OkHttpClient.Builder().addInterceptor(BackendSession.interceptor).build()).build()
}
