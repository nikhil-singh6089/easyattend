package com.example.easyattend

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.widget.EditText
import android.os.Bundle
import android.os.Handler
import android.widget.LinearLayout
import android.widget.Toast
import com.example.easyattend.databinding.ActivityCreateClassBinding
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

class CreateClassActivity : AppCompatActivity() {

    private lateinit var createClassBinding : ActivityCreateClassBinding
    private val classStudents = ArrayList<String>()
    private val editTextsStudents = ArrayList<EditText>()
    private val myRef = Firebase.database.reference.child("Classes")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createClassBinding = ActivityCreateClassBinding.inflate(layoutInflater)
        val view = createClassBinding.root
        setContentView(view)
        setSupportActionBar(createClassBinding.toolbarCreateClass)
        classStudents.clear()
        val facultyName = intent.getStringExtra("UserName").toString().trim()
        val facultyId = intent.getStringExtra("UserId").toString().trim()
        val className = intent.getStringExtra("ClassName").toString().trim()
        val classSize = intent.getIntExtra("ClassSize", 0)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = className
        createEditTextView(classSize)

        createClassBinding.buttonCreateClassEvent.setOnClickListener(){

            if(!isAnyEditTextEmpty()){

                getAllEditTextStrings(object : MyCallback {

                    override fun onComplete() {
                        sendDataToClasses(className,classSize,facultyName,facultyId)
                    }

                })

            }else{
                Toast.makeText(applicationContext, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }

        }

    }

    private fun sendDataToClasses(className : String, classSize : Int,userName : String,userId : String){

        val currentDate = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val dateCreated : String = dateFormat.format(currentDate).trim()
        val classStrength: Int = classSize
        val facultyRollNumber : String = userId
        val classId : String = UUID.randomUUID().toString()

        val madelass = Classes(dateCreated,userName,className,facultyRollNumber,classStrength,classStudents,classId)

        myRef.child(classId).setValue(madelass).addOnCompleteListener {

            if(it.isSuccessful){
                Toast.makeText(applicationContext, "class details uploaded", Toast.LENGTH_SHORT).show()
                Handler().postDelayed({

                    val intent = Intent(this@CreateClassActivity, FacultyMainActivity::class.java)
                    startActivity(intent)

                },100)
            }else{
                Toast.makeText(applicationContext, it.exception.toString(), Toast.LENGTH_LONG).show()
            }

        }


    }

    interface MyCallback {

        fun onComplete()

    }

    private fun createEditTextView(noOfEditText : Int){

        val editTextContainer = findViewById<LinearLayout>(R.id.editTextStudentContainer)

        for (i in 1..noOfEditText) {
            val editText = EditText(this)
            val params = LinearLayout.LayoutParams(
                220.dpToPx(), 75.dpToPx()
            )
            params.setMargins(0, 5.dpToPx(), 0, 5.dpToPx()) // Convert dp to pixels
            editText.layoutParams = params
            editText.hint = "Student $i"
            editTextsStudents.add(editText)
            editTextContainer.addView(editText)
        }

    }

    private fun getAllEditTextStrings(callback : MyCallback){

        createClassBinding.buttonCreateClassEvent.isClickable = false //protect rewrites

        for (editText in editTextsStudents) {
            classStudents.add(editText.text.toString())
        }
        callback.onComplete()
    }
    private fun isAnyEditTextEmpty(): Boolean {
        for (editText in editTextsStudents) {
            if (editText.text.isEmpty()) {
                return true
            }
        }
        return false
    }

    // Extension function to convert dp to pixels
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}