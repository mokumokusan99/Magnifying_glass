package jp.noppyworld.mokumokusan99.magnifying_glass

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import jp.noppyworld.mokumokusan99.magnifying_glass.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.camera.lifecycle.ProcessCameraProvider
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import androidx.camera.core.*
import androidx.camera.view.PreviewView

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var torchButton: ImageView
    private lateinit var cameraExecutor: ExecutorService
    private var torchState = TorchState.OFF
    private lateinit var imageView: ImageView // 画像表示用のImageViewを追加
    private lateinit var camera: Camera
    private var isPlaying: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        // ImageViewを初期化
        torchButton = findViewById(R.id.torchButton)
        // 初期状態はOFFのアイコンを設定
        torchButton.setImageResource(R.drawable.baseline_flashlight_off_24)

        val seekBar = findViewById<SeekBar>(R.id.seekBar)
        seekBar.progress = 0

        imageView = findViewById(R.id.pauseImageView)
        // 再生/停止ボタン
        viewBinding.Button.setOnClickListener {
            togglePlayPause()
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    private fun togglePlayPause() {
        isPlaying = !isPlaying
        if (isPlaying) {
            viewBinding.Button.text = "一時停止"
            startPreview()
        } else {
            viewBinding.Button.text = "再開"
            stopPreview()
        }
    }
    private fun startPreview() {

        val preview = findViewById<PreviewView>(R.id.viewFinder)

        preview.visibility = View.VISIBLE
        imageView.visibility = View.GONE
    }


    private fun stopPreview() {
        val preview = findViewById<PreviewView>(R.id.viewFinder)

        val original = preview.bitmap?: return
        val scaled = getscaledBitmap(original)
        imageView.setImageBitmap(scaled)
        imageView.scaleType = ImageView.ScaleType.CENTER

        preview.visibility = View.GONE
        imageView.visibility = View.VISIBLE

    }

    private fun getscaledBitmap(bitmap: Bitmap): Bitmap{
        val seekBar = findViewById<SeekBar>(R.id.seekBar)
        val scale = 1.0f + seekBar.progress / 200.0f // 拡大倍率
        val width = bitmap.width * scale
        val height = bitmap.height * scale
        return Bitmap.createScaledBitmap(bitmap, width.toInt(), height.toInt(), true)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview)


            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))

        // Select back camera as a default
        CameraSelector.DEFAULT_BACK_CAMERA

        Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
            }
        // プレビュー画面を拡大する関数
        fun zoomInPreview(scaleFactor: Float) {
            viewBinding.viewFinder.scaleX = scaleFactor
            viewBinding.viewFinder.scaleY = scaleFactor
        }
        // SeekBarのリスナーを設定
        val seekBar = findViewById<SeekBar>(R.id.seekBar)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // プレビュー画面を拡大する処理
                val scale = 1.0f + progress / 200.0f // 拡大倍率
                zoomInPreview(scale)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    // Toggle torch state
    fun toggleTorch(view: View) {
        val currentTorchState = torchState
        val newTorchState = if (currentTorchState == TorchState.OFF) TorchState.ON else TorchState.OFF
        camera.cameraControl.enableTorch(newTorchState == TorchState.ON)

        if (newTorchState == TorchState.OFF) {
            torchButton.setImageResource(R.drawable.baseline_flashlight_off_24) // OFFのアイコン
        } else {
            torchButton.setImageResource(R.drawable.baseline_flashlight_on_24) // ONのアイコン
        }
        torchState = newTorchState
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    companion object {
        private const val TAG = "CameraXApp"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}