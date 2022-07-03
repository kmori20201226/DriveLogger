package com.kmoriproj.drivelogger.server_interaction

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface DriveLogUploadService {

    @POST("upload")
    fun upload(@Body body:UploadBody): Call<ResponseBody>
}