package de.impacgroup.mlbarcodescanner.module

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import de.impacgroup.mlbarcodescanner.module.mlkit.BarcodeScannerProcessor
import de.impacgroup.mlbarcodescanner.module.mlkit.CameraSource
import de.impacgroup.mlbarcodescanner.module.mlkit.CameraSourcePreview
import de.impacgroup.mlbarcodescanner.module.mlkit.GraphicOverlay
import java.io.IOException
import java.util.*

class CameraActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private val PERMISSION_REQUESTS: Int = 1
    private var cameraSource: CameraSource? = null
    private var preview: CameraSourcePreview? = null
    private var graphicOverlay: GraphicOverlay? = null

    companion object: CameraScanner {

        private val TAG: String = "CameraActivity"
        private const val CAMERA_VIEW_TITLE = "cameraViewTitle"
        private const val CAMERA_SCANNER_INFO = "cameraScannerInfo"

        override fun startScanner(activity: AppCompatActivity, title: String, info: ScannerInfo) {
            val detailIntent = Intent(activity.applicationContext, CameraActivity::class.java)
            detailIntent.putExtra(CAMERA_VIEW_TITLE, title)
            detailIntent.putExtra(CAMERA_SCANNER_INFO, info)
            activity.startActivity(detailIntent)
        }

        private fun isPermissionGranted(context: Context, permission: String?): Boolean {
            if (ContextCompat.checkSelfPermission(context, permission!!) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                Log.i(TAG, "Permission granted: $permission")
                return true
            }
            Log.i(TAG, "Permission NOT granted: $permission")
            return false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(loadResource("Theme.CameraView", ResourceType.STYLE))
        super.onCreate(savedInstanceState)
        setContentView(loadResource("activity_camera", ResourceType.LAYOUT))

        preview = findViewById(loadResource("preview_view", ResourceType.IDENTIFIER))
        if (preview == null) {
            Log.d(TAG, "Preview is null")
        }

        graphicOverlay = findViewById(loadResource("graphic_overlay", ResourceType.IDENTIFIER))
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null")
        }

        val closeBtn: ImageButton = findViewById(loadResource("close_button", ResourceType.IDENTIFIER))
        closeBtn.setOnClickListener {
            dismiss()
        }

        if (allPermissionsGranted()) {
            createCameraSource()
        } else {
            runtimePermissions
        }
    }

    override fun onStart() {
        super.onStart()

        intent.extras?.getString(CAMERA_VIEW_TITLE)?.let {
            val titleView: TextView = findViewById(loadResource("title_text_view", ResourceType.IDENTIFIER))
            titleView.text = it
        }

        intent.extras?.getParcelable<ScannerInfo>(CAMERA_SCANNER_INFO)?.let {
            val infoTitleView: TextView = findViewById(loadResource("info_title_text_view", ResourceType.IDENTIFIER))
            infoTitleView.text = it.title

            val infoTxtView: TextView = findViewById(loadResource("info_text_view", ResourceType.IDENTIFIER))
            infoTxtView.text = it.infoText

            val btn: Button = findViewById(loadResource("manual_button", ResourceType.IDENTIFIER))
            btn.text = it.button?.title
            val color = ColorStateList.valueOf(Color.parseColor(it.button?.backgroundColor))
            btn.backgroundTintList = color
        }
    }

    override fun onBackPressed() {
        dismiss()
    }

    private fun dismiss() {
        finish()
    }

    public override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        createCameraSource()
        startCameraSource()
    }

    /** Stops the camera. */
    override fun onPause() {
        super.onPause()
        preview?.stop()
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (cameraSource != null) {
            cameraSource?.release()
        }
    }

    private fun loadResource(name: String, defType: ResourceType): Int {
        val packageName: String = this.application.packageName
        val resources: Resources = this.application.resources
        return resources.getIdentifier(name, defType.key, packageName)
    }

    private val runtimePermissions: Unit
        get() {
            val allNeededPermissions: MutableList<String?> = ArrayList()
            for (permission in requiredPermissions) {
                if (!isPermissionGranted(this, permission)) {
                    allNeededPermissions.add(permission)
                }
            }
            if (allNeededPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    allNeededPermissions.toTypedArray(),
                    PERMISSION_REQUESTS
                )
            }
        }

    private val requiredPermissions: Array<String?>
        get() =
            try {
                val info =
                    this.packageManager.getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
                val ps = info.requestedPermissions
                if (ps != null && ps.isNotEmpty()) {
                    ps
                } else {
                    arrayOfNulls(0)
                }
            } catch (e: Exception) {
                arrayOfNulls(0)
            }

    private fun allPermissionsGranted(): Boolean {
        for (permission in requiredPermissions) {
            if (!isPermissionGranted(this, permission)) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.i(TAG, "Permission granted!")
        if (allPermissionsGranted()) {
            createCameraSource()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private fun startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null")
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null")
                }
                preview!!.start(cameraSource, graphicOverlay)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source.", e)
                cameraSource!!.release()
                cameraSource = null
            }
        }
    }

    private fun createCameraSource() {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = CameraSource(this, graphicOverlay)
        }
        try {
            Log.i(TAG, "Using Barcode Detector Processor")
            cameraSource!!.setMachineLearningFrameProcessor(BarcodeScannerProcessor(this))
        } catch (e: Exception) {
            Log.e(TAG, "Can not create image processor: Barcode", e)
            Toast.makeText(
                applicationContext,
                "Can not create image processor: " + e.message,
                Toast.LENGTH_LONG
            )
                .show()
        }
    }
}