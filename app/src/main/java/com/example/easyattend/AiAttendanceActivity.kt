package com.example.easyattend

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.easyattend.databinding.ActivityAiAttendanceBinding
import com.squareup.picasso.Picasso
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AiAttendanceActivity : AppCompatActivity() {

    lateinit var aiAttendanceViewBinding: ActivityAiAttendanceBinding
    private var imageUri : Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aiAttendanceViewBinding = ActivityAiAttendanceBinding.inflate(layoutInflater)
        val view = aiAttendanceViewBinding.root
        setContentView(view)
        aiAttendanceViewBinding.buttonAiAttendenceImage.setOnClickListener(){

            takePicture()

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

    private val REQUEST_CODE_PERMISSIONS = 123

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
            val imageUri = result.data?.data
            imageUri?.let {

                Picasso.get().load(it).into(aiAttendanceViewBinding.AiClassAttendanceImageView)
                aiAttendanceViewBinding.progressBarClassAiAttendanceImage.visibility = View.INVISIBLE

            }
        }
    }
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cacheDir = cacheDir
        return File.createTempFile(
            "JPEG_${timeStamp}_",
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

}