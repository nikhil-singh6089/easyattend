package com.example.easyattend

import android.app.DatePickerDialog
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
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.easyattend.databinding.ActivityFacultyMainBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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
import java.util.Calendar
import java.util.Date

class FacultyMainActivity : AppCompatActivity() {

    private lateinit var facultyMainBinding : ActivityFacultyMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var userId : String

    private val myRef = Firebase.database.reference.child("Users")
    private val myRefClass = Firebase.database.reference.child("Classes")
    private var imageUri : Uri? = null
    private var isRunning = true
    private var classNameOptions = ArrayList<String>()
    private var mapClassNameToId: HashMap<String, String> = hashMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        facultyMainBinding = ActivityFacultyMainBinding.inflate(layoutInflater)
        val view = facultyMainBinding.root
        auth = Firebase.auth
        setContentView(view)
        setSupportActionBar(facultyMainBinding.toolbarFaculty)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = "Main Faculty Page"
        classNameOptions.clear() // to clear arrayList after create class Process
        facultyMainBinding.progressBarFacultyImage.visibility=View.VISIBLE
        userId = intent.getStringExtra("userId").toString()


        //deal with delay of database
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

            facultyMainBinding.buttonCreateClass.setOnClickListener(){

            createClassDialogBox()

        }
        facultyMainBinding.buttonCheckAttendance.setOnClickListener(){

            attendanceDialogBox()

        }
        facultyMainBinding.buttonAiAttendence.setOnClickListener() {

            takeAttendanceOfADayAIDialogBox()

        }

    }
// something i am exploring
//    override fun onStart() {
//
//        val user = auth.currentUser
//        if( user == null){
//
//            val intent = Intent(this@FacultyMainActivity,MainActivity::class.java)
//            startActivity(intent)
//
//        }
//
//        super.onStart()
//    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.menu_items,menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if(item.itemId == R.id.mainMenuProfile){

            val intent = Intent(this,ProfileActivity::class.java)
            intent.putExtra("uuid",auth.currentUser?.uid.toString())
            startActivity(intent)

        }
        if(item.itemId == R.id.mainMenuLogOut){

            Firebase.auth.signOut()
            val intent = Intent(this,MainActivity :: class.java)
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
                        facultyMainBinding.textViewName.text = username
                        userId = user?.rollNumber.toString()
                        imageUri = Uri.parse(user?.profilePictureUrl)
                        imageUri?.let {

                            Picasso.get().load(it).into(facultyMainBinding.facultyImageView)
                            facultyMainBinding.progressBarFacultyImage.visibility=View.INVISIBLE
                            Toast.makeText(applicationContext, "loading profile picture", Toast.LENGTH_SHORT).show()

                        }
                        //setting faculty classes
                        if(classNameOptions.isEmpty()){
                            setClassNameOptions(userId)
                        }else{
                            classNameOptions.clear()
                            setClassNameOptions(userId)
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

    private fun createClassDialogBox(){

        val dialogView = layoutInflater.inflate(R.layout.custom_dialog_createclass_layout, null)
        val editTextString = dialogView.findViewById<EditText>(R.id.editTextDialogClassName)
        val editTextInt = dialogView.findViewById<EditText>(R.id.editTextDialogClassSize)

        val dialogMessage = AlertDialog.Builder(this)
        dialogMessage.setTitle("Create a class")
        dialogMessage.setMessage("Class Names are unique please try to keep them unique")
        dialogMessage.setView(dialogView)

        dialogMessage.setNegativeButton("cancel") { dialogInterface, _ ->
            dialogInterface.cancel()
        }

        dialogMessage.setPositiveButton("yes") { dialogInterface, _ ->
            val stringValue = editTextString.text.toString()
            val intValue = editTextInt.text.toString().toInt()

            val intent = Intent(this, CreateClassActivity::class.java)
            intent.putExtra("ClassName", stringValue)
            intent.putExtra("ClassSize", intValue)
            intent.putExtra("UserName", facultyMainBinding.textViewName.text.toString())
            intent.putExtra("UserId", userId )
            startActivity(intent)
        }

        dialogMessage.create().show()

    }

    private fun viewAttendanceDialogBox(){

        val dialogView = layoutInflater.inflate(R.layout.custom_dialog_viewattendance_layout, null)
        val textViewString = dialogView.findViewById<TextView>(R.id.textViewVAOAD)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerViewAttendanceOfADay)
        val textInputLayout = dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutVAOAD)
        val textInputEditText = dialogView.findViewById<TextInputEditText>(R.id.editTextDateVAOAD)
        val buttonOk = dialogView.findViewById<Button>(R.id.buttonOkVAOAD)
        val buttonNotOk = dialogView.findViewById<Button>(R.id.buttonNotOkVAOAD)

        val adapter = ArrayAdapter(this@FacultyMainActivity, android.R.layout.simple_spinner_item, classNameOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val dialogMessage = AlertDialog.Builder(this)
        dialogMessage.setTitle("To View Attendance Of A Day")
        dialogMessage.setMessage("Process made easy at my expense")
        dialogMessage.setView(dialogView)

        val dialog = dialogMessage.create()

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Handle selection here
                val selectedItem = classNameOptions[position]
                textViewString.setText(selectedItem)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Another interface callback
                Toast.makeText(applicationContext, "please select some thing dont make my life harder", Toast.LENGTH_SHORT).show()
            }
        }

        textInputLayout.setEndIconOnClickListener() {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this@FacultyMainActivity,
                { _: DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                    val formattedMonth = String.format("%02d", monthOfYear + 1) // Add leading zero if necessary
                    val formattedDay = String.format("%02d", dayOfMonth) // Add leading zero if necessary
                    textInputEditText.setText("$year-$formattedMonth-$formattedDay")
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }

        buttonNotOk.setOnClickListener(){

            dialog.dismiss()

        }
        buttonOk.setOnClickListener(){

            if (textInputEditText.text.toString()
                    .isNotEmpty() && textInputEditText.text.toString().isNotEmpty()
            ) {
                val date = textInputEditText.text.toString().trim()
                val className = textViewString.text.toString()

                val intent = Intent(this@FacultyMainActivity, FacultyAttendanceViewActivity::class.java)
                intent.putExtra("ClassName", className)
                intent.putExtra("ClassId",mapClassNameToId[className])
                intent.putExtra("Date", date)
                intent.putExtra("UserName", facultyMainBinding.textViewName.text.toString())
                intent.putExtra("UserId", userId)
                dialog.dismiss()
                startActivity(intent)
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

    private fun takeAttendanceOfADayDialogBox(){

        val dialogView = layoutInflater.inflate(R.layout.custom_dialog_manualattendanceofaday_layout, null)
        val textViewString = dialogView.findViewById<TextView>(R.id.textViewMAOAD)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerManualAttendanceOfADay)
        val textInputLayout = dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutMAOAD)
        val textInputEditText = dialogView.findViewById<TextInputEditText>(R.id.editTextDateMAOAD)
        val buttonOk = dialogView.findViewById<Button>(R.id.buttonOkMAOAD)
        val buttonNotOk = dialogView.findViewById<Button>(R.id.buttonNotOkMAOAD)

        val adapter = ArrayAdapter(this@FacultyMainActivity, android.R.layout.simple_spinner_item, classNameOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val dialogMessage = AlertDialog.Builder(this)
        dialogMessage.setTitle("To take Attendance Of Any Day")
        dialogMessage.setMessage("advance Functionality")
        dialogMessage.setView(dialogView)

        val dialog = dialogMessage.create()

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Handle selection here
                val selectedItem = classNameOptions[position]
                textViewString.setText(selectedItem)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Another interface callback
                Toast.makeText(applicationContext, "please select some thing dont make my life harder", Toast.LENGTH_SHORT).show()
            }
        }

        textInputLayout.setEndIconOnClickListener() {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this@FacultyMainActivity,
                { _: DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                    val formattedMonth = String.format("%02d", monthOfYear + 1) // Add leading zero if necessary
                    val formattedDay = String.format("%02d", dayOfMonth) // Add leading zero if necessary
                    textInputEditText.setText("$year-$formattedMonth-$formattedDay")
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }

        buttonNotOk.setOnClickListener(){

            dialog.dismiss()

        }
        buttonOk.setOnClickListener(){

            if (textInputEditText.text.toString()
                    .isNotEmpty() && textInputEditText.text.toString().isNotEmpty()
            ) {
                val date = textInputEditText.text.toString()
                val className = textViewString.text.toString()

                val intent = Intent(this@FacultyMainActivity, FacultyTakeAttendanceActivity::class.java)
                intent.putExtra("ClassName", className)
                intent.putExtra("ClassId",mapClassNameToId[className])
                intent.putExtra("Date", date)
                intent.putExtra("UserName", facultyMainBinding.textViewName.text.toString())
                intent.putExtra("UserId", userId)
                intent.putExtra("CheckActivity","new")
                dialog.dismiss()
                startActivity(intent)
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

    private fun updateAttendanceOfADayDialogBox(){

        val dialogView = layoutInflater.inflate(R.layout.custom_dialog_updateattendanceofaday_layout, null)
        val textViewString = dialogView.findViewById<TextView>(R.id.textViewUAOAD)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerUpdateAttendanceOfADay)
        val textInputLayout = dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutUAOAD)
        val textInputEditText = dialogView.findViewById<TextInputEditText>(R.id.editTextDateUAOAD)
        val buttonOk = dialogView.findViewById<Button>(R.id.buttonOkUAOAD)
        val buttonNotOk = dialogView.findViewById<Button>(R.id.buttonNotOkUAOAD)

        val adapter = ArrayAdapter(this@FacultyMainActivity, android.R.layout.simple_spinner_item, classNameOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val dialogMessage = AlertDialog.Builder(this)
        dialogMessage.setTitle("Update Attendance Of Any Day")
        dialogMessage.setMessage("advance Functionality")
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

        textInputLayout.setEndIconOnClickListener() {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this@FacultyMainActivity,
                { _: DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                    val formattedMonth = String.format("%02d", monthOfYear + 1) // Add leading zero if necessary
                    val formattedDay = String.format("%02d", dayOfMonth) // Add leading zero if necessary
                    textInputEditText.setText("$year-$formattedMonth-$formattedDay")
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }

        buttonNotOk.setOnClickListener(){

            dialog.dismiss()

        }
        buttonOk.setOnClickListener(){

            if (textInputEditText.text.toString()
                    .isNotEmpty() && textInputEditText.text.toString().isNotEmpty()
            ) {
                val date = textInputEditText.text.toString()
                val className = textViewString.text.toString()

                val intent = Intent(this@FacultyMainActivity, FacultyTakeAttendanceActivity::class.java)
                intent.putExtra("ClassName", className)
                intent.putExtra("ClassId",mapClassNameToId[className])
                intent.putExtra("Date", date)
                intent.putExtra("UserName", facultyMainBinding.textViewName.text.toString())
                intent.putExtra("UserId", userId)
                intent.putExtra("CheckActivity","update".trim())
                dialog.dismiss()
                startActivity(intent)
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

    private fun takeAttendanceOfTodayDialogBox(){

        val dialogView = layoutInflater.inflate(R.layout.custom_dialog_todayattendance_layout, null)
        val textViewString = dialogView.findViewById<TextView>(R.id.textViewTAOTD)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerTakeAttendanceOfToday)
        val buttonOk = dialogView.findViewById<Button>(R.id.buttonOkTAOTD)
        val buttonNotOk = dialogView.findViewById<Button>(R.id.buttonNotOkTAOTD)
        val currentDate = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val dateCreated = dateFormat.format(currentDate).toString().trim()

        val adapter = ArrayAdapter(this@FacultyMainActivity, android.R.layout.simple_spinner_item, classNameOptions)
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
                val intent = Intent(this@FacultyMainActivity, FacultyTakeAttendanceActivity::class.java)
                intent.putExtra("ClassName", className)
                intent.putExtra("Date", dateCreated)
                intent.putExtra("UserName", facultyMainBinding.textViewName.text)
                intent.putExtra("UserId", userId)
                intent.putExtra("ClassId",mapClassNameToId[className].toString())
                startActivity(intent)
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

    private fun attendanceDialogBox(){

        val dialogView = layoutInflater.inflate(R.layout.custom_dialog_attendance_layout, null)
        val buttonTAOTD = dialogView.findViewById<Button>(R.id.buttonTAOTD)
        val buttonUAOAD = dialogView.findViewById<Button>(R.id.buttonUAOAD)
        val buttonTAOAD = dialogView.findViewById<Button>(R.id.buttonTAOAD)
        val buttonVAOAD = dialogView.findViewById<Button>(R.id.buttonVAOAD)

        val dialogMessage = AlertDialog.Builder(this)
        dialogMessage.setTitle("Attendance")
        dialogMessage.setMessage("Chose from any options")
        dialogMessage.setView(dialogView)

        val dialog = dialogMessage.create() // ganta ka debug

        buttonVAOAD.setOnClickListener(){
            dialog.dismiss()
            viewAttendanceDialogBox()
        }
        buttonTAOTD.setOnClickListener(){
            dialog.dismiss()
            takeAttendanceOfTodayDialogBox()
        }
        buttonTAOAD.setOnClickListener(){
            dialog.dismiss()
            takeAttendanceOfADayDialogBox()
        }
        buttonUAOAD.setOnClickListener(){
            dialog.dismiss()
            updateAttendanceOfADayDialogBox()
        }
        dialog.show()
    }

    private fun takeAttendanceOfADayAIDialogBox(){

        val dialogView = layoutInflater.inflate(R.layout.custom_dialog_manualattendanceofaday_layout, null)
        val textViewString = dialogView.findViewById<TextView>(R.id.textViewMAOAD)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerManualAttendanceOfADay)
        val textInputLayout = dialogView.findViewById<TextInputLayout>(R.id.textInputLayoutMAOAD)
        val textInputEditText = dialogView.findViewById<TextInputEditText>(R.id.editTextDateMAOAD)
        val buttonOk = dialogView.findViewById<Button>(R.id.buttonOkMAOAD)
        val buttonNotOk = dialogView.findViewById<Button>(R.id.buttonNotOkMAOAD)

        val adapter = ArrayAdapter(this@FacultyMainActivity, android.R.layout.simple_spinner_item, classNameOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val dialogMessage = AlertDialog.Builder(this)
        dialogMessage.setTitle("To take Attendance Of Any Day")
        dialogMessage.setMessage("advance Functionality")
        dialogMessage.setView(dialogView)

        val dialog = dialogMessage.create()

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Handle selection here
                val selectedItem = classNameOptions[position]
                textViewString.setText(selectedItem)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Another interface callback
                Toast.makeText(applicationContext, "please select some thing dont make my life harder", Toast.LENGTH_SHORT).show()
            }
        }

        textInputLayout.setEndIconOnClickListener() {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this@FacultyMainActivity,
                { _: DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int ->
                    val formattedMonth = String.format("%02d", monthOfYear + 1) // Add leading zero if necessary
                    val formattedDay = String.format("%02d", dayOfMonth) // Add leading zero if necessary
                    textInputEditText.setText("$year-$formattedMonth-$formattedDay")
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }

        buttonNotOk.setOnClickListener(){

            dialog.dismiss()

        }
        buttonOk.setOnClickListener(){

            if (textInputEditText.text.toString()
                    .isNotEmpty() && textInputEditText.text.toString().isNotEmpty()
            ) {
                val date = textInputEditText.text.toString()
                val className = textViewString.text.toString()

                val intent = Intent(this@FacultyMainActivity, AiAttendanceActivity::class.java)
                intent.putExtra("ClassName", className)
                intent.putExtra("ClassId",mapClassNameToId[className])
                intent.putExtra("Date", date)
                intent.putExtra("UserName", facultyMainBinding.textViewName.text.toString())
                intent.putExtra("UserId", userId)
                dialog.dismiss()
                startActivity(intent)
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

    private fun setClassNameOptions(userId : String){
        val ref = myRefClass.orderByChild("facultyRollNumber").equalTo(userId)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (classSnapshot in snapshot.children) {
                    val classData = classSnapshot.getValue(Classes::class.java)
                    if (classData != null) {
                        classNameOptions.add(classData.className)
                        mapClassNameToId[classData.className]= classData.classId
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