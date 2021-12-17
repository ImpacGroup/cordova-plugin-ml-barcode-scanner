package de.impacgroup.mlbarcodescanner.module

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.Barcode
import de.impacgroup.mlbarcodescanner.module.mlkit.BarcodeScannerProcessor
import de.impacgroup.mlbarcodescanner.module.mlkit.BarcodeScannerProcessorListener
import de.impacgroup.mlbarcodescanner.module.mlkit.MlImageAnalyzer
import kotlinx.coroutines.*
import java.lang.Runnable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume

abstract class CameraActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private lateinit var cameraExecutor: ExecutorService
    private val PERMISSION_REQUESTS: Int = 1
    private lateinit var previewView: PreviewView
    private lateinit var preview: Preview
    private lateinit var infoLayout: ConstraintLayout
    private lateinit var resultLayout: ConstraintLayout
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var cameraProvider: ProcessCameraProvider? = null
    private var detectedBarcodes: List<Barcode> = ArrayList()


    companion object {

        private val TAG: String = "CameraActivity"
        const val CAMERA_VIEW_TITLE = "cameraViewTitle"
        const val CAMERA_SCANNER_INFO = "cameraScannerInfo"
        const val RESULT_ANIMATION_DURATION = 550

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

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

        previewView = findViewById(loadResource("view_finder", ResourceType.IDENTIFIER))
        resultLayout = findViewById(loadResource("result_view_layout", ResourceType.IDENTIFIER))
        infoLayout = findViewById(loadResource("info_view_layout", ResourceType.IDENTIFIER))

        val closeBtn: ImageButton = findViewById(loadResource("close_button", ResourceType.IDENTIFIER))
        closeBtn.setOnClickListener {
            dismiss()
        }

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        previewView.post {
            if (allPermissionsGranted()) {
                setupCamera()
            } else {
                runtimePermissions
            }
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

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {

            // CameraProvider
            cameraProvider = cameraProviderFuture.get()
            if (!hasBackCamera()) {
                finish()
            }

            // Build and bind the camera use cases
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /** Declare and bind preview, capture and analysis use cases */
    private fun bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = windowManager.defaultDisplay

        val screenAspectRatio = aspectRatio(metrics.width, metrics.height)

        val rotation = metrics.rotation

        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        // Preview
        preview = Preview.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation
            .setTargetRotation(rotation)
            .build()

        // ImageCapture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            // We request aspect ratio but no resolution to match preview config, but letting
            // CameraX optimize for whatever specific resolution best fits our use cases
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            .setTargetRotation(rotation)
            .build()

        val analyzer = MlImageAnalyzer(BarcodeScannerProcessor(this))
        analyzer.listener = barcodeListener
        // ImageAnalysis
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, analyzer)
            }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture, imageAnalyzer)

            // Attach the viewfinder's surface provider to preview use case
            preview.setSurfaceProvider(previewView.surfaceProvider)
            // preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
            // observeCameraState(camera?.cameraInfo!!)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    /**
     *  [androidx.camera.core.ImageAnalysis.Builder] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {

        val previewRatio = width.coerceAtLeast(height).toDouble() / width.coerceAtMost(height)
        if (kotlin.math.abs(previewRatio - RATIO_4_3_VALUE) <= kotlin.math.abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }


    val barcodeListener = object: BarcodeScannerProcessorListener {
        override fun onSuccess(result: List<Barcode>) {
            CoroutineScope(Dispatchers.IO).launch {
                reduceToNew(
                    validate(result)
                )?.let { code ->
                    withContext(Dispatchers.Main) {
                        cameraExecutor.shutdown()
                        code.displayValue?.let {
                            val mCode = Code(it, BarcodeType.from(code))
                            mark(mCode)
                            didSelect(mCode)
                        }
                        dismiss()
                    }
                }
            }
        }
    }

    private suspend fun validate(barcodes: List<Barcode>): List<Barcode>  = barcodes.filter { barcode ->
        barcode.displayValue?.let {
            isValid(Code(it, BarcodeType.from(barcode)))
        } ?: kotlin.run { false }
    }

    private fun reduceToNew(barcodes: List<Barcode>): Barcode? {
        val lastBarcodes = detectedBarcodes
        detectedBarcodes = barcodes
        if (barcodes.isEmpty() ||
            lastBarcodes.isNotEmpty() &&
            lastBarcodes.first().displayValue == barcodes.first().displayValue
        ) {
            return null
        } else {
            return detectedBarcodes.first()
        }
    }

    private suspend fun mark(code: Code) {
        val result = resultFor(code)
        updateResultView(result)
    }

    private suspend fun updateResultView(result: ScannerResult) = suspendCancellableCoroutine<Unit> { cont ->
        val textView: TextView = findViewById(loadResource("result_title_text_view", ResourceType.IDENTIFIER))
        val imageView: ImageView = findViewById(loadResource("result_image_view", ResourceType.IDENTIFIER))
        textView.text = result.title
        if (result.successImg != null) {
            imageView.setImageDrawable(result.successImg)
        } else {
            imageView.setImageDrawable(AppCompatResources.getDrawable(this, loadResource("ic_camera_success_check_circle_24", ResourceType.DRAWABLE)))
        }
        imageView.setColorFilter(Color.parseColor(result.imgTintColor), PorterDuff.Mode.SRC_IN)

        val listener = object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                cont.resume(Unit)
            }
        }

        resultLayout.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate()
                .alpha(1.0f)
                .setDuration(RESULT_ANIMATION_DURATION.toLong())
                .setListener(listener)
        }
        infoLayout.animate()
            .alpha(0f)
            .setDuration(RESULT_ANIMATION_DURATION.toLong())
            .setListener(null)
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (!cameraExecutor.isShutdown) {
            cameraExecutor.shutdown()
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

    private val requiredPermissions: List<String>
        get() =
            listOf(android.Manifest.permission.CAMERA)

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
            setupCamera()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    protected abstract suspend fun isValid(code: Code): Boolean

    protected abstract suspend fun resultFor(code: Code): ScannerResult

    protected abstract fun didSelect(code: Code)
}