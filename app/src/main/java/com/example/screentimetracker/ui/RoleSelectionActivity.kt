package com.example.screentimetracker.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.screentimetracker.databinding.ActivityRoleSelectionBinding

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartAsUser.setOnClickListener {
            startActivity(Intent(this, UserActivity::class.java))
            finish() // remove this screen from the back stack
        }
    }
}
