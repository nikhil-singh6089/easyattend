package com.example.easyattend

data class Classes(val dateCreated : String = "",
                   val userName : String = "",
                   val className : String = "",
                   val facultyRollNumber : String = "",
                   val classStrength : Int = 0,
                   val classStudents : List<String> = emptyList(),
                   val classId : String = "") {
}