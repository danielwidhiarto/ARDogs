package com.example.ardogs

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ardogs.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var detector: DogDetector? = null

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var isProcessing = false

    // Fun fact management
    private var currentBreed: String? = null
    private var currentFunFactIndex = 0
    private var lastFunFactChangeTime = 0L
    private val funFactChangeIntervalMs = 5000L // Change fun fact every 5 seconds

    companion object {
        private const val TAG = "ARDogs"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize detector
        binding.tvStatusLog.setText(R.string.status_initializing)
        try {
            detector = DogDetector(this)
            Log.d(TAG, "DogDetector initialized successfully")
            binding.tvStatusLog.setText(R.string.status_ready)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing detector: ${e.message}", e)
            Toast.makeText(this, getString(R.string.model_loading_failed), Toast.LENGTH_LONG).show()
            // Don't finish - let user see the error message
            binding.btnToggleDetection.isEnabled = false
            binding.tvTopDetection.setText(R.string.model_loading_failed)
            binding.tvStatusLog.text = "Error: ${e.message}"
            return
        }

        // Check permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Setup UI
        binding.btnToggleDetection.setOnClickListener {
            isProcessing = !isProcessing
            binding.btnToggleDetection.setText(if (isProcessing) R.string.stop_detection else R.string.start_detection)
            if (!isProcessing) {
                binding.overlayView.setDetections(emptyList())
                binding.tvStatusLog.setText(R.string.status_ready)
                binding.tvPerformanceMetrics.text = ""
                binding.funFactPanel.visibility = android.view.View.GONE
                currentBreed = null
            }
        }

        // Set initial button text from resources
        binding.btnToggleDetection.setText(R.string.start_detection)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                    }

                // Image analyzer
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, DogAnalyzer())
                    }

                // Select back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

                Log.d(TAG, "Camera started successfully")

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private inner class DogAnalyzer : ImageAnalysis.Analyzer {

        private var frameCounter = 0
        private val processEveryNFrames = 3 // Process every 3rd frame for performance
        private var lastFrameTimeMs = System.currentTimeMillis()
        private var frameTimings = mutableListOf<Long>()
        private var inferenceTimings = mutableListOf<Long>()

        override fun analyze(imageProxy: ImageProxy) {
            if (!isProcessing || detector == null) {
                imageProxy.close()
                return
            }

            frameCounter++
            if (frameCounter % processEveryNFrames != 0) {
                imageProxy.close()
                return
            }

            val frameStartTime = System.currentTimeMillis()

            scope.launch(Dispatchers.Default) {
                try {
                    // Convert ImageProxy to Bitmap (handles rotation internally)
                    val bitmap = ImageUtils.imageProxyToBitmap(imageProxy)
                    Log.d(TAG, "Frame #$frameCounter: Bitmap size: ${bitmap.width}x${bitmap.height}")

                    // Detect dogs - detector will scale coordinates to bitmap size
                    val result = detector?.detect(bitmap) ?: return@launch
                    val detections = result.detections

                    val frameTimeMs = System.currentTimeMillis() - frameStartTime

                    // Track timing statistics
                    frameTimings.add(frameTimeMs)
                    inferenceTimings.add(result.inferenceTimeMs)

                    // Keep only last 30 frames for averaging
                    if (frameTimings.size > 30) {
                        frameTimings.removeAt(0)
                        inferenceTimings.removeAt(0)
                    }

                    val avgFrameTime = frameTimings.average().toLong()
                    val avgInferenceTime = inferenceTimings.average().toLong()
                    val fps = if (avgFrameTime > 0) 1000.0 / avgFrameTime else 0.0

                    Log.d(
                        TAG,
                        "Detection #$frameCounter: ${detections.size} dogs found | " +
                                "Frame: ${frameTimeMs}ms | Inference: ${result.inferenceTimeMs}ms | " +
                                "Preprocess: ${result.preprocessTimeMs}ms | Postprocess: ${result.postprocessTimeMs}ms | " +
                                "Avg FPS: ${"%.1f".format(fps)}"
                    )

                    // Update UI
                    withContext(Dispatchers.Main) {
                        binding.overlayView.setDetections(detections, bitmap.width, bitmap.height)
                        binding.tvDetectionCount.setText(
                            getString(R.string.detected_dogs, detections.size)
                        )

                        // Status log
                        binding.tvStatusLog.text = if (detections.isNotEmpty()) {
                            "Processing... Found ${detections.size} dog(s)"
                        } else {
                            "Scanning for dogs..."
                        }

                        // Performance metrics
                        binding.tvPerformanceMetrics.text = getString(
                            R.string.status_results,
                            avgFrameTime,
                            avgInferenceTime,
                            fps
                        )

                        if (detections.isNotEmpty()) {
                            val topDetection = detections.maxByOrNull { it.confidence }
                            topDetection?.let {
                                val confidence = (it.confidence * 100).toInt()
                                binding.tvTopDetection.text = getString(
                                    R.string.top_detection,
                                    it.className.replace("_", " "),
                                    confidence
                                )

                                Log.d(
                                    TAG,
                                    "Top detection: ${it.className} (${it.confidence * 100}%) at (${it.bbox.centerX()}, ${it.bbox.centerY()})"
                                )

                                // Show fun facts for AR immersive experience
                                updateFunFacts(it.className)
                            }
                        } else {
                            binding.tvTopDetection.setText(R.string.no_dogs_detected)
                            // Hide fun fact panel when no dog detected
                            binding.funFactPanel.visibility = android.view.View.GONE
                            currentBreed = null
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error during detection: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        binding.tvStatusLog.text = "Error: ${e.message}"
                    }
                } finally {
                    imageProxy.close()
                }
            }
        }
    }

    private fun updateFunFacts(breedName: String) {
        val currentTime = System.currentTimeMillis()

        // If breed changed, reset to first fun fact
        if (breedName != currentBreed) {
            currentBreed = breedName
            currentFunFactIndex = 0
            lastFunFactChangeTime = currentTime
        }
        // If same breed but enough time has passed, cycle to next fun fact
        else if (currentTime - lastFunFactChangeTime > funFactChangeIntervalMs) {
            val breedInfo = DogBreedDatabase.getBreedInfo(breedName)
            if (breedInfo != null) {
                currentFunFactIndex = (currentFunFactIndex + 1) % breedInfo.funFacts.size
                lastFunFactChangeTime = currentTime
            }
        }

        // Display breed information with fun facts
        val breedInfo = DogBreedDatabase.getBreedInfo(breedName)
        if (breedInfo != null) {
            binding.funFactPanel.visibility = android.view.View.VISIBLE

            // Display current fun fact
            if (breedInfo.funFacts.isNotEmpty()) {
                binding.tvFunFact.text = breedInfo.funFacts[currentFunFactIndex]
            }

            // Display breed origin and temperament
            binding.tvBreedOrigin.text = "📍 Origin: ${breedInfo.origin} | ${breedInfo.size}"

            // Add subtle animation when fact changes
            if (currentTime - lastFunFactChangeTime < 300) { // Just changed
                binding.tvFunFact.alpha = 0f
                binding.tvFunFact.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
        } else {
            binding.funFactPanel.visibility = android.view.View.GONE
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.camera_permission_required),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        cameraExecutor.shutdown()
        detector?.close()
    }
}