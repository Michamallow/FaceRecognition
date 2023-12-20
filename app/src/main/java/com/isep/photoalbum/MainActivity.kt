package com.isep.photoalbum

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.isep.photoalbum.import_scan.CameraActivity
import com.isep.photoalbum.import_scan.PopupDialog
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.util.*

class MainActivity : AppCompatActivity() {
    private val REQUEST_CODE_IMPORT_FILE = 123
    private val importedFiles = ArrayList<File>() // List to track imported files
    private var importFileName: String? = null
    private var imagesPerLine = 3 // Default number of images per line

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val scanImportButton = findViewById<Button>(R.id.scanImportButton)
        scanImportButton.setOnClickListener { showPopupDialog() }

        loadImportedFiles()
    }

    private fun showPopupDialog() {
        val dialog = PopupDialog(this, object : PopupDialog.OnOptionSelectedListener {
            override fun onOptionSelected(fileName: String, option: PopupDialog.Option) {
                if (option == PopupDialog.Option.SCAN) {
                    // Start the CameraActivity to capture a photo
                    val intent = Intent(this@MainActivity, CameraActivity::class.java)
                    intent.putExtra("file_name", fileName)
                    startActivity(intent)
                } else if (option == PopupDialog.Option.IMPORT) {
                    importFileName = fileName // Store the file name
                    openFilePicker()
                }
            }
        })
        dialog.show()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*" // Allow all file types for import
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        startActivityForResult(intent, REQUEST_CODE_IMPORT_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_IMPORT_FILE && resultCode == RESULT_OK) {
            if (data != null) {
                val selectedFileUri: Uri? = data.data

                if (selectedFileUri != null) {
                    try {
                        // Use the provided file name or generate a unique name
                        var fileName = importFileName
                        if (fileName.isNullOrEmpty()) {
                            fileName = generateUniqueFileName()
                        }

                        // Create the "ImportedDocuments" folder if it doesn't exist
                        val folder = File(filesDir, "ImportedDocuments")
                        if (!folder.exists()) {
                            folder.mkdirs()
                        }

                        // Create a new File object with the destination path
                        val destinationFile = File(folder, fileName)

                        // Copy the imported file to the destination
                        val inputStream: InputStream? = contentResolver.openInputStream(selectedFileUri)
                        val outputStream: OutputStream = Files.newOutputStream(destinationFile.toPath())
                        val buffer = ByteArray(1024)
                        var length: Int
                        while (inputStream?.read(buffer).also { length = it!! } != -1) {
                            outputStream.write(buffer, 0, length)
                        }
                        inputStream?.close()
                        outputStream.close()

                        // Clear the stored import file name
                        importFileName = null

                        importedFiles.add(destinationFile)
                        updateImportedFilesView()

                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        } else if (resultCode == RESULT_OK) {
            if (data != null) {
                val capturedPhotoPath = data.getStringExtra("captured_photo_path")

                if (!capturedPhotoPath.isNullOrEmpty()) {
                    importedFiles.add(File(capturedPhotoPath))
                }
                updateImportedFilesView()
            }
        }
    }

    // Load and display imported files when the app starts
    private fun loadImportedFiles() {
        val folder = File(filesDir, "ImportedDocuments")
        val files = folder.listFiles()

        if (files != null) {
            importedFiles.addAll(files.toList())
        }

        updateImportedFilesView()
    }

    private fun updateImportedFilesView() {
        val documentList = findViewById<LinearLayout>(R.id.DocumentList)
        documentList.removeAllViews() // Clear the existing views

        // Create a layout for the buttons to modify the number of images per line
        val buttonLayout = LinearLayout(this)
        buttonLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        buttonLayout.orientation = LinearLayout.HORIZONTAL

        // Button to set 1 image per line
        val oneImageButton = Button(this)
        oneImageButton.text = "1"
        oneImageButton.setOnClickListener {
            imagesPerLine = 1
            updateImportedFilesView()
        }

        // Button to set 3 images per line
        val threeImagesButton = Button(this)
        threeImagesButton.text = "3"
        threeImagesButton.setOnClickListener {
            imagesPerLine = 3
            updateImportedFilesView()
        }

        // Button to set 6 images per line
        val sixImagesButton = Button(this)
        sixImagesButton.text = "6"
        sixImagesButton.setOnClickListener {
            imagesPerLine = 6
            updateImportedFilesView()
        }

        buttonLayout.addView(oneImageButton)
        buttonLayout.addView(threeImagesButton)
        buttonLayout.addView(sixImagesButton)

        documentList.addView(buttonLayout)

        var currentLineLayout: LinearLayout? = null

        for ((index, file) in importedFiles.withIndex()) {
            if (file.isFile) {
                if (index % imagesPerLine == 0) {
                    // Create a new LinearLayout for each row
                    currentLineLayout = LinearLayout(this)
                    currentLineLayout.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    currentLineLayout.orientation = LinearLayout.HORIZONTAL
                    documentList.addView(currentLineLayout)
                }

                val fileLayout = LinearLayout(this)
                fileLayout.layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f / imagesPerLine
                )
                fileLayout.gravity = Gravity.CENTER
                fileLayout.orientation = LinearLayout.VERTICAL // Set orientation to vertical

                val imageView = ImageView(this)
                imageView.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                imageView.adjustViewBounds = true
                imageView.scaleType = ImageView.ScaleType.FIT_CENTER

                // Load the image using BitmapFactory
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                imageView.setImageBitmap(bitmap)

                val xButton = Button(this)
                xButton.text = "X"
                xButton.setOnClickListener {
                    removeFile(file)
                }

                fileLayout.addView(imageView)
                fileLayout.addView(xButton)

                currentLineLayout?.addView(fileLayout)
            }
        }
    }

    // Method to remove a file from app storage
    private fun removeFile(file: File) {
        if (file.exists()) {
            if (file.delete()) {
                importedFiles.remove(file)
                updateImportedFilesView()
            } else {
                Toast.makeText(this, "File couldn't be removed", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Generate a unique file name if the user didn't specify one
    private fun generateUniqueFileName(): String {
        return "imported_file_${System.currentTimeMillis()}"
    }
}