package ru.chezzz.denisfacedetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceAnalyzer(
    context: Context,
    private val overlay: FaceOverlayView
) : ImageAnalysis.Analyzer {

    private val detector =
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(
                    FaceDetectorOptions.PERFORMANCE_MODE_FAST
                )
                .build()
        )

    private val model =
        KeypointModel(context)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(
        imageProxy: ImageProxy
    ) {
        val mediaImage =
            imageProxy.image ?: run {
                imageProxy.close()
                return
            }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        
        // 1. Convert ImageProxy to Bitmap
        var bitmap = imageProxy.toBitmap()

        // 2. Rotate Bitmap to be upright (match what the user sees in the preview)
        if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        // 3. Process the upright bitmap with ML Kit
        // Since we manually rotated the bitmap, we pass 0 as the rotation degree
        val image = InputImage.fromBitmap(bitmap, 0)

        detector.process(image)
            .addOnSuccessListener { faces ->
                val allPoints = mutableListOf<PointF>()
                val allBounds = mutableListOf<RectF>()

                if (faces.isEmpty()) {
                    overlay.update(emptyList(), emptyList(), bitmap.width, bitmap.height)
                    return@addOnSuccessListener
                }

                faces.forEach { face ->
                    val box = face.boundingBox
                    val rectF = RectF(box)
                    allBounds.add(rectF)

                    // Ensure the crop area is within the bitmap bounds
                    val left = box.left.coerceAtLeast(0)
                    val top = box.top.coerceAtLeast(0)
                    val right = box.right.coerceAtMost(bitmap.width)
                    val bottom = box.bottom.coerceAtMost(bitmap.height)
                    
                    val width = right - left
                    val height = bottom - top

                    if (width > 0 && height > 0) {
                        // 4. Crop the face for the TFLite keypoint model
                        val crop = Bitmap.createBitmap(bitmap, left, top, width, height)
                        val pred = model.predict(crop)

                        // 5. Scale the 96x96 keypoints to the actual face bounding box coordinates
                        // The model returns 30 values (15 points x,y) or 136 values (68 points x,y) etc.
                        // Assuming the model outputs coordinates in the 0..96 range for a 96x96 input.
                        for (i in pred.indices step 2) {
                            val px = left + (pred[i] * width / 96f)
                            val py = top + (pred[i + 1] * height / 96f)
                            allPoints.add(PointF(px, py))
                        }
                    }
                }

                // 6. Update the overlay with points, boxes, and the upright image dimensions
                overlay.update(allPoints, allBounds, bitmap.width, bitmap.height)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}