package com.example.hoode_app.data.remote

import com.hoodeconnect.backend.MediaUploader

object BackblazeClient {
    suspend fun uploadImage(fileBytes: ByteArray, fileName: String, mimeType: String = "image/jpeg"): Result<String> =
        MediaUploader.upload(fileBytes, fileName, mimeType)
}
