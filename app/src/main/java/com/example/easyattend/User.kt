package com.example.easyattend

data class User(val uuid : String = "",
                val userName : String = "",
                val rollNumber : String = "",
                val role : String = "",
                val attendancePictureUrl : String = "",
                val profilePictureUrl : String = "") {
}