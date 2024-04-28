package com.example.easyattend

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.easyattend.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    lateinit var profileBinding : ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profileBinding = ActivityProfileBinding.inflate(layoutInflater)
        val view = profileBinding.root
        setContentView(view)

    }
}