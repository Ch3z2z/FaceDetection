package ru.chezzz.denisfacedetection

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.Color
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class KeypointModel(context: Context) {

    private val interpreter: Interpreter

    init {
        interpreter = Interpreter(loadModelFile(context, "facial_keypoints_model.tflite"))
    }

    private fun loadModelFile(context: Context, modelPath: String): ByteBuffer {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predict(faceBitmap: Bitmap): FloatArray {
        val resized = Bitmap.createScaledBitmap(faceBitmap, 96, 96, true)

        val input = ByteBuffer.allocateDirect(96 * 96 * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        for (y in 0 until 96) {
            for (x in 0 until 96) {
                val p = resized.getPixel(x, y)
                val gray = (Color.red(p) + Color.green(p) + Color.blue(p)) / 3f
                input.putFloat(gray / 255f)
            }
        }

        val output = Array(1) { FloatArray(30) }
        interpreter.run(input, output)

        return output[0]
    }
}