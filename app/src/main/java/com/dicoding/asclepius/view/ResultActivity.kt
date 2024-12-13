package com.dicoding.asclepius.view

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.dicoding.asclepius.R
import com.dicoding.asclepius.data.database.History
import com.dicoding.asclepius.data.database.HistoryDatabase
import com.dicoding.asclepius.databinding.ActivityResultBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.io.InputStream

class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private var imagePath: String? = null
    private var resultText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this, R.color.primary)))

        val imageUri = intent.getStringExtra(EXTRA_IMAGE_URI)?.let { Uri.parse(it) }
        if (imageUri != null) {
            binding.resultImage.setImageURI(imageUri)
            imagePath = imageUri.toString()

            try {
                val bitmap = getBitmapFromUri(imageUri)
                bitmap?.let {
                    val imageClassifierHelper = ImageClassifierHelper(
                        context = this,
                        classifierListener = object : ImageClassifierHelper.ClassifierListener {
                            override fun onError(error: String) {
                                Toast.makeText(this@ResultActivity, "Error: $error", Toast.LENGTH_LONG).show()
                            }

                            override fun onResults(results: List<Classifications>?, inferenceTime: Long) {
                                if (results != null && results.isNotEmpty()) {
                                    showResults(results)
                                } else {
                                    Snackbar.make(binding.root, "No results found. Please try again.", Snackbar.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                    imageClassifierHelper.classifyImage(bitmap)
                } ?: run {
                    Snackbar.make(binding.root, "Failed to load image. Please try again.", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Invalid image URI. Please select a valid image.", Toast.LENGTH_LONG).show()
        }

        binding.saveButton.setOnClickListener {
            saveHistoryToDatabase()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading Bitmap: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun showResults(results: List<Classifications>) {
        val topResult = results[0]
        val label = topResult.categories[0].label
        val score = topResult.categories[0].score

        fun Float.formatToString(): String {
            return String.format("%.2f%%", this * 100)
        }
        resultText = "$label ${score.formatToString()}"
        binding.resultText.text = resultText
    }

    private fun saveHistoryToDatabase() {
        if (imagePath != null && resultText.isNotEmpty()) {
            val history = History(
                imagePath = imagePath!!,
                result = resultText
            )

            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val historyDao = HistoryDatabase.getDatabase(applicationContext).historyDao()
                    historyDao.insertHistory(history)
                }
                runOnUiThread {
                    Toast.makeText(this@ResultActivity, "Data saved successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Snackbar.make(binding.root, "Failed to save data. Please try again.", Snackbar.LENGTH_LONG).show()
        }
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }
}
