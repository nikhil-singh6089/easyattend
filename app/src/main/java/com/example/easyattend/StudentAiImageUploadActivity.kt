package com.example.easyattend

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.easyattend.databinding.ActivityStudentAiImageUploadBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import java.io.File
import java.io.IOException

class StudentAiImageUploadActivity : AppCompatActivity() {

    private lateinit var studentAiImageUploadBinding: ActivityStudentAiImageUploadBinding
    private lateinit var activityResultLauncher : ActivityResultLauncher<Intent>

    private var imageUri : Uri? = null
    private var rollNumber : String? = null
    private val REQUEST_CODE_PERMISSIONS = 123

    private val myRefUser = Firebase.database.reference.child("Users")
    private var storage : FirebaseStorage = FirebaseStorage.getInstance()
    private var storageRef : StorageReference = storage.reference.child("AttendanceImages")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        studentAiImageUploadBinding = ActivityStudentAiImageUploadBinding.inflate(layoutInflater)
        val view = studentAiImageUploadBinding.root
        setContentView(view)
        studentAiImageUploadBinding.progressBarStudentAiImageActivity.visibility = View.INVISIBLE
        registerActivityResultLauncher()

        rollNumber = intent.getStringExtra("RollNumber").toString().trim()

        studentAiImageUploadBinding.buttonTakeImageStudentAiImageActivity.setOnClickListener(){

            takePicture()

        }
        studentAiImageUploadBinding.buttonUpdateImageStudentAiImageActivity.setOnClickListener(){

            updateImage(rollNumber!!)

        }
        studentAiImageUploadBinding.ImageViewStudentAiImageActivity.setOnClickListener(){

            choseImage()

        }

    }

    private fun takePicture() {
        val permissionCamera = Manifest.permission.CAMERA
        val permissionStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE
        val permissionReadStorage = Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permissionCamera) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, permissionStorage) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, permissionReadStorage) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(permissionCamera, permissionStorage, permissionReadStorage),
                REQUEST_CODE_PERMISSIONS
            )
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoFile = try {
                createImageFile()
            } catch (e: IOException) {
                null
            }

            photoFile?.also {
                imageUri = FileProvider.getUriForFile(
                    this,
                    "com.example.easyattend.fileprovider",
                    it
                )
                cropImage(imageUri)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                startActivityForResult(takePictureIntent, 1)
                //cropImage(imageUri)
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                openCamera()
            } else {
                // Handle permission denial
            }
        }

    }

    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // Handle the cropped image URI
            imageUri = result.data?.data
            imageUri?.let {
                Picasso.get().load(it).into(studentAiImageUploadBinding.ImageViewStudentAiImageActivity)
                studentAiImageUploadBinding.progressBarStudentAiImageActivity.visibility = View.INVISIBLE

            }
        }
    }
    @Throws(IOException::class)
    private fun createImageFile(): File {
        //val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cacheDir = cacheDir
        return File.createTempFile(
            "ClassAIAttendancePicture",
            ".jpg",
            cacheDir
        )
    }
    private fun cropImage(imageUri: Uri?) {
        imageUri?.let {
            val cropIntent = Intent("com.android.camera.action.CROP")
            cropIntent.setDataAndType(it, "image/*")
            cropIntent.putExtra("crop", "true")
            cropIntent.putExtra("aspectX", 1)
            cropIntent.putExtra("aspectY", 1)
            cropIntent.putExtra("outputX", 200)
            cropIntent.putExtra("outputY", 200)
            cropIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            cropIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            cropLauncher.launch(cropIntent)

        }
    }

    private fun updateImage(rollNumber : String){

        studentAiImageUploadBinding.buttonUpdateImageStudentAiImageActivity.isClickable = false
        studentAiImageUploadBinding.buttonTakeImageStudentAiImageActivity.isClickable = false

        val imagename : String = rollNumber

        val imageRefernce = storageRef.child(imagename)

        imageUri?.let {

            imageRefernce.putFile(it).addOnSuccessListener {

                Toast.makeText(applicationContext, "User image uploaded", Toast.LENGTH_SHORT).show()

                val uploadedImageUrl = storageRef.child(imagename)
                uploadedImageUrl.downloadUrl.addOnSuccessListener {

                    val imageUrl : String = it.toString()

                    val updates = HashMap<String, Any>()
                    updates["attendancePictureUrl"] = imageUrl

                    myRefUser.child(rollNumber).updateChildren(updates)
                        .addOnSuccessListener {
                            // Handle success
                            Toast.makeText(applicationContext, "Attendance picture url uploaded", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            // Handle failure
                            Toast.makeText(applicationContext, e.toString(), Toast.LENGTH_SHORT).show()

                        }
                }
                studentAiImageUploadBinding.buttonUpdateImageStudentAiImageActivity.isClickable = true
                studentAiImageUploadBinding.buttonTakeImageStudentAiImageActivity.isClickable = true

            }.addOnFailureListener{

                Toast.makeText(applicationContext, "AttendanceImage upload failed", Toast.LENGTH_SHORT).show()
                studentAiImageUploadBinding.buttonUpdateImageStudentAiImageActivity.isClickable = true
                studentAiImageUploadBinding.buttonTakeImageStudentAiImageActivity.isClickable = true

            }

        }
    }
    @Synchronized
    private fun choseImage(){

        val permissionCamera = Manifest.permission.CAMERA
        val permissionStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE
        val permissionReadStorage = Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, permissionCamera) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, permissionStorage) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, permissionReadStorage) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(permissionCamera, permissionStorage, permissionReadStorage),
                1
            )
        } else {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
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
                    Picasso.get().load(it).into(studentAiImageUploadBinding.ImageViewStudentAiImageActivity)
                    studentAiImageUploadBinding.progressBarStudentAiImageActivity.visibility = View.INVISIBLE
                    cropImageOld(imageUri)

                }
            })
    }

    private fun cropImageOld(imageUri: Uri?) {
        imageUri?.let { sourceUri ->
            val cropIntent = Intent("com.android.camera.action.CROP")
            cropIntent.setDataAndType(sourceUri, "image/*")
            cropIntent.putExtra("crop", "true")
            cropIntent.putExtra("aspectX", 1)
            cropIntent.putExtra("aspectY", 1)
            cropIntent.putExtra("outputX", 200)
            cropIntent.putExtra("outputY", 200)
            cropIntent.putExtra("scale", true)
            cropIntent.putExtra("return-data", false) // making it false did it but why
            cropLauncher.launch(cropIntent)
        }
    }
}