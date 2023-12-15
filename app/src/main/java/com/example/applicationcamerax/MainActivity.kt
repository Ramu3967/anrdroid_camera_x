package com.example.applicationcamerax

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.applicationcamerax.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding


    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()){permissions ->
        // Handle the permission results
        var permissionGranted = true
        permissions.entries.forEach{
            if (it.key in REQUIRED_PERMISSIONS && it.value == false) permissionGranted =false
        }
        if (!permissionGranted)
            Toast.makeText(baseContext, "Permissions request denied", Toast.LENGTH_SHORT).show()
        else startCamera()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // request camera permissions
        if (allPermissionsGranted()) startCamera()
        else requestPermissions()

        // setting up the listeners
        binding.btnImageCapture.setOnClickListener { takePhoto() }
        binding.btnVideoCapture.setOnClickListener { captureVideo() }

    }

    private fun allPermissionsGranted() =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun takePhoto() {}

    private fun captureVideo() {}

    private fun startCamera() {}

    private fun requestPermissions() { activityResultLauncher.launch(REQUIRED_PERMISSIONS)}

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object{
        private const val TAG = "CameraXApplication"
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }.toTypedArray()
    }
}