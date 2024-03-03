package com.example.easyattend

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.easyattend.databinding.ActivityFacultyTakeAttendanceBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.UUID

class FacultyTakeAttendanceActivity : AppCompatActivity() {

    private lateinit var facultyAttendanceTakeBinding: ActivityFacultyTakeAttendanceBinding
    private var userId : String = ""
    private var studentAttendanceList: ArrayList<StudentAttendance> = arrayListOf()
    private val myRefClass = Firebase.database.reference.child("Classes")
    private val myRefUser = Firebase.database.reference.child("Users")
    private val myRefAttendance = Firebase.database.reference.child("Attendance")
    private lateinit var takeAttendanceAdapter: TakeAttendanceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        facultyAttendanceTakeBinding =  ActivityFacultyTakeAttendanceBinding.inflate(layoutInflater)
        val view = facultyAttendanceTakeBinding.root
        setContentView(view)

        userId = intent.getStringExtra("userId").toString()
        var className = intent.getStringExtra("ClassName").toString()
        val classId = intent.getStringExtra("ClassId").toString()
        var userName = intent.getStringExtra("UserName").toString()
        var date = intent.getStringExtra("Date").toString()

        facultyAttendanceTakeBinding.textViewAttendanceDate.text = date
        facultyAttendanceTakeBinding.textviewFTAClassName.text = className

        populateStudentAttendanceList(classId)

        facultyAttendanceTakeBinding.buttonFacultyMakePresent.setOnClickListener(){

            takeAttendanceAdapter.makePresent()

        }
        facultyAttendanceTakeBinding.buttonFacultyMakeAbsent.setOnClickListener(){
            takeAttendanceAdapter.makeAbsent()
        }

        facultyAttendanceTakeBinding.buttonFacultyTakeAttendance.setOnClickListener(){

            takeAttendance(classId,className,userName,date)

        }

    }

    private fun populateStudentAttendanceList(classId : String){

        val ref = myRefClass.orderByChild("classId").equalTo(classId)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (classSnapshot in snapshot.children) {
                    val classData = classSnapshot.getValue(Classes::class.java)
                    if (classData != null) {
                        val studentList : List<String> = classData.classStudents
                        for(student in studentList ){

                            val newRef = myRefUser.orderByChild("rollNumber").equalTo(student)
                            newRef.addListenerForSingleValueEvent(object : ValueEventListener{

                                override fun onDataChange(snapshot: DataSnapshot) {
                                    for(classSnapshot in snapshot.children){

                                        val userData = classSnapshot.getValue(User::class.java)
                                        val studentName = userData?.userName.toString()
                                        val userId = userData?.rollNumber.toString()
                                        val userProfileImage = userData?.profilePictureUrl.toString()
                                        val attendanceStatus : Boolean = false

                                        val userAttendance = StudentAttendance(studentName,userId,userProfileImage,attendanceStatus)

                                        studentAttendanceList.add(userAttendance)

                                    }

                                    takeAttendanceAdapter = TakeAttendanceAdapter(this@FacultyTakeAttendanceActivity,studentAttendanceList)

                                    facultyAttendanceTakeBinding.RecyclerViewTakeAttendance.layoutManager = LinearLayoutManager(this@FacultyTakeAttendanceActivity)

                                    facultyAttendanceTakeBinding.RecyclerViewTakeAttendance.adapter = takeAttendanceAdapter

                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(applicationContext, "${error.code}"+error.details+error.message, Toast.LENGTH_SHORT).show()
                                }

                            })
                        }
                    }else{
                        Toast.makeText(applicationContext, "no data found", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
                Toast.makeText(applicationContext, "${error.code}"+error.details+error.message, Toast.LENGTH_SHORT).show()
            }
        })

    }

    private fun takeAttendance(classId : String,className : String,userName : String,date : String){

        val attendanceId : String = UUID.randomUUID().toString()
        val attendance = Attendance(attendanceId,date,classId,className,studentAttendanceList,userName,userId)

        myRefAttendance.child(attendanceId).setValue(attendance).addOnCompleteListener(){

            if(it.isSuccessful) {
                Toast.makeText(applicationContext, "attendance added", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(applicationContext, it.exception.toString(), Toast.LENGTH_SHORT).show()
            }
        }


    }
}
