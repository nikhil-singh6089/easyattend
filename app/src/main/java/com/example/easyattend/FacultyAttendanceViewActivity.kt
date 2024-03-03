package com.example.easyattend

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.easyattend.databinding.ActivityFacultyAttendanceViewBinding

class FacultyAttendanceViewActivity : AppCompatActivity() {

    lateinit var facultyAttendanceViewbinding : ActivityFacultyAttendanceViewBinding

    private var userId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        facultyAttendanceViewbinding = ActivityFacultyAttendanceViewBinding.inflate(layoutInflater)
        val view = facultyAttendanceViewbinding.root
        setContentView(view)

        userId = intent.getStringExtra("userId").toString()
        var className = intent.getStringExtra("ClassName").toString()
        var userName = intent.getStringExtra("UserName").toString()
        var date = intent.getStringExtra("Date").toString()

        println({className})
        println({userName})
        println({date})

    }
}