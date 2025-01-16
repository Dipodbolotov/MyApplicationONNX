package com.example.myapplication;

import ai.djl.ndarray.types.Shape
import ai.onnxruntime.*
import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.DetermineDurationProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.mfcc.MFCC
import io.kinference.ndarray.arrays.FloatNDArray
import io.kinference.ndarray.arrays.Strides
import io.kinference.ndarray.arrays.tiled.FloatTiledArray
import io.kinference.utils.toLongArray
import org.jetbrains.kotlinx.dl.api.core.shape.toTensorShape
import kotlin.math.min


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var recordButton: Button
    private lateinit var resultTextView: TextView
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recordButton = findViewById(R.id.recordButton)
        resultTextView = findViewById(R.id.tvResult)

        // Запрос разрешений на запись аудио
        requestPermissions()

        recordButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }
    }

    private fun requestPermissions() {
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.RECORD_AUDIO] == true &&
                permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true &&
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
            ) {
                Log.d(TAG, "Permissions granted")
            } else {
                Log.d(TAG, "Permissions denied")
            }
        }.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        )
    }

    private fun startRecording() {
        isRecording = true
        recordButton.text = "Stop Recording"

        val filePath = "${externalCacheDir?.absolutePath}/recorded_audio.wav"
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                1
            )
//            return
        }
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            16000, // Частота дискретизации
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            AudioRecord.getMinBufferSize(
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
        )

        audioRecord.startRecording()

        val audioData = ByteArray(1024)
        val outputStream = FileOutputStream(File(filePath))

        Thread {
            while (isRecording) {
                val read = audioRecord.read(audioData, 0, audioData.size)
                if (read > 0) {
                    outputStream.write(audioData, 0, read)
                }
            }
            audioRecord.stop()
            audioRecord.release()
            outputStream.close()

            Log.d("No1w", "Now1")
            // Запуск инференса после остановки записи
            runOnUiThread {
                Log.d("Now", "Now")
                recognizeAudio(filePath)
            }
        }.start()
    }

    private fun stopRecording() {
        isRecording = false
        recordButton.text = "Start Recording"
    }

    private fun cacheModelFile(): String {
        val f = File("$cacheDir/model.onnx")
        if (!f.exists()) {
            try {
                assets.open("model.onnx").use { inputStream ->
                    FileOutputStream(f).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
        return f.path
    }

    private fun recognizeAudio(filePath: String) {
//        val assetManager = assets
//        val onnxModelPath = "model.onnx"

        try {
            val ortEnvironment = OrtEnvironment.getEnvironment()
            val cachedFilePath = cacheModelFile()
            val ortSession = ortEnvironment.createSession(cachedFilePath)

            // Prepare input data
            val data = processAudioToTensor(filePath)
            val inputTensor = OnnxTensor.createTensor(ortEnvironment, data)
            val inputMap: Map<String, OnnxTensor> = mapOf("input_features" to inputTensor)

            // Run model in a separate thread
            Thread {
                try {
                    val result = ortSession.run(inputMap)
                    val outputTensor = result[0] as OnnxTensor
                    val outputData = outputTensor.value as Array<FloatArray>

                    // Log results
                    Log.d(TAG, "Inference result: ${outputData.contentToString()}")

                    // Update UI on the main thread
                    runOnUiThread {
                        resultTextView.text = "Inference result: ${outputData.contentToString()}"
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error during inference: ${e.message}", e)
                }
            }.start()
        } catch (e: Exception) {
            Log.e("Error", "Failed to initialize ONNX Runtime: ${e.message}", e)
        }
    }

    fun processAudioToTensor(audioFilePath: String): FloatTiledArray {
        val sampleRate = 16000
        val bufferSize = 876.0f
        val bufferOverlap = 256
        val nMels = 80
        val targetFrames = 876

        // Initialize dispatcher
        val dispatcher: AudioDispatcher = AudioDispatcherFactory.fromFile(File(audioFilePath), bufferSize.toInt(), bufferOverlap)

        // Initialize MFCC processor
        val mfccProcessor = MFCC(sampleRate, bufferSize, nMels, 20, 50f, sampleRate / 2f)
        val mfccFeatures = mutableListOf<FloatArray>()

        dispatcher.addAudioProcessor(mfccProcessor)
        dispatcher.addAudioProcessor(object : AudioProcessor {
            override fun process(audioEvent: AudioEvent): Boolean {
                // Extract MFCCs for each buffer
                val mfcc = mfccProcessor.mfcc
                if (mfcc != null) {
                    mfccFeatures.add(mfcc)
                }
                return true
            }

            override fun processingFinished() {
                // No-op
            }
        })

        // Run the dispatcher to process the audio file
        dispatcher.run()

        // Ensure the number of frames matches targetFrames
        val paddedFeatures = Array(targetFrames) { FloatArray(nMels) { 0f } }
        for (i in 0 until min(targetFrames, mfccFeatures.size)) {
            System.arraycopy(mfccFeatures[i], 0, paddedFeatures[i], 0, nMels)
        }

        val flattenedData = paddedFeatures.flatMap { it.toList() }.toFloatArray()
        val shape = Shape(targetFrames.toLong(), nMels.toLong(), 1)
        val strides = Strides(intArrayOf(targetFrames, nMels, 1))
        val tiledArray = FloatTiledArray(strides, flattenedData)
        return  tiledArray
    }
}
