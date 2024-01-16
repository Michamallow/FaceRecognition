package com.isep.facerecognition

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.util.ArrayList
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class MeasurementActivity : AppCompatActivity() {
    private val TOLERANCE: Double = 150.0
    private val FILE_NAME = "face_ratios.txt"

    private val measurementPoints = mutableListOf<Pair<Float, Float>>()
    private val registeredFaceRatios = mutableMapOf<String, Float>()
    private val orderedPoints = listOf("E1", "E2", "SFR", "SFL", "BN", "SNR", "SNL", "TL", "BL", "RL", "LL", "BC")
    private var currentPointIndex = 0


    private val instructionTextView: TextView by lazy {
        findViewById(R.id.instructionTextView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_measurement)

        val measurementImageView: ImageView = findViewById(R.id.measurementPhotoView)

        updateInstructionText()

        measurementImageView.setOnTouchListener { _, event ->
            handleTouchEvent(event)
            true // Retourne true pour indiquer que l'événement a été traité
        }

        val btnRecognize: Button = findViewById(R.id.btnRecognize)
        btnRecognize.setOnClickListener { onRecognizeClick() }

        val btnSaveProfile: Button = findViewById(R.id.btnSaveProfile)
        btnSaveProfile.setOnClickListener { onSaveProfileClick() }
    }

    fun onLoadImageClick(view: View) {
        val folder = File(filesDir, "ImportedDocuments")
        val files = folder.listFiles()

        if (files != null && files.isNotEmpty()) {
            showImageSelectionDialog(files)
            // Réinitialisez la page après le chargement
            measurementPoints.clear()
            registeredFaceRatios.clear()
            currentPointIndex = 0


            // Suppression des points visuels de l'image précédente
            val measurementLayout: ViewGroup = findViewById(R.id.measurementLayout)
            measurementLayout.removeAllViews()

            updateInstructionText()
        } else {
            showToast("Aucune image importée trouvée.")
        }
    }

    private fun showImageSelectionDialog(files: Array<File>) {
        val fileNames = files.map { it.name }.toTypedArray()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choisir une image")
            .setItems(fileNames) { _, which ->
                val selectedImage = files[which]
                displaySelectedImage(selectedImage)
            }

        builder.create().show()
    }

    private fun displaySelectedImage(selectedImage: File) {
        val measurementImageView: ImageView = findViewById(R.id.measurementPhotoView)
        measurementImageView.setImageURI(Uri.fromFile(selectedImage))

        // Réinitialisez la page après le chargement
        measurementPoints.clear()
        registeredFaceRatios.clear()
        currentPointIndex = 0
        showRecognitionButtons(false)

        // Suppression des points visuels de l'image précédente
        val measurementLayout: ViewGroup = findViewById(R.id.measurementLayout)
        measurementLayout.removeAllViews()

        updateInstructionText()
        loadRegisteredFaceRatios()

        showToast("Image chargée avec succès.")
    }

    private fun loadRegisteredFaceRatios() {
        val file = File(filesDir, FILE_NAME)

        if (file.exists()) {
            val lines = file.readLines()
            for (line in lines) {
                val parts = line.split(":")
                if (parts.size == 2) {
                    val profileName = parts[0].trim()
                    val ratiosSum = parts[1].trim().toFloatOrNull()

                    if (profileName.isNotEmpty() && ratiosSum != null) {
                        registeredFaceRatios[profileName] = ratiosSum
                    }
                }
            }
        }
    }

    private fun updateInstructionText() {
        if (currentPointIndex < orderedPoints.size) {
            instructionTextView.text = "Placez le point : ${orderedPoints[currentPointIndex]}"
        } else {
            instructionTextView.text = "Tous les points ont été placés."
            showRecognitionButtons(true)
        }
    }

    private fun showRecognitionButtons(show: Boolean) {
        val btnRecognize: Button = findViewById(R.id.btnRecognize)
        val btnSaveProfile: Button = findViewById(R.id.btnSaveProfile)
        val profileNameEditText: EditText = findViewById(R.id.profileNameEditText)

        if(show){
            btnRecognize.visibility = View.VISIBLE
            profileNameEditText.visibility = View.VISIBLE
            btnSaveProfile.visibility = View.VISIBLE
        }else{
            btnRecognize.visibility = View.INVISIBLE
            profileNameEditText.visibility = View.INVISIBLE
            profileNameEditText.text.clear()
            btnSaveProfile.visibility = View.INVISIBLE
        }
    }

    private fun handleTouchEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                addMeasurementPoint(event.rawX, event.rawY)
                //showToast("Point ajouté à : (${event.rawX}, ${event.rawY})")

                currentPointIndex++
                updateInstructionText()
            }
        }
    }

    private fun addMeasurementPoint(x: Float, y: Float) {
        if (measurementPoints.size < 12) {
            measurementPoints.add(Pair(x, y))

            val pointImageView = ImageView(this)
            pointImageView.setImageResource(R.drawable.red_dot)
            pointImageView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val measurementLayout: ViewGroup = findViewById(R.id.measurementLayout)
            measurementLayout.addView(pointImageView)

            val params = pointImageView.layoutParams as ViewGroup.MarginLayoutParams
            val parentLocation = IntArray(2)
            measurementLayout.getLocationOnScreen(parentLocation)

            params.leftMargin = (x - parentLocation[0] - pointImageView.width / 2).toInt()
            params.topMargin = (y - parentLocation[1] - pointImageView.height / 2).toInt()
            pointImageView.layoutParams = params
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun onRecognizeClick() {
        recognizeFace()
    }

    private fun recognizeFace() {
        val currentRatiosSum = calculateRatios()

        for ((profileName, registeredRatio) in registeredFaceRatios) {
            val difference = abs(currentRatiosSum - registeredRatio)
            if (difference < TOLERANCE) {
                showToast("Visage reconnu avec Ratios : $currentRatiosSum pour $profileName")
                return
            }
        }

        showToast("Aucun visage reconnu.")
    }

    private fun onSaveProfileClick() {
        val profileNameEditText: EditText = findViewById(R.id.profileNameEditText)
        val profileName = profileNameEditText.text.toString().trim()

        if (profileName.isNotEmpty()) {
            val currentRatiosSum = calculateRatios()
            registeredFaceRatios[profileName] = currentRatiosSum

            // Sauvegarde dans le fichier
            saveProfileToFile(profileName, currentRatiosSum)

            showToast("Profil $profileName enregistré avec succès.")
        } else {
            showToast("Veuillez entrer un nom de profil.")
        }
    }

    private fun saveProfileToFile(profileName: String, ratiosSum: Float) {
        val file = File(filesDir, FILE_NAME)
        file.appendText("$profileName:$ratiosSum\n")
    }

    /*val lipsRatio = getDistance(BLY, TLY, RLX, LLX)
        val noseRatio = getDistance(BNY, E1Y, SNRX, SNLX)
        val faceRatio = getDistance(BCY, E1Y, SFRX, SFLX)
        val eyesRatio = getDistance(SFRX, SFLX, E1X, E2X)*/
    //"E1", "E2", "SFR", "SFL", "BN", "SNR", "SNL", "TL", "BL", "RL", "LL", "BC"
    private fun calculateRatios(): Float {

        val lipsRatio = getDistance(measurementPoints[8].second, measurementPoints[7].second, measurementPoints[9].first, measurementPoints[10].first)
        val noseRatio = getDistance(measurementPoints[4].second, measurementPoints[0].second, measurementPoints[5].first, measurementPoints[6].first)
        val faceRatio = getDistance(measurementPoints[11].second, measurementPoints[0].second, measurementPoints[2].first, measurementPoints[3].first)
        val eyesRatio = getDistance(measurementPoints[2].first, measurementPoints[3].first, measurementPoints[0].first, measurementPoints[1].first)

        return lipsRatio + noseRatio + faceRatio + eyesRatio
    }

    private fun getDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt((x1 - x2).pow(2) + (y1 - y2).pow(2))
    }
}