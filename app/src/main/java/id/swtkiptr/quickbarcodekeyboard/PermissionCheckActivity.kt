package id.swtkiptr.keyboardscanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PermissionCheckActivity : AppCompatActivity() {
    // Modern permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, we can close this activity
            Toast.makeText(this, R.string.camera_permission_granted, Toast.LENGTH_SHORT).show()
        } else {
            // Permission denied, show a message
            Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show()
        }
        // Always finish, the InputService will handle showing permission button if needed
        finish()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_check)
        
        // Check and request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            // Already has permission, just finish
            finish()
        }
    }
}