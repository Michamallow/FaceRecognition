package com.isep.facerecognition.import_scan

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import com.isep.facerecognition.R

class PopupDialog(context: Context, private val optionSelectedListener: OnOptionSelectedListener) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.popup_scan_import_layout)

        val fileNameEditText = findViewById<EditText>(R.id.fileNameEditText)
        val radioOptions = findViewById<RadioGroup>(R.id.radioOptions)
        val scanRadioButton = findViewById<RadioButton>(R.id.takePhotoRadioButton)
        val importRadioButton = findViewById<RadioButton>(R.id.importFileRadioButton)
        val confirmButton = findViewById<Button>(R.id.saveButton)

        confirmButton.setOnClickListener {
            val fileName = fileNameEditText.text.toString()
            val selectedOptionId = radioOptions.checkedRadioButtonId

            if (selectedOptionId == scanRadioButton.id) {
                optionSelectedListener.onOptionSelected(fileName, Option.SCAN)
            } else if (selectedOptionId == importRadioButton.id) {
                optionSelectedListener.onOptionSelected(fileName, Option.IMPORT)
            }

            dismiss()
        }
    }

    interface OnOptionSelectedListener {
        fun onOptionSelected(fileName: String, option: Option)
    }

    enum class Option {
        SCAN,
        IMPORT
    }
}