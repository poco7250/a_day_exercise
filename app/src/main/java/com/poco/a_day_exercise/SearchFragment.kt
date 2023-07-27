package com.poco.a_day_exercise

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.ImageDecoder.decodeBitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.poco.a_day_exercise.databinding.FragmentSearchBinding
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil.loadLabels
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.*

class SearchFragment : Fragment() {
	val binding by lazy {FragmentSearchBinding.inflate(layoutInflater)}
	lateinit var cameraPermission:ActivityResultLauncher<String> // 카메라 권한
	lateinit var storagePermission:ActivityResultLauncher<String> // 저장소 권한
	lateinit var cameraLauncher: ActivityResultLauncher<Uri> // 카메라 앱 호출
	lateinit var galleryLauncher: ActivityResultLauncher<String>// 갤러리 앱 호출

	private var photoUri:Uri? = null
	private var isCameraOpen = false
	private var isGalleryOpen = false


	private val MODEL_PATH = "searchequipment.tflite"

	private lateinit var classifier: Classifier
	private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
		uri?.let { imageUri ->
			try {
				val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
					val source = ImageDecoder.createSource(requireContext().contentResolver, imageUri)
					ImageDecoder.decodeBitmap(source)
				} else {
					MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)
				}

				// 이미지 분류
				val result = classifier.classify(bitmap)

				// 분류 결과 출력
				val resultStr = String.format(Locale.ENGLISH, "%s", result.first)
				binding.textResult.text = resultStr // 수정된 부분
				binding.imagepreview.setImageURI(imageUri)

			} catch (e: IOException) {
				Toast.makeText(requireContext(), "이미지를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
			}
		}
	}

	private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
		if (isSuccess) {
			// 이미지 찍기 성공
			val imageUri = photoUri ?: return@registerForActivityResult // NULL이면 함수를 종료
			try {
				val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
					val source = ImageDecoder.createSource(requireContext().contentResolver, imageUri)
					ImageDecoder.decodeBitmap(source)
				} else {
					MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)
				}

				// 이미지 분류
				val result = classifier.classify(bitmap)

				// 분류 결과 출력
				val resultStr = String.format(Locale.ENGLISH, "%s", result.first)
				binding.textResult.text = resultStr // 수정된 부분
				binding.imagepreview.setImageURI(photoUri)
			} catch (e: IOException) {
				Toast.makeText(requireContext(), "이미지를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
			}
		}
	}


	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		// 카메라
		storagePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()){ isGranted ->
			if(isGranted) {
				setViews()
			} else {
				Toast.makeText(requireContext(), "외부 저장소 권한을 승힌해야 앱을 사용할 수 있습니다.", Toast.LENGTH_LONG).show()
			}
		}
		cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()){ isGranted ->
			if(isGranted) {
				openCamera()
			} else{
				Toast.makeText(requireContext(),
					"카메라 권한을 승인해야 카메라를 사용할 수 있습니다.",
					Toast.LENGTH_LONG).show()
			}
		}
		// 카메라

		// 갤러리
		galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
				uri -> binding.imagepreview.setImageURI(uri)
				initClassifier()
				getContent.launch("image/*")
		}

		storagePermission.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
		return binding.root
	}

	class Classifier(private var context: Context, private val modelName: String)
	{
		private lateinit var model: Model
		private lateinit var inputImage : TensorImage
		private lateinit var outputBuffer: TensorBuffer
		private var modelInputChannel = 0
		private var modelInputWidth = 0
		private var modelInputHeight = 0
		private var labels = listOf<String>()


		fun init() {
			model = Model.createModel(context, modelName)
			initModelShape()
			val loadedLabels = loadLabels(context, LABEL_FILE)
			labels += loadedLabels
		}

		private fun loadLabels(context: Context, labelFile: String): List<String> {
			val labels = mutableListOf<String>()
			try {
				val reader = BufferedReader(InputStreamReader(context.assets.open(labelFile)))
				var line: String?
				while (reader.readLine().also { line = it } != null) {
					labels.add(line!!)
				}
				reader.close()
			} catch (e: IOException) {
				e.printStackTrace()
			}
			return labels
		}

		private fun initModelShape() {
			val inputTensor = model.getInputTensor(0)
			val inputShape = inputTensor.shape()
			modelInputChannel = inputShape[0]
			modelInputWidth = inputShape[1]
			modelInputHeight = inputShape[2]

			inputImage = TensorImage(inputTensor.dataType())

			val outputTensor = model.getOutputTensor(0)
			outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), outputTensor.dataType())
		}

		private fun convertBitmapToARGB8888(bitmap: Bitmap) = bitmap.copy(Bitmap.Config.ARGB_8888, true)

		private fun loadImage(bitmap: Bitmap): TensorImage {
			if (bitmap.config != Bitmap.Config.ARGB_8888) {
				inputImage.load(convertBitmapToARGB8888(bitmap))
			} else {
				inputImage.load(bitmap)
			}
			val imageProcessor = ImageProcessor.Builder()
				.add(ResizeOp(modelInputWidth, modelInputHeight, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
				.add(NormalizeOp(0.0f, 255.0f))
				.build()

			return imageProcessor.process(inputImage)
		}

		fun classify(image: Bitmap): Pair<String, Float> {
			inputImage = loadImage(image)
			val inputs = arrayOf(inputImage.buffer)
			val outputs = mutableMapOf<Int, Any>()
			outputs[0] = outputBuffer.buffer.rewind()
			model.run(inputs, outputs)
			val output = TensorLabel(labels, outputBuffer).mapWithFloatValue
			return argmax(output)
		}

		fun finish() {
			if (::model.isInitialized) model.close()
		}

		private fun argmax(map: Map<String, Float>) =
			map.entries.maxByOrNull { it.value }?.let {
				it.key to it.value
			} ?: ("" to 0f)

		companion object {
			const val CLASSIFIER = "searchequipment.tflite"
			const val LABEL_FILE = "labels.txt"
		}


	}

	private fun initClassifier() {
		classifier = Classifier(requireContext(), MODEL_PATH)
		try {
			classifier.init()
		} catch (exception: IOException) {
			Toast.makeText(requireContext(), "분류기를 초기화할 수 없습니다.", Toast.LENGTH_SHORT).show()
		}
	}

	private fun setViews() {
		binding.camerabutton.setOnClickListener {
			cameraPermission.launch(android.Manifest.permission.CAMERA)
		}
		binding.gallerybutton.setOnClickListener {
			openGallery()
		}
	}

	private fun openCamera() {
		val photoFile = File.createTempFile(
			"IMG_",
			".jpg",
			requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
		)
		photoUri = FileProvider.getUriForFile(
			requireContext(),
			"com.poco.a_day_exercise.provider",
			photoFile
		)
		initClassifier()
		takePicture.launch(photoUri)
	}
	private fun openGallery() {
		initClassifier()
		getContent.launch("image/*")
	}


}