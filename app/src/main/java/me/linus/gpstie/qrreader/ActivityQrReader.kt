package me.linus.gpstie.qrreader

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.content.PermissionChecker
import android.support.v7.app.AppCompatActivity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import me.linus.gpstie.R

class ActivityQrReader: AppCompatActivity() {

    companion object {
        // Note that this numbers have no real meaning and could be anything else
        private val CAMERA_PERMISSION_REQUEST_ID = 502
        val RESULT_READING_SUCCEEDED = 200
        val RESULT_READING_CANCELLED = 450
        val RESULT_READING_FAILED_PERMISSION_DENIED = 451
    }

    lateinit var cameraPreview: SurfaceView // Displayed fullscreen to view the camera image

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_READING_CANCELLED) // Cancelled when not changed


        // Set fullscreen
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // Transculent navigation buttons when supported
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        // Load layout
        setContentView(R.layout.gt_qrreader_layout)
        cameraPreview = findViewById(R.id.qt_qrreader_camera_preview) as SurfaceView


        // Load camera or request permission dynamically
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST_ID)
        else
            initQrReading()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {

        permissions
                .mapIndexed { index, permission -> Pair(permission, grantResults[index]) }
                .filter { (permission, _) -> permission == Manifest.permission.CAMERA }
                .forEach { (_, grantResult) ->
                    if(grantResult == PermissionChecker.PERMISSION_GRANTED)
                        initQrReading()
                    else {
                        setResult(RESULT_READING_FAILED_PERMISSION_DENIED)
                        finish()
                    }
                }
    }

    @SuppressLint("MissingPermission")
            /**
     * Starts scanning for qr codes.
     * When this method is called, it was already ensured beforehand that the permission to
     * use the camera was given by the user.
     */
    fun initQrReading() {
        // Codes qr coded on-the-fly (part of Google Play Services [Vision API])
        val qrDetector = BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build()

        val camSource = CameraSource.Builder(this, qrDetector)
                .setRequestedPreviewSize(1280, 720) // <- 720p
                .setAutoFocusEnabled(true)
                .build()

        // Listen for recognized qr codes
        qrDetector.setProcessor(object: Detector.Processor<Barcode> {

            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                if (detections.detectedItems.size() == 0) return

                val scannedText = detections.detectedItems.valueAt(0).displayValue
                if(!scannedText.startsWith("gpstie://")) return

                runOnUiThread {
                    val data = Intent()
                    data.putExtra("address", scannedText.replaceFirst("gpstie://", ""))
                    setResult(RESULT_READING_SUCCEEDED, data)
                    camSource.stop()
                    finish()
                }
            }

            override fun release() {}
        })

        // Start camera and display a preview on cameraPreview
        camSource.start(cameraPreview.holder)
    }

    /**
     * Scanning cancelled by user
     */
    override fun onBackPressed() {
        finish()
        super.onBackPressed()
    }

    /**
     * Finish activity onStop, because the camera shuts down automatically
     */
    override fun onStop() {
        finish()
        super.onPause()
    }

}