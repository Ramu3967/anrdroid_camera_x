package com.example.applicationcamerax.imageanalyzers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import android.util.Log
import android.util.SparseArray
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.Text
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import java.nio.ByteBuffer

@ExperimentalGetImage class TextAnalyzer(private val context: Context): ImageAnalysis.Analyzer {
    private val TAG = "my#TextAnalyzer"
    private val textRecognizer: TextRecognizer by lazy {
        TextRecognizer.Builder(context).build()
    }

    init {
        if (!textRecognizer.isOperational) {
            Log.w(TAG, "TextRecognizer dependencies are not yet available.")
        }
    }

    override fun analyze(imageProxy: ImageProxy) {
//        val bitmap = imageToBitmap(imageProxy.image)
//        processImage(bitmap)
//        imageProxy.close()

        textRecognizer.setProcessor(object: Detector.Processor<TextBlock>{
            override fun release() {

            }

            override fun receiveDetections(detections: Detector.Detections<TextBlock>) {
                val items = detections.detectedItems
                if(items.size() != 0){

                }
            }
        })

        //
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val frame = imageProxyToBitmap(imageProxy)?.let { Frame.Builder().setBitmap(it).build() }
            val textBlocks = frame?.let { textRecognizer.detect(it) }

            if (textBlocks != null) {
                processTextBlocks(textBlocks)
            }

            mediaImage.close()
        }
    }

    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val image: Image = imageProxy.image ?: return null

        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer

        // Ensure the buffer is properly rewound before reading
        buffer.rewind()

        val width = image.width
        val height = image.height
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width

        // Calculate the adjusted buffer size
        val adjustedBufferSize = buffer.remaining() + rowPadding

        // Create a new buffer with the adjusted size
        val adjustedBuffer = ByteBuffer.allocate(adjustedBufferSize)
        buffer.get(adjustedBuffer.array(), 0, buffer.remaining())

        val bitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height,
            Bitmap.Config.ARGB_8888
        )

        bitmap.copyPixelsFromBuffer(adjustedBuffer)
        imageProxy.close()

        return bitmap
    }

    private fun processTextBlocks(textBlocks: SparseArray<TextBlock>) {
        // Process the detected text blocks as needed
        for (index in 0 until textBlocks.size()) {
            val textBlock = textBlocks.valueAt(index)
            val text = textBlock.value
            // Do something with the extracted text
            // You may also obtain other information like textBlock.boundingBox, textBlock.cornerPoints, etc.
            Log.e(TAG, "processTextBlocks: $text", )
        }
    }
}