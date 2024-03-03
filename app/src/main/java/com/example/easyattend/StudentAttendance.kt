package com.example.easyattend

data class StudentAttendance(

    val studentName : String = "",
    val userId : String = "",
    val profilePictureUrl : String = "",
    var attendanceStatus : Boolean = false

) {
}