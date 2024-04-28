package com.example.easyattend

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.example.easyattend.databinding.ActivityStudentMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso

class StudentMainActivity : AppCompatActivity() {

    lateinit var  studentMainBinding : ActivityStudentMainBinding
    private lateinit var auth: FirebaseAuth

    private val myRef = Firebase.database.reference.child("Users")
    private var imageUri : Uri? = null
    private var rollNumber : String? = null
    private var isRunning = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        studentMainBinding = ActivityStudentMainBinding.inflate(layoutInflater)
        val view = studentMainBinding.root
        setContentView(view)
        auth = Firebase.auth
        setSupportActionBar(studentMainBinding.toolbarStudentMainPage)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = "Main Student Page"
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

}