package com.example.easyattend

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.easyattend.databinding.ActivityFacultyAttendanceViewBinding
import com.google.android.play.integrity.internal.f
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.io.FileOutputStream
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File

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
        val className = intent.getStringExtra("ClassName").toString()
        val classId = intent.getStringExtra("ClassId").toString()
        val date = intent.getStringExtra("Date").toString().trim()

        facultyAttendanceViewbinding.textViewClassNameViewSA.text = className
        facultyAttendanceViewbinding.textViewDateViewSA.text = date

        getAttendanceList(classId,date)
        facultyAttendanceViewbinding.ButtontakeExcel.setOnClickListener(){

            generateExcelSheet(date ,classId ,className ,studentAttendanceList)

        }

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
    private fun generateExcelSheet(
        date: String,
        classId: String,
        className: String,
        studentAttendanceList: List<StudentAttendance>
    ) {
        // Create a new workbook
        val workbook = XSSFWorkbook()

        // Create a new sheet
        val sheet = workbook.createSheet("Attendance")

        // Create a row for headers
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Date")
        headerRow.createCell(1).setCellValue("Class ID")
        headerRow.createCell(2).setCellValue("Class Name")
        headerRow.createCell(3).setCellValue("Student Name")
        headerRow.createCell(4).setCellValue("User ID")
        headerRow.createCell(5).setCellValue("Profile Picture URL")
        headerRow.createCell(6).setCellValue("Attendance Status")

        // Add class information to the first row
        sheet.getRow(0).createCell(0).setCellValue(date)
        sheet.getRow(0).createCell(1).setCellValue(classId)
        sheet.getRow(0).createCell(2).setCellValue(className)

        // Add student attendance data to the sheet
        for ((index, student) in studentAttendanceList.withIndex()) {
            val row = sheet.createRow(index + 1)
            row.createCell(3).setCellValue(student.studentName)
            row.createCell(4).setCellValue(student.userId)
            row.createCell(5).setCellValue(student.profilePictureUrl)
            row.createCell(6).setCellValue(student.attendanceStatus)
        }

        // Write the workbook to a file
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val fileName : String = "attendance_${date}_${className}.xlsx"
        val excelFile = File(documentsDir, fileName)
        val outputStream = FileOutputStream(excelFile)
        workbook.write(outputStream)
        outputStream.close()
        workbook.close()
    }
}