package com.example.easyattend

data class Attendance (
    val attendanceId : String = "",
    val date : String = "",
    val classId : String = "",
    val className : String = "",
    val studentAttendanceList : List<StudentAttendance> = emptyList(),
    val userName : String = "",
    val userId : String = ""){
}