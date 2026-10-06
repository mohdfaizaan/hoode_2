package com.example.hoode_app.ui.common

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoodeconnect.backend.CommunityApi
import com.hoodeconnect.backend.MediaUploader
import com.hoodeconnect.backend.SubmissionAttempt
import com.hoodeconnect.backend.SubmissionErrors
import com.hoodeconnect.backend.UploadStage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

data class PostFormState(val busy:Boolean=false,val message:String="",val completed:Boolean=false,val photo:String?=null)

class CommunityPostViewModel(private val saved:SavedStateHandle):ViewModel() {
    var title:String
        get()=saved["title"] ?: ""
        set(value){saved["title"]=value}
    var body:String
        get()=saved["body"] ?: ""
        set(value){saved["body"]=value}
    private var uploaded:String
        get()=saved["uploaded"] ?: ""
        set(value){saved["uploaded"]=value}
    private val attempt=SubmissionAttempt(saved.get<String>("requestId") ?: UUID.randomUUID().toString().also{saved["requestId"]=it})
    private val mutableState=MutableStateFlow(PostFormState(photo=saved["photo"]))
    val state=mutableState.asStateFlow()
    private var job:Job?=null

    fun selectPhoto(uri:Uri) {
        if(state.value.busy)return
        saved["photo"]=uri.toString();uploaded=""
        mutableState.value=PostFormState(photo=uri.toString())
    }

    fun submit(gallery:Boolean,photographer:String) {
        if(state.value.busy || state.value.completed)return
        val snapshot=state.value
        mutableState.value=snapshot.copy(busy=true,message="Preparing submission…")
        job=viewModelScope.launch(attempt) {
            try {
                if(uploaded.isBlank() && snapshot.photo!=null) {
                    uploaded=MediaUploader.uploadUri(Uri.parse(snapshot.photo)) { progress->
                        if(isActive)mutableState.value=state.value.copy(message=when(progress.stage) {
                            UploadStage.PREPARING -> "Preparing photo…"
                            UploadStage.AUTHORIZING -> "Connecting to photo storage…"
                            UploadStage.UPLOADING -> "Uploading photo: ${progress.percent}%"
                        })
                    }.getOrThrow()
                }
                mutableState.value=state.value.copy(message="Saving your submission…")
                val payload=if(gallery)JSONObject().put("caption",body.trim()).put("photographer",photographer.ifBlank{"Resident"})
                    else JSONObject().put("description",body.trim())
                CommunityApi.submit(if(gallery)"gallery" else "community_update",title.trim(),payload,uploaded,attempt.id).getOrThrow()
                mutableState.value=state.value.copy(busy=false,completed=true,message="Pending")
            } catch(cancelled:CancellationException) {
                mutableState.value=state.value.copy(busy=false,message="Stopped waiting. Your draft is kept. Check Profile → Activity or tap Submit again to confirm safely.")
                throw cancelled
            } catch(error:Exception) {
                mutableState.value=state.value.copy(busy=false,message=SubmissionErrors.message(error))
            }
        }
    }

    fun cancel(){job?.cancel()}
}
