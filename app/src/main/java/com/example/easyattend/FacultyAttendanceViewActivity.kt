package com.example.easyattend

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.easyattend.databinding.ActivityFacultyAttendanceViewBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class FacultyAttendanceViewActivity : AppCompatActivity() {

    lateinit var facultyAttendanceViewbinding : ActivityFacultyAttendanceViewBinding
    private var studentAttendanceList: ArrayList<StudentAttendance> = arrayListOf()
    private var userId = ""
    private val myRefAttendance = Firebase.database.reference.child("Attendance")
    private lateinit var viewAttendanceAdapter: ViewAttendanceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        facultyAttendanceViewbinding = ActivityFacultyAttendanceViewBinding.inflate(layoutInflater)
        val view = facultyAttendanceViewbinding.root
        setContentView(view)

        userId = intent.getStringExtra("userId").toString()
        var className = intent.getStringExtra("ClassName").toString()
        val classId = intent.getStringExtra("ClassId").toString()
        var date = intent.getStringExtra("Date").toString().trim()

        facultyAttendanceViewbinding.textViewClassNameViewSA.text = className
        facultyAttendanceViewbinding.textViewDateViewSA.text = date

        getAttendanceList(classId,date)

    }

    private fun getAttendanceList(classId : String,date : String){

        val ref = myRefAttendance.orderByChild("classId").equalTo(classId)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (classSnapshot in snapshot.children){

                    val attendanceData = classSnapshot.getValue(Attendance::class.java)
                    val validateDate = attendanceData?.date?.trim()
                    if (attendanceData != null && date == validateDate ) {
                        studentAttendanceList =
                            attendanceData.studentAttendanceList as ArrayList<StudentAttendance>
                        updateRecycleViewer()
                        return

                    }else{
                        Toast.makeText(applicationContext, "attendance List empty", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, "${error.code}"+error.details+error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun updateRecycleViewer(){

        viewAttendanceAdapter = ViewAttendanceAdapter(this@FacultyAttendanceViewActivity,studentAttendanceList)
        facultyAttendanceViewbinding.RecyclerViewVEachDA.layoutManager =LinearLayoutManager(this@FacultyAttendanceViewActivity)
        facultyAttendanceViewbinding.RecyclerViewVEachDA.adapter = viewAttendanceAdapter

    }
}