package com.example.easyattend

data class Attendance (
    val uuid : String = "",
    val Date : String = "",
    val classId : String = "",
    val className : String = "",
    val studentList : List<StudentAttendance> = emptyList(),
    val facultyName : String = "",
    val facultyRollNumber : String = ""){
}