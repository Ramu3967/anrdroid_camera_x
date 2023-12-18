package com.example.applicationcamerax.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class OcrHelper(context: Context, language: String) {
    private val mTess: TessBaseAPI = TessBaseAPI()
    private val TAG = "TesseractOCR"

    init {
        var fileExistFlag = false
        val assetManager = context.assets

        val dstPathDir = "/tesseract/tessdata/"
        val srcFile = "eng.traineddata"
        var inFile: InputStream? = null

        val dstPathDirFull = File(context.filesDir, dstPathDir)
        val dstInitPathDir = File(context.filesDir, "/tesseract")
        val dstPathFile = File(dstPathDirFull, srcFile)
        var outFile: FileOutputStream? = null

        try {
            inFile = assetManager.open(srcFile)

            if (!dstPathDirFull.exists()) {
                if (!dstPathDirFull.mkdirs()) {
                    Toast.makeText(context, "$srcFile can't be created.", Toast.LENGTH_SHORT).show()
                }
                outFile = FileOutputStream(dstPathFile)
            } else {
                fileExistFlag = true
            }

        } catch (ex: Exception) {
            Log.e(TAG, ex.message ?: "")

        } finally {
            if (fileExistFlag) {
                try {
                    inFile?.close()
                    mTess.init(dstInitPathDir.absolutePath, language)
                } catch (ex: Exception) {
                    Log.e(TAG, ex.message ?: "")
                }
            }

            if (inFile != null && outFile != null) {
                try {
                    // Copy file
                    val buf = ByteArray(1024)
                    var len: Int
                    while (inFile.read(buf).also { len = it } != -1) {
                        outFile.write(buf, 0, len)
                    }
                    inFile.close()
                    outFile.close()
                    mTess.init(dstInitPathDir.absolutePath, language)
                } catch (ex: IOException) {
                    Log.e(TAG, ex.message ?: "")
                }
            } else {
                Toast.makeText(context, "$srcFile can't be read.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getOCRResult(bitmap: Bitmap): String {
        mTess.setImage(bitmap)
        return mTess.utF8Text
    }

    fun onDestroy() {
        mTess.end()
    }

}