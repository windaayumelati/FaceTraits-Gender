package com.example.uasml2_gender

import android.content.res.AssetManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class SimulasiFragment : Fragment() {

    private lateinit var interpreter: Interpreter
    private val modelPath = "gender.tflite"

    private lateinit var edtLongHair: EditText
    private lateinit var edtForeheadWidth: EditText
    private lateinit var edtForeheadHeight: EditText
    private lateinit var edtNoseWide: EditText
    private lateinit var edtNoseLong: EditText
    private lateinit var edtLipsThin: EditText
    private lateinit var edtDistanceNoseToLipLong: EditText
    private lateinit var btnPredict: Button
    private lateinit var txtResult: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_simulasi, container, false)

        // Initialize UI components
        edtLongHair = view.findViewById(R.id.edtLongHair)
        edtForeheadWidth = view.findViewById(R.id.edtForeheadWidth)
        edtForeheadHeight = view.findViewById(R.id.edtForeheadHeight)
        edtNoseWide = view.findViewById(R.id.edtNoseWide)
        edtNoseLong = view.findViewById(R.id.edtNoseLong)
        edtLipsThin = view.findViewById(R.id.edtLipsThin)
        edtDistanceNoseToLipLong = view.findViewById(R.id.edtDistanceNoseToLipLong)
        btnPredict = view.findViewById(R.id.btnPredict)
        txtResult = view.findViewById(R.id.txtResult)

        // Set text change listeners for binary inputs
        setBinaryInputValidation(edtLongHair)
        setBinaryInputValidation(edtNoseWide)
        setBinaryInputValidation(edtNoseLong)
        setBinaryInputValidation(edtLipsThin)
        setBinaryInputValidation(edtDistanceNoseToLipLong)

        // Set text change listeners for decimal inputs
        setDecimalInputValidation(edtForeheadWidth)
        setDecimalInputValidation(edtForeheadHeight)

        // Load TensorFlow Lite model
        initInterpreter()

        // Set OnClickListener for Predict button
        btnPredict.setOnClickListener {
            // Clear previous result
            txtResult.text = ""

            if (validateInputs()) {
                // Get values from each input and convert to Float
                val longHair = edtLongHair.text.toString().toFloat()
                val foreheadWidth = edtForeheadWidth.text.toString().toFloat()
                val foreheadHeight = edtForeheadHeight.text.toString().toFloat()
                val noseWide = edtNoseWide.text.toString().toFloat()
                val noseLong = edtNoseLong.text.toString().toFloat()
                val lipsThin = edtLipsThin.text.toString().toFloat()
                val distanceNoseToLipLong = edtDistanceNoseToLipLong.text.toString().toFloat()

                // Perform inference with valid inputs
                val result = doInference(
                    longHair, foreheadWidth, foreheadHeight,
                    noseWide, noseLong, lipsThin, distanceNoseToLipLong
                )

                // Display result based on prediction
                val gender = if (result == 1) "Laki-laki" else "Perempuan"
                txtResult.text = gender

                // Show result in an AlertDialog
                showResultDialog(gender)
            } else {
                // Clear result if inputs are invalid
                txtResult.text = ""
            }
        }

        return view
    }

    // Method to initialize TensorFlow Lite interpreter
    private fun initInterpreter() {
        val options = Interpreter.Options()
        options.setNumThreads(5)
        options.setUseNNAPI(true)
        interpreter = Interpreter(loadModelFile(requireContext().assets, modelPath), options)
    }

    // Method to load TensorFlow Lite model file
    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // Method to set text change listener for binary inputs
    private fun setBinaryInputValidation(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s.toString()
                if (text.isNotEmpty()) {
                    if (text != "0" && text != "1") {
                        showToast("Input harus berupa angka 0 atau 1")
                        editText.setText("")
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // Method to set text change listener for decimal inputs
    private fun setDecimalInputValidation(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Do nothing on text change for decimal inputs
            }

            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (text.isNotEmpty() && !isValidDecimal(text)) {
                    showToast("Input harus berupa angka dengan satu digit di belakang koma")
                    editText.setText("")  // Atur kembali nilai kosong jika tidak valid
                }
            }
        })
    }

    // Method to validate decimal input with exactly one digit after the decimal point
    private fun isValidDecimal(input: String): Boolean {
        if (input.isEmpty()) return true // Izinkan input kosong

        // Validasi input angka dengan satu digit di belakang koma
        val decimalIndex = input.indexOf('.')
        return if (decimalIndex != -1) {
            // Pastikan tepat satu digit setelah titik desimal
            input.substring(decimalIndex + 1).length <= 1
        } else {
            // Jika tidak ada titik desimal, validasi sebagai bilangan bulat
            true
        }
    }

    // Method to validate input
    private fun validateInputs(): Boolean {
        val longHairText = edtLongHair.text.toString().trim()
        val foreheadWidthText = edtForeheadWidth.text.toString().trim()
        val foreheadHeightText = edtForeheadHeight.text.toString().trim()
        val noseWideText = edtNoseWide.text.toString().trim()
        val noseLongText = edtNoseLong.text.toString().trim()
        val lipsThinText = edtLipsThin.text.toString().trim()
        val distanceNoseToLipLongText = edtDistanceNoseToLipLong.text.toString().trim()

        return if (longHairText.isEmpty() ||
            foreheadWidthText.isEmpty() ||
            foreheadHeightText.isEmpty() ||
            noseWideText.isEmpty() ||
            noseLongText.isEmpty() ||
            lipsThinText.isEmpty() ||
            distanceNoseToLipLongText.isEmpty()
        ) {
            showToast("Semua input harus diisi")
            false
        } else {
            // After ensuring all inputs are not empty, validate values and data type
            val longHair = longHairText.toFloatOrNull()
            val foreheadWidth = foreheadWidthText.toFloatOrNull()
            val foreheadHeight = foreheadHeightText.toFloatOrNull()
            val noseWide = noseWideText.toFloatOrNull()
            val noseLong = noseLongText.toFloatOrNull()
            val lipsThin = lipsThinText.toFloatOrNull()
            val distanceNoseToLipLong = distanceNoseToLipLongText.toFloatOrNull()

            return when {
                longHair == null || (longHair != 0f && longHair != 1f) -> {
                    showToast("Nilai Rambut Panjang harus 0 atau 1")
                    false
                }
                foreheadWidth == null || foreheadWidth < 11.4f || foreheadWidth > 15.5f -> {
                    showToast("Lebar dahi harus antara 11.4 dan 15.5 cm")
                    false
                }
                foreheadHeight == null || foreheadHeight < 5.1f || foreheadHeight > 7.1f -> {
                    showToast("Tinggi dahi harus antara 5.1 dan 7.1 cm")
                    false
                }
                noseWide == null || (noseWide != 0f && noseWide != 1f) -> {
                    showToast("Nilai Hidung Lebar harus 0 atau 1")
                    false
                }
                noseLong == null || (noseLong != 0f && noseLong != 1f) -> {
                    showToast("Nilai Hidung Panjang harus 0 atau 1")
                    false
                }
                lipsThin == null || (lipsThin != 0f && lipsThin != 1f) -> {
                    showToast("Nilai Bibir Tipis harus 0 atau 1")
                    false
                }
                distanceNoseToLipLong == null || (distanceNoseToLipLong != 0f && distanceNoseToLipLong != 1f) -> {
                    showToast("Nilai Jarak Hidung ke Bibir Panjang harus 0 atau 1")
                    false
                }
                else -> true
            }
        }
    }

    // Method to show toast message
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    // Method to perform inference
    private fun doInference(
        longHair: Float, foreheadWidth: Float, foreheadHeight: Float,
        noseWide: Float, noseLong: Float, lipsThin: Float, distanceNoseToLipLong: Float
    ): Int {
        val inputVal = floatArrayOf(longHair, foreheadWidth, foreheadHeight, noseWide, noseLong, lipsThin, distanceNoseToLipLong)
        val output = Array(1) { FloatArray(2) }
        interpreter.run(inputVal, output)
        Log.e("result", output[0].contentToString())

        // Output is a probability distribution, get the index with the highest probability
        return output[0].indices.maxByOrNull { output[0][it] } ?: -1
    }

    // Method to show result in an AlertDialog
    private fun showResultDialog(gender: String) {
        val message = "Berdasarkan data yang diinputkan, hasil prediksi gender adalah $gender"
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Hasil Prediksi")
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }
}
