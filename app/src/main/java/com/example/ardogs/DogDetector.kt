package com.example.ardogs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import ai.onnxruntime.*
import java.nio.FloatBuffer

data class Detection(
    val bbox: RectF,
    val confidence: Float,
    val classId: Int,
    val className: String
)

data class DetectionResult(
    val detections: List<Detection>,
    val inferenceTimeMs: Long,
    val preprocessTimeMs: Long,
    val postprocessTimeMs: Long
)

class DogDetector(context: Context) {
    private val ortEnv = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    // Use dynamic input dimensions read from the model when possible
    // Default to 320 which is common for YOLO-n models; will try to override from model
    private var inputWidth = 320
    private var inputHeight = 320
    // If model provides static expected dims, record them here
    private var modelExpectedWidth: Int? = null
    private var modelExpectedHeight: Int? = null
    private val confidenceThreshold = 0.35f  // Increase threshold to filter out large low-confidence detections
    private val iouThreshold = 0.45f

    // Bounding box tightening factors - shrink bbox to focus on dog body/face only
    // Width: 0.80 means shrink by 20% (10% from each side - left/right)
    // Height: 0.70 means shrink by 30% (15% from each side - top/bottom)
    private val bboxTighteningFactorWidth = 1f
    private val bboxTighteningFactorHeight = 1f

    // Daftar 15 ras anjing yang dilatih dalam model (alphabetical order)
    private val classNames = listOf(
        "Beagle",
        "Chihuahua",
        "Doberman",
        "French_bulldog",
        "German_shepherd",
        "Golden_retriever",
        "Labrador_retriever",
        "Maltese_dog",
        "Pomeranian",
        "Pug",
        "Rottweiler",
        "Samoyed",
        "Shih-Tzu",
        "Siberian_husky",
        "Standard_poodle"
    )

    init {
        val modelBytes = context.assets.open("yolo11n_best.onnx").readBytes()
        session = ortEnv.createSession(modelBytes)
        try {
            // Read model input shape if available: expected shape [N,C,H,W]
            val inputName = session.inputNames.iterator().next()
            val nodeInfo = session.inputInfo[inputName]
            android.util.Log.d("DogDetector", "Input node info: $nodeInfo")
            val tensorInfo = nodeInfo?.info as? TensorInfo
            val shape = tensorInfo?.shape
            android.util.Log.d("DogDetector", "Raw model input shape: ${shape?.joinToString()}")
            if (shape != null && shape.size >= 4) {
                // shape[2] = height, shape[3] = width
                val h = shape[2]
                val w = shape[3]
                android.util.Log.d("DogDetector", "Model reported H=$h W=$w")
                if (h > 0 && w > 0) {
                    inputHeight = h.toInt()
                    inputWidth = w.toInt()
                    modelExpectedHeight = h.toInt()
                    modelExpectedWidth = w.toInt()
                } else {
                    // If model used -1 for dynamic dims, keep defaults (320) but log
                    android.util.Log.w("DogDetector", "Model has dynamic input dimensions; using defaults ${inputWidth}x${inputHeight}")
                }
            } else {
                android.util.Log.w("DogDetector", "Cannot read model input shape; using defaults ${inputWidth}x${inputHeight}")
            }
        } catch (e: Exception) {
            android.util.Log.w("DogDetector", "Could not read model input shape, using defaults: ${inputWidth}x${inputHeight}", e)
        }
        android.util.Log.d("DogDetector", "Model input expected size: ${inputWidth}x${inputHeight} (modelExpected=${modelExpectedWidth}x${modelExpectedHeight})")
    }

    // Padding info - set to 0 since we're not using letterbox
    private var padLeft = 0f
    private var padTop = 0f
    private var scaleX = 1f
    private var scaleY = 1f

    fun detect(bitmap: Bitmap): DetectionResult {
        val totalStartTime = System.currentTimeMillis()

        // Preprocess image - simple resize without letterbox
        val preprocessStart = System.currentTimeMillis()
        val targetW = modelExpectedWidth ?: inputWidth
        val targetH = modelExpectedHeight ?: inputHeight

        android.util.Log.d("DogDetector", "Resizing bitmap from ${bitmap.width}x${bitmap.height} to ${targetW}x${targetH}")

        // Simple resize to target size
        @Suppress("DEPRECATION")
        val resized = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)

        // Calculate scale factors for coordinate conversion
        scaleX = bitmap.width.toFloat() / targetW
        scaleY = bitmap.height.toFloat() / targetH

        var inputBuffer = preprocessImage(resized)
        var preprocessTimeMs = System.currentTimeMillis() - preprocessStart

        android.util.Log.d("DogDetector", "Preprocess: ${preprocessTimeMs}ms (scaleX=$scaleX, scaleY=$scaleY)")

        // Run inference (with one retry if ORT complains about dimensions)
        var inferenceTimeMs: Long
        var outputTensorRaw: Any? = null
        var hadError = false
        try {
            val inferenceStart = System.currentTimeMillis()
            val inputName = session.inputNames.iterator().next()
            // Use the shape matching the buffer we prepared
            val tensorH = (modelExpectedHeight ?: inputHeight).toLong()
            val tensorW = (modelExpectedWidth ?: inputWidth).toLong()
            val shape = longArrayOf(1, 3, tensorH, tensorW)
            android.util.Log.d("DogDetector", "Creating tensor with shape: ${shape.joinToString()}")
            android.util.Log.d("DogDetector", "Input buffer capacity=${inputBuffer.capacity()} expected=${3 * tensorH * tensorW}")
            val tensor = OnnxTensor.createTensor(ortEnv, inputBuffer, shape)

            val results = session.run(mapOf(inputName to tensor))
            @Suppress("UNCHECKED_CAST")
            outputTensorRaw = results[0].value
            inferenceTimeMs = System.currentTimeMillis() - inferenceStart

            tensor.close()
            results.close()

        } catch (e: OrtException) {
            // Parse expected dims from error message if present and retry once
            android.util.Log.e("DogDetector", "ORT exception during inference: ${e.message}")
            hadError = true
            inferenceTimeMs = -1

            val msg = e.message ?: ""
            try {
                // Look for "index: 2 Got: <got2> Expected: <exp2>" and index 3
                val regex = Regex("index:\\s*2\\s*Got:\\s*(\\d+)\\s*Expected:\\s*(\\d+).*index:\\s*3\\s*Got:\\s*(\\d+)\\s*Expected:\\s*(\\d+)", RegexOption.DOT_MATCHES_ALL)
                val match = regex.find(msg)
                if (match != null && match.groupValues.size >= 5) {
                    val expH = match.groupValues[2].toInt()
                    val expW = match.groupValues[4].toInt()
                    android.util.Log.w("DogDetector", "Detected expected dimensions from ORT error: H=$expH W=$expW. Will retry once.")

                    // Update input dims and re-preprocess
                    inputHeight = expH
                    inputWidth = expW
                    modelExpectedHeight = expH
                    modelExpectedWidth = expW
                    android.util.Log.d("DogDetector", "Retry: Resizing bitmap to: ${expW}x${expH}")
                    @Suppress("DEPRECATION")
                    val resized2 = Bitmap.createScaledBitmap(bitmap, expW, expH, true)
                    android.util.Log.d("DogDetector", "Retry: Resized bitmap actual: ${resized2.width}x${resized2.height}")
                    inputBuffer = preprocessImage(resized2)
                    preprocessTimeMs = System.currentTimeMillis() - preprocessStart

                    // Retry inference once
                    try {
                        val inferenceStart2 = System.currentTimeMillis()
                        val inputName2 = session.inputNames.iterator().next()
                        val shape2 = longArrayOf(1, 3, expH.toLong(), expW.toLong())
                        android.util.Log.d("DogDetector", "Retry creating tensor with shape: ${shape2.joinToString()}")
                        android.util.Log.d("DogDetector", "Retry input buffer capacity=${inputBuffer.capacity()} expected=${3 * expH * expW}")
                        val tensor2 = OnnxTensor.createTensor(ortEnv, inputBuffer, shape2)
                        val results2 = session.run(mapOf(inputName2 to tensor2))
                        @Suppress("UNCHECKED_CAST")
                        outputTensorRaw = results2[0].value
                        inferenceTimeMs = System.currentTimeMillis() - inferenceStart2
                        tensor2.close()
                        results2.close()
                        hadError = false
                        android.util.Log.d("DogDetector", "Retry inference success with ${inputWidth}x${inputHeight}")
                    } catch (e2: Exception) {
                        android.util.Log.e("DogDetector", "Retry failed: ${e2.message}", e2)
                        hadError = true
                    }
                }
            } catch (parseEx: Exception) {
                android.util.Log.e("DogDetector", "Failed to parse ORT error message: ${parseEx.message}", parseEx)
            }
        } catch (e: Exception) {
            android.util.Log.e("DogDetector", "Unexpected exception during inference: ${e.message}", e)
            inferenceTimeMs = -1
            hadError = true
        }

        if (hadError || outputTensorRaw == null) {
            android.util.Log.w("DogDetector", "Inference failed — returning empty DetectionResult")
            return DetectionResult(emptyList(), inferenceTimeMs, preprocessTimeMs, 0)
        }

        @Suppress("UNCHECKED_CAST")
        val outputTensor = outputTensorRaw as Array<Array<FloatArray>>

        android.util.Log.d("DogDetector", "Inference: ${inferenceTimeMs}ms")

        // Postprocess
        val postprocessStart = System.currentTimeMillis()
        val detections = postprocess(outputTensor, bitmap.width, bitmap.height)
        val postprocessTimeMs = System.currentTimeMillis() - postprocessStart

        android.util.Log.d("DogDetector", "Postprocess: ${postprocessTimeMs}ms")

        val totalTimeMs = System.currentTimeMillis() - totalStartTime
        android.util.Log.d("DogDetector", "Total detection time: ${totalTimeMs}ms | Detections: ${detections.size}")

        return DetectionResult(
            detections = detections,
            inferenceTimeMs = inferenceTimeMs,
            preprocessTimeMs = preprocessTimeMs,
            postprocessTimeMs = postprocessTimeMs
        )
    }

    private fun preprocessImage(bitmap: Bitmap): FloatBuffer {
        val w = bitmap.width
        val h = bitmap.height
        val buffer = FloatBuffer.allocate(3 * w * h)
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        // Normalize to [0, 1] and convert to CHW format (Channel, Height, Width)
        for (c in 0..2) {
            for (i in pixels.indices) {
                val pixel = pixels[i]
                val value = when(c) {
                    0 -> ((pixel shr 16) and 0xFF) / 255f // R
                    1 -> ((pixel shr 8) and 0xFF) / 255f  // G
                    else -> (pixel and 0xFF) / 255f       // B
                }
                buffer.put(value)
            }
        }
        buffer.rewind()
        return buffer
    }

    private fun postprocess(output: Array<Array<FloatArray>>, imgWidth: Int, imgHeight: Int): List<Detection> {
        val detections = mutableListOf<Detection>()

        android.util.Log.d("DogDetector", "Output shape: [${output.size}, ${output[0].size}, ${output[0][0].size}]")

        // YOLO output format bisa 2 macam:
        // Format 1 (transposed): [1, 19, 2100] - 19 = (4 bbox + 15 classes), 2100 = predictions
        // Format 2 (standard): [1, 2100, 19] - 2100 = predictions, 19 = (4 bbox + 15 classes)

        val dim1 = output[0].size      // bisa 19 atau 2100
        val dim2 = output[0][0].size   // bisa 2100 atau 19

        // Deteksi format mana yang digunakan
        val isTransposed = dim1 < dim2  // jika dim1 (19) < dim2 (2100), berarti transposed

        val numPredictions: Int
        val numClasses: Int

        if (isTransposed) {
            // Format [1, 19, 2100] - transposed
            numClasses = dim1 - 4
            numPredictions = dim2
            android.util.Log.d("DogDetector", "Detected TRANSPOSED format: [1, $dim1, $dim2]")
        } else {
            // Format [1, 2100, 19] - standard
            numPredictions = dim1
            numClasses = dim2 - 4
            android.util.Log.d("DogDetector", "Detected STANDARD format: [1, $dim1, $dim2]")
        }

        android.util.Log.d("DogDetector", "Num predictions: $numPredictions, Num classes: $numClasses")

        for (i in 0 until numPredictions) {
            // Get bbox coordinates and class probabilities based on format
            val xCenterRaw: Float
            val yCenterRaw: Float
            val widthRaw: Float
            val heightRaw: Float
            var maxProb = 0f
            var maxClassId = 0

            if (isTransposed) {
                // Format [1, 19, 2100] - data untuk prediction ke-i ada di kolom ke-i
                xCenterRaw = output[0][0][i]
                yCenterRaw = output[0][1][i]
                widthRaw = output[0][2][i]
                heightRaw = output[0][3][i]

                // Find class with max probability
                for (c in 0 until numClasses) {
                    val prob = output[0][4 + c][i]
                    if (prob > maxProb) {
                        maxProb = prob
                        maxClassId = c
                    }
                }
            } else {
                // Format [1, 2100, 19] - data untuk prediction ke-i ada di row ke-i
                val prediction = output[0][i]
                xCenterRaw = prediction[0]
                yCenterRaw = prediction[1]
                widthRaw = prediction[2]
                heightRaw = prediction[3]

                // Find class with max probability
                for (c in 0 until numClasses) {
                    val prob = prediction[4 + c]
                    if (prob > maxProb) {
                        maxProb = prob
                        maxClassId = c
                    }
                }
            }

            // Correct coordinates: scale from model space (320x320) to original image
            // Coordinates from model are in resized space - just apply scale factor
            val xCenter = xCenterRaw * scaleX
            val yCenter = yCenterRaw * scaleY
            val width = widthRaw * scaleX
            val height = heightRaw * scaleY

            // Filter by confidence threshold
            if (maxProb > confidenceThreshold) {
                // Apply asymmetric bounding box tightening
                // Width: shrink by 30% (keep current - already good)
                // Height: shrink by 40% (more aggressive to focus on body/face)
                val tightenedWidth = width * bboxTighteningFactorWidth
                val tightenedHeight = height * bboxTighteningFactorHeight

                // Center remains the same, but bbox is smaller
                val left = xCenter - tightenedWidth / 2
                val top = yCenter - tightenedHeight / 2
                val right = xCenter + tightenedWidth / 2
                val bottom = yCenter + tightenedHeight / 2

                // Calculate bounding box size relative to image
                val bboxArea = tightenedWidth * tightenedHeight
                val imageArea = imgWidth * imgHeight
                val areaRatio = bboxArea / imageArea

                // Filter out bounding boxes that are too large (likely false positives)
                // Skip boxes that cover more than 85% of the image
                if (areaRatio > 0.85f) {
                    if (detections.size < 3) {
                        android.util.Log.d("DogDetector", "Detection $i REJECTED: bbox too large (${(areaRatio * 100).toInt()}% of image)")
                    }
                    continue
                }

                val bbox = RectF(left, top, right, bottom)
                val className = if (maxClassId < classNames.size) {
                    classNames[maxClassId]
                } else {
                    "Dog_$maxClassId"
                }

                // Log raw dan corrected values untuk debugging
                if (detections.size < 3) { // Log hanya 3 deteksi pertama
                    android.util.Log.d("DogDetector", "Detection $i RAW: xc=$xCenterRaw yc=$yCenterRaw w=$widthRaw h=$heightRaw")
                    android.util.Log.d("DogDetector", "Detection $i CORRECTED: xc=$xCenter yc=$yCenter w=$width h=$height")
                    android.util.Log.d("DogDetector", "Detection $i TIGHTENED: w=$tightenedWidth h=$tightenedHeight (width: ${(bboxTighteningFactorWidth * 100).toInt()}%, height: ${(bboxTighteningFactorHeight * 100).toInt()}%)")
                    android.util.Log.d("DogDetector", "Detection $i SIZE: ${(areaRatio * 100).toInt()}% of image area")
                    android.util.Log.d("DogDetector", "Detection $i FINAL: class=$className conf=${"%.2f".format(maxProb)} bbox=(${left.toInt()}, ${top.toInt()}, ${right.toInt()}, ${bottom.toInt()})")
                }

                detections.add(Detection(bbox, maxProb, maxClassId, className))
            }
        }

        android.util.Log.d("DogDetector", "Total detections before NMS: ${detections.size}")

        // Apply Non-Maximum Suppression
        val nmsResult = nms(detections)
        android.util.Log.d("DogDetector", "Total detections after NMS: ${nmsResult.size}")

        return nmsResult
    }

    private fun nms(detections: List<Detection>): List<Detection> {
        if (detections.isEmpty()) return emptyList()

        // Sort by confidence
        val sortedDetections = detections.sortedByDescending { it.confidence }
        val result = mutableListOf<Detection>()
        val suppressed = BooleanArray(sortedDetections.size)

        for (i in sortedDetections.indices) {
            if (suppressed[i]) continue

            result.add(sortedDetections[i])

            for (j in i + 1 until sortedDetections.size) {
                if (suppressed[j]) continue

                val iou = calculateIoU(sortedDetections[i].bbox, sortedDetections[j].bbox)
                if (iou > iouThreshold) {
                    suppressed[j] = true
                }
            }
        }

        return result
    }

    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val intersectLeft = maxOf(box1.left, box2.left)
        val intersectTop = maxOf(box1.top, box2.top)
        val intersectRight = minOf(box1.right, box2.right)
        val intersectBottom = minOf(box1.bottom, box2.bottom)

        val intersectWidth = maxOf(0f, intersectRight - intersectLeft)
        val intersectHeight = maxOf(0f, intersectBottom - intersectTop)
        val intersectArea = intersectWidth * intersectHeight

        val box1Area = box1.width() * box1.height()
        val box2Area = box2.width() * box2.height()
        val unionArea = box1Area + box2Area - intersectArea

        return if (unionArea > 0) intersectArea / unionArea else 0f
    }

    fun close() {
        session.close()
    }
}
