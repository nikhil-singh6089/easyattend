package com.example.easyattend

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.easyattend.databinding.ActivityStudentMainBinding
import com.example.easyattend.databinding.DialogBoxStudentAttendanceViewBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date

class StudentMainActivity : AppCompatActivity() {

    lateinit var  studentMainBinding : ActivityStudentMainBinding
    private lateinit var auth: FirebaseAuth

    private val myRef = Firebase.database.reference.child("Users")
    private val myRefAttendance = Firebase.database.reference.child("Attendance")
    private val myRefClass = Firebase.database.reference.child("Classes")
    private var imageUri : Uri? = null
    private var rollNumber : String? = null
    private var isRunning = true
    private var classNameOptions = ArrayList<String>()
    private var mapClassNameToId: HashMap<String, String> = hashMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        studentMainBinding = ActivityStudentMainBinding.inflate(layoutInflater)
        val view = studentMainBinding.root
        setContentView(view)
        auth = Firebase.auth
        setSupportActionBar(studentMainBinding.toolbarStudentMainPage)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = "Main Student Page"
        classNameOptions.clear()
        studentMainBinding.progressBarStudentImage.visibility= View.VISIBLE

        Thread {

            while (imageUri == null && isRunning) {

                val uuid : String? = intent.getStringExtra("uuid")
                if (uuid != null) {
                    getUserData(uuid)
                }else{
                    Toast.makeText(applicationContext, "error getting uuid", Toast.LENGTH_SHORT).show()
                    isRunning = false
                }
                Thread.sleep(1000) // Wait for 1 second (1000 milliseconds) before checking again
            }
            if (!isRunning) {
                println("Thread stopped")
                return@Thread
            }

            isRunning = false
        }.start()

        studentMainBinding.buttonAddUserAIImage.setOnClickListener(){

            val intent = Intent(this@StudentMainActivity ,StudentAiImageUploadActivity::class.java)
            intent.putExtra("RollNumber",rollNumber)
            startActivity(intent)

        }
        studentMainBinding.buttonCheckStudentAttendance.setOnClickListener(){

            showAttendancOfAClass()

        }

    }
//    adding menu items in toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.menu_items,menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if(item.itemId == R.id.mainMenuProfile){

            val intent = Intent(this@StudentMainActivity ,ProfileActivity::class.java)
            intent.putExtra("uuid",auth.currentUser?.uid.toString())
            startActivity(intent)

        }
        if(item.itemId == R.id.mainMenuLogOut){

            Firebase.auth.signOut()
            val intent =Intent(this@StudentMainActivity ,MainActivity::class.java)
            // Added flags to clear the activity stack and prevent the user from navigating back
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Start the login activity and finish the current activity
            startActivity(intent)
            finish()

        }

        return super.onOptionsItemSelected(item)
    }

    private fun getUserData(uuid: String) {
        val query: Query = myRef.orderByChild("uuid").equalTo(uuid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (userSnapshot in dataSnapshot.children) {
                        val user = userSnapshot.getValue(User::class.java)
                        val username = user?.userName
                        rollNumber = user?.rollNumber
                        rollNumber?.let { setClassNameOptions(it) }
                        studentMainBinding.textViewStudentName.text=username
                        imageUri = Uri.parse(user?.profilePictureUrl)
                        imageUri?.let {

                            Picasso.get().load(it).into(studentMainBinding.studentImageView)
                            studentMainBinding.progressBarStudentImage.visibility=View.INVISIBLE
                            Toast.makeText(applicationContext, "loading profile picture", Toast.LENGTH_SHORT).show()

                        }

                    }
                } else {
                    Toast.makeText(applicationContext, "uuid not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(applicationContext, databaseError.toException().toString(), Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun showAttendancOfAClass(){

        val dialogView = layoutInflater.inflate(R.layout.custom_dialog_todayattendance_layout, null)
        val textViewString = dialogView.findViewById<TextView>(R.id.textViewTAOTD)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerTakeAttendanceOfToday)
        val buttonOk = dialogView.findViewById<Button>(R.id.buttonOkTAOTD)
        val buttonNotOk = dialogView.findViewById<Button>(R.id.buttonNotOkTAOTD)
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val dateCreated = dateFormat.format(currentDate).toString().trim()

        val adapter = ArrayAdapter(this@StudentMainActivity, android.R.layout.simple_spinner_item, classNameOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val dialogMessage = AlertDialog.Builder(this)
        dialogMessage.setTitle("Today Attendance")
        dialogMessage.setMessage("Process made easy at my expense dont need hassle of calendar selection")
        dialogMessage.setView(dialogView)

        val dialog = dialogMessage.create()

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Handle selection here
                val selectedItem = classNameOptions[position]
                textViewString.text = selectedItem
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Another interface callback
                Toast.makeText(applicationContext, "please select some thing dont make my life harder", Toast.LENGTH_SHORT).show()
            }
        }

        buttonNotOk.setOnClickListener(){
            dialog.dismiss()
        }

        buttonOk.setOnClickListener(){

            if (dateCreated.isNotEmpty()) {
                val className = textViewString.text.toString()
                dialog.dismiss()
                val classId = mapClassNameToId[className].toString()
                rollNumber?.let { it1 ->
                    getAttendanceCountByClassIdAndStudentId(
                        classId, it1
                    ) { presentCount, absentCount ->
                        // Handle the attendance counts
//                        println("Present count: $presentCount, Absent count: $absentCount")
                        Toast.makeText(applicationContext, "Present count: $presentCount, Absent count: $absentCount", Toast.LENGTH_SHORT).show()
                        val dialogBinding = DialogBoxStudentAttendanceViewBinding.inflate(layoutInflater)
                        val dialogView = dialogBinding.root
                        val builder = AlertDialog.Builder(this)
                            .setView(dialogView)
                            .setTitle("Attendace of class")
                        val dialog = builder.create()
                        val totalDays = presentCount+absentCount
                        val percentage = (presentCount/totalDays)*100

                        dialogBinding.textViewStudentTotalCount.text = totalDays.toString()
                        dialogBinding.textViewStudentPresentCount.text = presentCount.toString()
                        dialogBinding.textViewStudentAbsentCount.text = absentCount.toString()
                        dialogBinding.textViewStudentAttendancePercentage.text = percentage.toString()

                        dialog.show()
                    }
                }

            } else {
                Toast.makeText(
                    applicationContext,
                    "date aur class to select karo",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }
        dialog.show()

    }
    fun getAttendanceCountByClassIdAndStudentId(
        classId: String,
        studentId: String,
        callback: (Int, Int) -> Unit
    ) {

        myRefAttendance.orderByChild("classId").equalTo(classId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var presentCount = 0
                    var absentCount = 0

                    for (attendanceSnapshot in snapshot.children) {
                        val attendance = attendanceSnapshot.getValue(Attendance::class.java)
                        if (attendance != null) {
                            val studentAttendanceList = attendance.studentAttendanceList
                            val studentAttendance = studentAttendanceList.find { it.userId == studentId }
                            if (studentAttendance != null) {
                                if (studentAttendance.attendanceStatus) {
                                    presentCount++
                                } else {
                                    absentCount++
                                }
                            } else {
                                absentCount++
                            }
                        }
                    }

                    callback(presentCount, absentCount)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun setClassNameOptions(userId: String) {
        val ref = myRefClass.orderByChild("studentList")
            .startAt(userId)
            .endAt("$userId\uf8ff")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (classSnapshot in snapshot.children) {
                    val classData = classSnapshot.getValue(Classes::class.java)
                    if (classData != null && classData.classStudents.contains(userId)) {
                        classNameOptions.add(classData.className)
                        mapClassNameToId[classData.className] = classData.classId
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
                Toast.makeText(applicationContext, error.toString(), Toast.LENGTH_SHORT).show()
            }
        })
    }

}