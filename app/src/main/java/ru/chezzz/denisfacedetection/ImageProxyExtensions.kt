package ru.chezzz.denisfacedetection

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

fun ImageProxy.toBitmap(): Bitmap {

    val yBuffer = planes[0].buffer

    val bytes =
        ByteArray(yBuffer.remaining())

    yBuffer.get(bytes)

    val yuvImage =
        YuvImage(
            bytes,
            ImageFormat.NV21,
            width,
            height,
            null
        )

    val stream =
        ByteArrayOutputStream()

    yuvImage.compressToJpeg(
        Rect(
            0,
            0,
            width,
            height
        ),
        100,
        stream
    )

    val jpeg =
        stream.toByteArray()

    return BitmapFactory.decodeByteArray(
        jpeg,
        0,
        jpeg.size
    )
}