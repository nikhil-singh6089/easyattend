package com.example.easyattend

import com.google.firebase.database.DataSnapshot
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

data class AttendanceData(
    val classId: String,
    val date: String,
    val classImageUrl: String,
    val className: String,
    val userName: String,
    val userId: String
){
    companion object {
        fun fromSnapshot(snapshot: DataSnapshot): AttendanceData? {
            return try {
                val classId = snapshot.child("classId").value.toString()
                val date = snapshot.child("date").value.toString()
                val classImageUrl = snapshot.child("classImageUrl").value.toString()
                val className = snapshot.child("className").value.toString()
                val userName = snapshot.child("userName").value.toString()
                val userId = snapshot.child("userId").value.toString()

                AttendanceData(classId, date, classImageUrl, className, userName, userId)
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class ResponseBody(
    val message : String
){}
interface EasyattendAPI {

    @POST
    fun sendAttendanceImageAndData(@Url url: String, @Body requestBody: AttendanceData): Call<ResponseBody>

}