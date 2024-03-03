package com.example.easyattend

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.easyattend.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso


class SignUpActivity : AppCompatActivity() {

    private lateinit var signUpBinding : ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private val myRef = Firebase.database.reference.child("Users")
    private lateinit var activityResultLauncher : ActivityResultLauncher<Intent>

    private var imageUri : Uri? = null
    private var storage : FirebaseStorage = FirebaseStorage.getInstance()
    private var storageRef : StorageReference = storage.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        signUpBinding = ActivitySignUpBinding.inflate(layoutInflater)
        val view = signUpBinding.root
        setContentView(view)
        registerActivityResultLauncher()
        auth = Firebase.auth

        signUpBinding.newUserSignUpButton.setOnClickListener(){

            val userEmail = signUpBinding.editTextUserEmail.text.toString()
            val userPassword = signUpBinding.signInPasswordEditTextTextPassword.text.toString()
            val userPasswordCheck = signUpBinding.signInPasswordCheckEditTextTextPassword.text.toString()
            if(userPassword == userPasswordCheck){

                signUpUser(userEmail,userPassword, object : MyCallback {

                    override fun onComplete(){

                        if( signUpBinding.radioButtonAsATeacher.isChecked){
                            Handler().postDelayed({
                            val intent = Intent(this@SignUpActivity, FacultyMainActivity::class.java)
                            intent.putExtra("uuid", auth.currentUser?.uid.toString());
                            startActivity(intent)},2000)
                        }else{
                            Handler().postDelayed({
                            val intent = Intent(this@SignUpActivity, StudentMainActivity::class.java)
                            intent.putExtra("uuid", auth.currentUser?.uid.toString());
                            startActivity(intent)},2000)

                        }
                    }

                })

            }else{
                Toast.makeText(applicationContext, "check password", Toast.LENGTH_SHORT).show()
            }

        }

        signUpBinding.signUpImageView.setOnClickListener(){

            choseImage()

        }


    }

    interface MyCallback {

        fun onComplete()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            val intent =Intent()
            intent.type= "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            activityResultLauncher.launch(intent)
        }

    }

    private fun signUpUser(email: String, password: String, callback: MyCallback){

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    Toast.makeText(applicationContext, "User created", Toast.LENGTH_LONG).show()
                    val userEmail = signUpBinding.editTextUserEmail.text.toString()
                    val userPassword = signUpBinding.signInPasswordEditTextTextPassword.text.toString()
                    if(auth.currentUser?.uid == null) {
                        loginUser(userEmail, userPassword)

                    }else{
                        Firebase.auth.signOut()
                        loginUser(userEmail, userPassword)
                    }
                    callback.onComplete()

                } else {

                    Toast.makeText(applicationContext, task.exception.toString(), Toast.LENGTH_SHORT).show()

                }
            }
    }

    @Synchronized
    private fun loginUser(email : String, password: String){

        auth.signInWithEmailAndPassword(email,password).addOnCompleteListener { task ->

            if(task.isSuccessful){

                Toast.makeText(applicationContext, "you are logged in moving to Main page", Toast.LENGTH_LONG).show()
                addUserToDataBase()


            }else{

                Toast.makeText(applicationContext, task.exception.toString(), Toast.LENGTH_LONG).show()

            }
        }
    }

    @Synchronized
    private fun addUserToDataBase(){

        val uuid : String = auth.currentUser?.uid.toString()
        val userName : String = signUpBinding.editTextName.text.toString()
        val rollNumber : String = signUpBinding.editTextUserId.text.toString()
        var role: String = "Student"
        if( signUpBinding.radioButtonAsATeacher.isChecked){
            role = "Teacher"
        }
        val attendancePictureUrl : String = ""
        val profilePictureUrl : String = ""

        val user = User(uuid,userName,rollNumber,role,attendancePictureUrl, profilePictureUrl)

        myRef.child(rollNumber).setValue(user).addOnCompleteListener { task ->

            if(task.isSuccessful){

                Toast.makeText(applicationContext, "User data added database", Toast.LENGTH_LONG).show()
                if (imageUri != null){
                    uploadPhoto()
                }


            }else{

                Toast.makeText(applicationContext, task.exception.toString(), Toast.LENGTH_LONG).show()

            }
        }
    }

    @Synchronized
    private fun choseImage(){

        val permission = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            Manifest.permission.READ_MEDIA_IMAGES
        }else{
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if(ContextCompat.checkSelfPermission(this,permission) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(permission),1)
        }else{
            val intent =Intent()
            intent.type= "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            activityResultLauncher.launch(intent)
        }
    }

    @Synchronized
    private fun registerActivityResultLauncher(){

        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback {

                val resultCode = it.resultCode
                val imageData = it.data
                if(resultCode == RESULT_OK && imageData != null){
                    imageUri = imageData.data
                }
                imageUri?.let {

                    Picasso.get().load(it).into(signUpBinding.signUpImageView)

                }

            })
    }

    private fun uploadPhoto(){

        signUpBinding.newUserSignUpButton.isClickable = false

        val imagename = signUpBinding.editTextUserId.text.toString()

        val imageRefernce = storageRef.child("ProfileImages").child(imagename)

        imageUri?.let {

            imageRefernce.putFile(it).addOnSuccessListener {

                Toast.makeText(applicationContext, "User image uploaded", Toast.LENGTH_SHORT).show()

                val uploadedImageUrl = storageRef.child("ProfileImages").child(imagename)
                uploadedImageUrl.downloadUrl.addOnSuccessListener {

                    val imageUrl : String = it.toString()

                    val userId = signUpBinding.editTextUserId.text.toString()

                    val updates = HashMap<String, Any>()
                    updates["profilePictureUrl"] = imageUrl

                    myRef.child(userId).updateChildren(updates)
                        .addOnSuccessListener {
                            // Handle success
                            Toast.makeText(applicationContext, "profile picture url uploaded", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            // Handle failure
                            Toast.makeText(applicationContext, e.toString(), Toast.LENGTH_SHORT).show()

                        }
                }

            }.addOnFailureListener{

                Toast.makeText(applicationContext, "ProfileImage upload failed", Toast.LENGTH_SHORT).show()

            }

        }

    }
}
