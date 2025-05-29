package com.example.sliverroad

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class PhotoCaptureActivity : AppCompatActivity() {
    private lateinit var photoUri: Uri
    private lateinit var photoFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1002)
        } else {
            dispatchTakePictureIntent()
        }
    }

    private fun dispatchTakePictureIntent() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            photoFile = createImageFile()
            photoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        } else {
            Toast.makeText(this, "카메라 앱이 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "IMG_$timeStamp"
        return File.createTempFile(fileName, ".jpg", cacheDir)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Toast.makeText(this, "사진 촬영 완료", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
    }
}
