package id.swtkiptr.keyboardscanner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.content.SharedPreferences
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity(), StepsAdapter.StepActionCallback {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isFirstLaunch = sharedPreferences.getBoolean("is_first_launch", true)
        
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        // Disable swiping between pages - we want to control navigation
        viewPager.isUserInputEnabled = false

        // Set up the ViewPager with StepsAdapter
        val stepsAdapter = StepsAdapter(this, this)
        viewPager.adapter = stepsAdapter

        // Set up TabLayout with ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = "Step ${position + 1}"
        }.attach()

        // Always start at step 1 for first launch
        if (isFirstLaunch) {
            viewPager.currentItem = 0
            // Mark first launch as completed
            sharedPreferences.edit().putBoolean("is_first_launch", false).apply()
        } else {
            // Check if keyboard is already enabled
            val isKeyboardEnabled = isKeyboardEnabled()
            val isKeyboardSelected = isKeyboardSelected()
            val isCameraPermissionGranted = checkCameraPermission()

            // Determine the start position based on what's already set up
            var startPosition = 0
            if (isKeyboardEnabled) {
                startPosition = 1
                if (isKeyboardSelected) {
                    startPosition = 2
                    if (isCameraPermissionGranted) {
                        startPosition = 3
                    }
                }
            }

            // Set initial page
            viewPager.currentItem = startPosition
        }
    }

    private fun isKeyboardEnabled(): Boolean {
        val enabledInputMethods = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_INPUT_METHODS) ?: ""
        // More precise check: look for the exact IME service name pattern, not just package name
        val serviceComponent = "$packageName/.BarcodeInputService"
        return enabledInputMethods.contains(serviceComponent)
    }

    private fun isKeyboardSelected(): Boolean {
        val selectedInputMethod = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
        return selectedInputMethod.contains(packageName)
    }

    override fun onStepCompleted(position: Int) {
        // Move to next step if not the last one
        if (position < 3) {
            viewPager.currentItem = position + 1
        } else {
            // On the last step (step 4), save that setup is completed
            sharedPreferences.edit().putBoolean("setupCompleted", true).apply()
            Toast.makeText(this, "Setup completed! You can now use the keyboard.", Toast.LENGTH_LONG).show()
        }
    }

    override fun showInputMethodPicker() {
        val imeManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imeManager.showInputMethodPicker()
    }

    override fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == 
                PackageManager.PERMISSION_GRANTED
    }

    override fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this, 
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
                onStepCompleted(2)  // Move to next step after camera permission granted
            } else {
                Toast.makeText(this, "Camera permission is required for scanning", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Activity result handling for checking when user returns from enabling keyboard
    override fun onResume() {
        super.onResume()
        
        // First verify if we need to reset to step 1
        if (!isKeyboardEnabled() && viewPager.currentItem > 0) {
            viewPager.currentItem = 0
            return
        }
        
        // Check current step and verify if completed
        when (viewPager.currentItem) {
            0 -> if (isKeyboardEnabled()) onStepCompleted(0)
            1 -> if (isKeyboardSelected()) onStepCompleted(1)
            2 -> if (checkCameraPermission()) onStepCompleted(2)
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }
}