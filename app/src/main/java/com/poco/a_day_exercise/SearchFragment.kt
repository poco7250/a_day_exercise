package com.poco.a_day_exercise

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.ImageDecoder.decodeBitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.text.method.LinkMovementMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
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
	private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>// 권한

	private var photoUri:Uri? = null


	private val MODEL_PATH = "searchequipment.tflite"

	private lateinit var classifier: Classifier
	private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
		uri?.let { imageUri ->
			try {
				val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
					val source = ImageDecoder.createSource(requireContext().contentResolver, imageUri)
					decodeBitmap(source)
				} else {
					MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)
				}

				// 이미지 분류
				val result = classifier.classify(bitmap)

				// 분류 결과 출력
				val resultStr = String.format(Locale.ENGLISH, "%s", result.first)
				binding.textResult.movementMethod = LinkMovementMethod.getInstance()
				binding.textResult.text = resultStr // 수정된 부분
				binding.textResult.setOnClickListener {
					val query = resultStr // 원하는 검색어 입력
					val intent = Intent(Intent.ACTION_VIEW,
						Uri.parse("https://www.youtube.com/results?search_query=$query"))
					startActivity(intent)
				}
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
					decodeBitmap(source)
				} else {
					MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)
				}

				// 이미지 분류
				val result = classifier.classify(bitmap)

				// 분류 결과 출력
				val resultStr = String.format(Locale.ENGLISH, "%s", result.first)
				binding.textResult.text = resultStr // 수정된 부분
				binding.textResult.setOnClickListener {
					val query = resultStr // 원하는 검색어 입력
					val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$query"))
					startActivity(intent)
				}
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
		// 버전 체크 해서 permission다르게하기
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
		{
			val permission = arrayOf(
				android.Manifest.permission.CAMERA,
				android.Manifest.permission.READ_MEDIA_IMAGES
			)
			// 카메라
			permissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
				if (permissions[android.Manifest.permission.CAMERA] == true &&
					permissions[android.Manifest.permission.READ_MEDIA_IMAGES] == true) {
					binding.camerabutton.setOnClickListener {
						openCamera()
					}
					binding.gallerybutton.setOnClickListener {
						openGallery()
					}
				} else {
					Toast.makeText(requireContext(), "외부 저장소 권한을 승인해야 앱을 사용할 수 있습니다.", Toast.LENGTH_LONG).show()
					val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", requireActivity().packageName, null))
					startActivity(intent)
				}
			}
			permissionsLauncher.launch(permission)
		}
		else {
			val permission = arrayOf(
				android.Manifest.permission.CAMERA,
				android.Manifest.permission.READ_EXTERNAL_STORAGE,
				android.Manifest.permission.WRITE_EXTERNAL_STORAGE
			)

			// 카메라
			permissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
				if (permissions[android.Manifest.permission.CAMERA] == true &&
					permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE] == true &&
					permissions[android.Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
					) {
					binding.camerabutton.setOnClickListener {
						openCamera()
					}
					binding.gallerybutton.setOnClickListener {
						openGallery()
					}
				} else {
					Toast.makeText(requireContext(), "외부 저장소 권한을 승인해야 앱을 사용할 수 있습니다.", Toast.LENGTH_LONG).show()
					val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", requireActivity().packageName, null))
					startActivity(intent)
				}
			}
			permissionsLauncher.launch(permission)
		}
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

	private fun openCamera() {
		if (hasPermissions()) {
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
		} else {
			// 권한이 거절되었을 때 설정으로 이동
			openAppSettings()
		}
	}
	private fun openGallery() {
		if (hasPermissions()) {
			initClassifier()
			getContent.launch("image/*")
		} else {
			// 권한이 거절되었을 때 설정으로 이동
			openAppSettings()
		}
	}

	// 권한 확인
	private fun hasPermissions(): Boolean {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			(ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
					&& ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED)
		} else {
			(ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
					&& ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
					&& ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
		}
	}

	// 설정으로 이동
	private fun openAppSettings() {
		val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
		val uri = Uri.fromParts("package", requireContext().packageName, null)
		intent.data = uri
		startActivity(intent)
	}

}