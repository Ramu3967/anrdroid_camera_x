package com.example.applicationcamerax

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import com.example.applicationcamerax.databinding.ActivityMainBinding
import com.example.applicationcamerax.imageanalyzers.LuminosityAnalyzer
import com.example.applicationcamerax.imageanalyzers.TextAnalyzer
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@ExperimentalGetImage class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null
    private var luminosityListener : (luma: Double) -> Unit = {
        Log.d(TAG, "luma Listener: avg luminosity: $it")
    }

    private lateinit var cameraExecutor: ExecutorService
    private var isOcrEnabled = false
    private lateinit var tessBaseAPI: TessBaseAPI

    // ImageAnalyzer use case
    private val imageAnalyzer = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()


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

        // Initialize Tesseract
        initTesseract()

        // request camera permissions
        if (allPermissionsGranted()) startCamera()
        else requestPermissions()

        // setting up the listeners
        binding.btnImageCapture.setOnClickListener { takePhoto() }
        binding.btnDetect.setOnClickListener { detectText() }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun initTesseract() {
        try{
            tessBaseAPI = TessBaseAPI()
            val tessDataDir: File = getExternalFilesDir(null)
                ?: throw RuntimeException("Unable to get external files directory.")
            val tessDataPath = tessDataDir.absolutePath

            // Copy tesseract data from assets to the app's external files directory
            copyTessDataToExternal(tessDataPath)

            // Initialize tesseract with the language and the path to the Tesseract data
            tessBaseAPI.init(tessDataPath, "eng") // Change "eng" to the language code you need
            Log.d("OCR_RESULT", "Tesseract initialized successfully")
        }catch (e: Exception) {
            Log.e("OCR_ERROR", "Error initializing Tesseract", e)
        }
    }

    // tesseract needs the resources to be in the ext storage
    private fun copyTessDataToExternal(tessDataPath: String) {
        try {
            val assetManager: AssetManager = assets
            val inputStream: InputStream = assetManager.open("eng.traineddata")

            // Create the tessdata subfolder if it doesn't exist
            val tessDataDir = File(tessDataPath, "tessdata")
            if (!tessDataDir.exists()) {
                tessDataDir.mkdirs()
            }

            val outFile = File(tessDataDir, "eng.traineddata")
            val outputStream = FileOutputStream(outFile)

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Log.e("CopyTessData", "Error copying Tesseract data", e)
        }
    }

    private fun allPermissionsGranted() =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        // create create time stamped name and MediaStore entry
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.CANADA).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply{
            put(MediaStore.MediaColumns.DISPLAY_NAME,name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P){
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }
        // create output options object which contains file's metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()
        // img capture listener, triggered after the photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object: ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${outputFileResults.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "onImageSaved: $msg")
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "onError: photo capture failed: ${exception.message}",exception )
                }
            }
        )
    }

    private fun detectText() {
        toggleOcr()
    }

    private fun startCamera() {
        COORDINATE_SYSTEM_VIEW_REFERENCED
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // used to bind the lifecycle of the camera to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            // Preview use case
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(binding.viewFinder.surfaceProvider) }
            // ImageCapture use case
            imageCapture = ImageCapture.Builder().build()


            // select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            kotlin.runCatching {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalyzer)
            }.onFailure { Log.e(TAG, "startCamera: Use case binding failed $it ", ) }

        },ContextCompat.getMainExecutor(this))

        // Set up the imageAnalyzer to process frames
        imageAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(this)) { image ->
            if (isOcrEnabled) {
                // Convert CameraX Image to tesseract-compatible format
                val bitmap = image.image?.toBitmap()

                val ocrResult = bitmap?.let { performOCR(it) }

                // print the OCR result in logs
                if (ocrResult != null) {
                    Log.d("OCR_RESULT", ocrResult)
                }

                // Disable OCR until the button is pressed again
                isOcrEnabled = false
            }

            image.close()
        }
    }

    private fun toggleOcr() {
        // Toggle the OCR state
        isOcrEnabled = !isOcrEnabled

        // If OCR is enabled, you might want to perform some UI updates or feedback
        if (isOcrEnabled) {
            // Update UI or provide feedback, if needed
            Toast.makeText(this, "OCR Enabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestPermissions() { activityResultLauncher.launch(REQUIRED_PERMISSIONS)}

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun Image.toBitmap(): Bitmap {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // Check if the byte array is not null and has valid data
        if (bytes.isNotEmpty()) {
            // Ensure that the BitmapFactory.decodeByteArray does not receive a null array
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            Log.d("OCR_RESULT", "Bitmap created successfully")
            return bitmap ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            // Handle the case when the byte array is empty
            Log.e("OCR_ERROR", "Byte array is empty")
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
    }


    private fun performOCR(bitmap: Bitmap): String {
        try {
            // Perform OCR on the bitmap
            tessBaseAPI.setImage(bitmap)
            val ocrResult = tessBaseAPI.utF8Text
            Log.d("OCR_RESULT", "OCR Result: $ocrResult")
            return ocrResult
        } catch (e: Exception) {
            Log.e("OCR_ERROR", "Error performing OCR", e)
            return "OCR Error"
        }
    }

    companion object{
        private const val TAG = "CameraXApplication"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }.toTypedArray()
    }
}