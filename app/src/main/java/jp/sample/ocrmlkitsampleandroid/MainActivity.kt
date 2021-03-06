package jp.sample.ocrmlkitsampleandroid

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions

import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private val REQUEST = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)

            intent.type = "image/*"
            startActivityForResult(intent, REQUEST)

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                var bitmapOrigin: Bitmap? = null
                var bitmap: Bitmap? = null

                val uri = data.data
                try {
                    uri?.let {
                        contentResolver.openFileDescriptor(it, "r").use {parcelFileDescriptorNullable ->
                            parcelFileDescriptorNullable?.let {parcelFileDescriptor ->
                                val fileDescriptor = parcelFileDescriptor.fileDescriptor
                                bitmapOrigin = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                                contentResolver.openInputStream(it).use {
                                    it?.let {
                                        val exifInterface = ExifInterface(it)
                                        val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
                                        val degrees = when (orientation) {
                                            1 -> { 0f }
                                            3-> { 180f }
                                            6 -> { 90f }
                                            8 -> { 270f }
                                            else -> { 0f }
                                        }
                                        val matrix = Matrix()

                                        val imageWidth = bitmapOrigin?.getWidth() ?: 0
                                        val imageHeight = bitmapOrigin?.getHeight() ?: 0
                                        matrix.setRotate(degrees, imageWidth.toFloat() / 2, imageHeight.toFloat() / 2)
                                        bitmap = Bitmap.createBitmap(bitmapOrigin!!, 0, 0, imageWidth, imageHeight, matrix, true)
                                    }
                                }
                            }
                        }
                    }


                    bitmap?.let {
                        image.setImageBitmap(it)
                        val image = FirebaseVisionImage.fromBitmap(it)

                        val detector = FirebaseVision.getInstance().onDeviceTextRecognizer

                        detector.processImage(image)
                            .addOnSuccessListener { firebaseVisionText ->
                                // Task completed successfully
                                ocr.text = firebaseVisionText.text
                            }
                            .addOnFailureListener {
                                // Task failed with an exception
                            }
                    } ?: run {
                        ocr.text = "bitmap is null"
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else {
                ocr.text = "system error"
            }
        }
    }
}
