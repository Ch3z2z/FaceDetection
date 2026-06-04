package ru.chezzz.denisfacedetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File

class FaceAnalyzer(
    context: Context,
    private val overlay: FaceOverlayView
) : ImageAnalysis.Analyzer {

    private val model = KeypointModel(context)

    private lateinit var faceCascade: CascadeClassifier

    init {
        loadCascade(context)
    }

    private fun loadCascade(context: Context) {

        val input =
            context.assets.open(
                "haarcascade_frontalface_default.xml"
            )

        val cascadeDir =
            context.getDir(
                "cascade",
                Context.MODE_PRIVATE
            )

        val cascadeFile =
            File(
                cascadeDir,
                "haarcascade_frontalface_default.xml"
            )

        cascadeFile.outputStream().use {
            input.copyTo(it)
        }

        faceCascade =
            CascadeClassifier(
                cascadeFile.absolutePath
            )
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(
        imageProxy: ImageProxy
    ) {

        try {

            val rotationDegrees =
                imageProxy.imageInfo.rotationDegrees

            var bitmap =
                imageProxy.toBitmap()

            if (rotationDegrees != 0) {

                val matrix = Matrix().apply {
                    postRotate(
                        rotationDegrees.toFloat()
                    )
                }

                bitmap =
                    Bitmap.createBitmap(
                        bitmap,
                        0,
                        0,
                        bitmap.width,
                        bitmap.height,
                        matrix,
                        true
                    )
            }

            val allPoints =
                mutableListOf<PointF>()

            val allBounds =
                mutableListOf<RectF>()

            val rgba = Mat()
            Utils.bitmapToMat(
                bitmap,
                rgba
            )

            val gray = Mat()

            Imgproc.cvtColor(
                rgba,
                gray,
                Imgproc.COLOR_RGBA2GRAY
            )

            val faces = MatOfRect()

            faceCascade.detectMultiScale(
                gray,
                faces,
                1.2,
                5,
                0,
                Size(80.0, 80.0),
                Size()
            )

            for (face: Rect in faces.toArray()) {

                val left =
                    face.x.coerceAtLeast(0)

                val top =
                    face.y.coerceAtLeast(0)

                val right =
                    (face.x + face.width)
                        .coerceAtMost(bitmap.width)

                val bottom =
                    (face.y + face.height)
                        .coerceAtMost(bitmap.height)

                val width =
                    right - left

                val height =
                    bottom - top

                if (width <= 0 || height <= 0)
                    continue

                allBounds.add(
                    RectF(
                        left.toFloat(),
                        top.toFloat(),
                        right.toFloat(),
                        bottom.toFloat()
                    )
                )

                val crop =
                    Bitmap.createBitmap(
                        bitmap,
                        left,
                        top,
                        width,
                        height
                    )

                val pred =
                    model.predict(crop)
                Log.d(
                    "KP",
                    pred.take(10).joinToString()
                )

                for (i in pred.indices step 2) {

                    val px =
                        left +
                                pred[i] *
                                width /
                                96f

                    val py =
                        top +
                                pred[i + 1] *
                                height /
                                96f

                    allPoints.add(
                        PointF(
                            px,
                            py
                        )
                    )
                }
            }

            overlay.update(
                allPoints,
                allBounds,
                bitmap.width,
                bitmap.height
            )

            rgba.release()
            gray.release()
            faces.release()

        } finally {
            imageProxy.close()
        }
    }
}