package com.mezcode.demo.roloscan.ocrreader

import android.app.Application
import android.content.ContentResolver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent

@HiltAndroidApp
class RoloScanApp : Application()

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideContentResolver(application: Application): ContentResolver {
        return application.contentResolver
    }
}