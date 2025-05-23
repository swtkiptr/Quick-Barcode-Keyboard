package id.swtkiptr.keyboardscanner

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class StepsAdapter(private val context: Context, private val callback: StepActionCallback) : 
    RecyclerView.Adapter<StepsAdapter.StepViewHolder>() {

    interface StepActionCallback {
        fun onStepCompleted(position: Int)
        fun showInputMethodPicker()
        fun checkCameraPermission(): Boolean
        fun requestCameraPermission()
    }

    private val steps = listOf(
        "Step 1: Enable the keyboard in settings. This allows your device to recognize our keyboard as an input method.",
        "Step 2: Select our keyboard as your current input method. This will activate the scanner keyboard.",
        "Step 3: Grant camera permissions for barcode scanning. This allows the keyboard to use your camera for scanning.",
        "Step 4: You're all set! Start using the keyboard to scan barcodes in any text field."
    )

    private val buttonTexts = listOf(
        "Open Keyboard Settings",
        "Select Scanner Keyboard",
        "Grant Camera Permission",
        "Finish Setup"
    )

    private val stepIcons = listOf(
        R.drawable.ic_keyboard_setup,
        R.drawable.ic_select_keyboard,
        R.drawable.ic_camera_permission,
        R.drawable.ic_success
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.step_item, parent, false)
        return StepViewHolder(view)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        holder.textView.text = steps[position]
        holder.actionButton.text = buttonTexts[position]
        holder.iconView.setImageResource(stepIcons[position])

        // Set button actions based on the step
        holder.actionButton.setOnClickListener {
            when (position) {
                0 -> { // Enable Keyboard
                    val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                    context.startActivity(intent)
                }
                1 -> { // Select Keyboard
                    callback.showInputMethodPicker()
                }
                2 -> { // Camera Permission
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
                        != PackageManager.PERMISSION_GRANTED) {
                        callback.requestCameraPermission()
                    } else {
                        callback.onStepCompleted(position)
                    }
                }
                3 -> { // Finish
                    callback.onStepCompleted(position)
                }
            }
        }

        // Update button state based on completion status
        when (position) {
            2 -> { // Camera permission step
                if (callback.checkCameraPermission()) {
                    holder.actionButton.text = "Permission Granted ✓"
                    holder.actionButton.isEnabled = false
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return steps.size
    }

    class StepViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.step_description)
        val actionButton: Button = itemView.findViewById(R.id.step_action_button)
        val iconView: ImageView = itemView.findViewById(R.id.step_icon)
    }
}