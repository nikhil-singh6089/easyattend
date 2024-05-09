package com.example.easyattend

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.easyattend.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class MainActivity : AppCompatActivity() {

    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var auth : FirebaseAuth
    private val myRef = Firebase.database.reference.child("Users")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        val view = mainBinding.root
        auth = Firebase.auth
        setContentView(view)

        mainBinding.ButtonSignIn.setOnClickListener(){
            //did a judad
            //trim() kitna jaruri ha nikal ke deak lo
            val email = mainBinding.editTextEmail.text.toString().trim()
            val password = mainBinding.editTextPassword.text.toString().trim()

//            println("NoteId : ${email}")
//            println("UserName : ${password}")
            //best way to get hacked

            if(email.isNotEmpty() && password.isNotEmpty()){
                loginUser(email, password)
            }else{
                Toast.makeText(applicationContext, "please enter data in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        mainBinding.ButtonToSignUp.setOnClickListener(){

            val intent = Intent(this,SignUpActivity::class.java)
            startActivity(intent)

        }

    }

    private fun loginUser(email : String, password: String){

        auth.signInWithEmailAndPassword(email,password).addOnCompleteListener { task ->

            if(task.isSuccessful){

                Toast.makeText(applicationContext, "you are logged in moving to Main page", Toast.LENGTH_LONG).show()
                val uuid : String = auth.currentUser?.uid.toString()
                sendUserToActivity(uuid)

            }else{

                Toast.makeText(applicationContext, task.exception.toString(), Toast.LENGTH_LONG).show()

            }
        }
    }

    private fun sendUserToActivity(uuid: String) {
        val query: Query = myRef.orderByChild("uuid").equalTo(uuid)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (userSnapshot in dataSnapshot.children) {
                        val user = userSnapshot.getValue(User::class.java)
                        val userId = user?.rollNumber
                        val role = user?.role
                        if(role == "Teacher"){

                            val intent = Intent(this@MainActivity, FacultyMainActivity :: class.java)
                            intent.putExtra("userId", userId)
                            intent.putExtra("uuid", auth.currentUser?.uid.toString())
                            startActivity(intent)

                        }else{

                            val intent = Intent(this@MainActivity, StudentMainActivity :: class.java)
                            intent.putExtra("userId", userId)
                            intent.putExtra("uuid", auth.currentUser?.uid.toString())
                            startActivity(intent)
                        }

                    }
                } else {
                    Log.d("Firebase", "User with UUID $uuid not found")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Error getting user by UUID", databaseError.toException())
            }
        })
    }

    // For User Remenberance
    override fun onStart() {

        val user = auth.currentUser
        if( user != null){

            val uuid : String = auth.currentUser?.uid.toString()
            sendUserToActivity(uuid)

        }

        super.onStart()
    }

}