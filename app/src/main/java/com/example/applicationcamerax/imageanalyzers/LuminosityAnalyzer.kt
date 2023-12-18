package com.example.applicationcamerax.imageanalyzers

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

class LuminosityAnalyzer(private val listener: (luma: Double)-> Unit) : ImageAnalysis.Analyzer{
    override fun analyze(image: ImageProxy) {
        val buffer = image.planes[0].buffer
        val data = buffer.toByteArray()
        val pixels = data.map{ it.toInt() and 0xFF}
        val luma = pixels.average()

        listener(luma)
        image.close()
    }

    private fun ByteBuffer.toByteArray(): ByteArray{
        rewind() // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data) // copy the buffer into a byte array
        return data
    }
}