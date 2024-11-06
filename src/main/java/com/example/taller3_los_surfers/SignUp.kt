package com.example.taller3_los_surfers


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.taller3_los_surfers.databinding.ActivitySingupBinding


class SignUp : AppCompatActivity() {

    private lateinit var binding: ActivitySingupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingupBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
