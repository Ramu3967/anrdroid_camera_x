package com.example.applicationcamerax.imageanalyzers

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.example.applicationcamerax.utils.ImageUtils
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition

class TextAnalyzer(
    private val context: Context,
    private val lifecycle: Lifecycle,
    private val result: MutableLiveData<String>
): ImageAnalysis.Analyzer {

    private val detector = TextRecognition.getClient()

    init {
        lifecycle.addObserver(detector)
    }

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image ?: return
            val convertImageToBitmap = ImageUtils.convertYuv420888ImageToBitmap(mediaImage)
            recognizeText(InputImage.fromBitmap(convertImageToBitmap,0)).addOnCompleteListener {
                imageProxy.close()
            }
        }

    private fun recognizeText(image: InputImage): Task<Text> {
        return detector.process(image)
            .addOnSuccessListener {
                result.value = it.text
            }
            .addOnFailureListener {
                Log.e(TAG, "recognizeText: failed to process the image" )
            }
    }

    companion object {
        private const val TAG = "TextAnalyzer"
    }
}