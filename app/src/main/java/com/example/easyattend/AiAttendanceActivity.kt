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
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.easyattend.databinding.ActivityAiAttendanceBinding
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import java.io.File
import java.io.IOException

class AiAttendanceActivity : AppCompatActivity() {

    private lateinit var aiAttendanceViewBinding: ActivityAiAttendanceBinding
    private lateinit var activityResultLauncher : ActivityResultLauncher<Intent>

    private val storage : FirebaseStorage = FirebaseStorage.getInstance()
    private var storageRef : StorageReference = storage.reference.child("AIImages")
    private val myRefCI = Firebase.database.reference.child("CLassImages")

    private val REQUEST_CODE_PERMISSIONS = 123
    private var imageUri : Uri? = null
    private lateinit var userId : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aiAttendanceViewBinding = ActivityAiAttendanceBinding.inflate(layoutInflater)
        val view = aiAttendanceViewBinding.root
        setContentView(view)
        registerActivityResultLauncher()

        userId = intent.getStringExtra("UserId").toString()
        var className = intent.getStringExtra("ClassName").toString()
        val classId = intent.getStringExtra("ClassId").toString()
        var userName = intent.getStringExtra("UserName").toString()
        var date = intent.getStringExtra("Date").toString().trim()

        aiAttendanceViewBinding.textViewClassNameForAiAttendance.text = className

        aiAttendanceViewBinding.progressBarClassAiAttendanceImage.visibility = View.INVISIBLE

        aiAttendanceViewBinding.buttonAiAttendenceImage.setOnClickListener(){

            takePicture()

        }
        aiAttendanceViewBinding.AiClassAttendanceImageView.setOnClickListener(){

            choseImage()

        }
        aiAttendanceViewBinding.buttonTakeAiAttendance.setOnClickListener(){

            uploadPhoto(classId,date)

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
        if(requestCode == 1 && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }){
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intent)
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
                    Picasso.get().load(it).into(aiAttendanceViewBinding.AiClassAttendanceImageView)
                    aiAttendanceViewBinding.progressBarClassAiAttendanceImage.visibility = View.INVISIBLE
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

    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            imageUri = result.data?.data
            Toast.makeText(applicationContext, "result came ${result.resultCode} ${result.data}", Toast.LENGTH_LONG).show()
            imageUri?.let {
                Picasso.get().load(it).into(aiAttendanceViewBinding.AiClassAttendanceImageView)
                aiAttendanceViewBinding.progressBarClassAiAttendanceImage.visibility = View.INVISIBLE

            }
        }
    }

    private fun uploadPhoto(classId : String,date : String){

        aiAttendanceViewBinding.buttonTakeAiAttendance.isClickable = false


        val imageRefernce = storageRef.child(date).child(classId)

        imageUri?.let {

            imageRefernce.putFile(it).addOnSuccessListener {

                Toast.makeText(applicationContext, " image uploaded", Toast.LENGTH_SHORT).show()

                val uploadedImageUrl = storageRef.child(date).child(classId)
                uploadedImageUrl.downloadUrl.addOnSuccessListener {

                    val AIImageUrl : String = it.toString()
                    val data = AIImage(AIImageUrl)
                    myRefCI.child(date).child(classId).setValue(data).addOnCompleteListener { task ->

                        if(task.isSuccessful){
                            Toast.makeText(applicationContext, "imageUrl added database", Toast.LENGTH_LONG).show()
                        }else{
                            Toast.makeText(applicationContext, task.exception.toString(), Toast.LENGTH_LONG).show()
                        }
                    }

                }
            }.addOnFailureListener{
                Toast.makeText(applicationContext, "AIAttendanceImage upload failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

