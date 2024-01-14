package com.isep.facerecognition.import_scan

import android.Manifest
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.Surface
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.isep.facerecognition.R
import java.io.File
import java.util.Date
import java.util.Locale

class CameraActivity : AppCompatActivity() {
    private lateinit var cameraPreviewView: androidx.camera.view.PreviewView
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var captureButton: ImageButton
    private lateinit var imageCapture: ImageCapture // Store the ImageCapture use case

    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private var capturedPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        cameraPreviewView = findViewById(R.id.cameraPreview)
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        captureButton = findViewById(R.id.captureButton)

        captureButton.setOnClickListener {
            capturePhoto()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            initCamera()
        }

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(cameraProvider)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun initCamera() {
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                // Create the ImageCapture use case
                imageCapture = ImageCapture.Builder()
                    .setTargetRotation(Surface.ROTATION_0)
                    .build()

                // Now that the camera is bound, you can proceed with binding the preview and imageCapture
                bindCameraUseCases(cameraProvider)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases(@NonNull cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.setSurfaceProvider(cameraPreviewView.surfaceProvider)

        // Bind both preview and imageCapture to the camera
        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview, imageCapture)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initCamera()
            } else {
                Toast.makeText(this@CameraActivity, "Camera permission not granted.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun capturePhoto() {
        var fileName = "";
        if(intent.getStringExtra("file_name")!=null){
            fileName = intent.getStringExtra("file_name")!!;
        }else{
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRANCE)
            fileName = "IMG_" + sdf.format(Date()) + ".jpg"
        }

        val folder = File(filesDir, "ImportedDocuments")
        if (!folder.exists()) {
            folder.mkdirs()
        }

        val destinationFile = File(folder, fileName)
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(destinationFile).build()
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                capturedPhotoPath = destinationFile.absolutePath
                returnToMainActivityWithPhoto()
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(this@CameraActivity, "Failed to capture photo: " + exception.message, Toast.LENGTH_LONG).show()
                exception.printStackTrace()
            }
        })
    }

    private fun returnToMainActivityWithPhoto() {
        val intent = intent
        intent.putExtra("captured_photo_path", capturedPhotoPath)
        setResult(RESULT_OK, intent)
        finish()
    }
}