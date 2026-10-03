package com.example.aitranslator

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL
import java.net.HttpURLConnection
import org.json.JSONObject
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    private lateinit var tvActiveModel: TextView
    private lateinit var etInputText: EditText
    private lateinit var tvCharCount: TextView
    private lateinit var btnPickImage: ImageButton
    private lateinit var btnPaste: Button
    private lateinit var btnClearAll: Button
    private lateinit var layoutImagePreview: View
    private lateinit var tvImageCountBadge: TextView
    private lateinit var btnClearAllImages: TextView
    private lateinit var layoutThumbnailsContainer: LinearLayout
    private lateinit var btnClearText: ImageButton
    private lateinit var rgTranslationStyle: RadioGroup
    private lateinit var rbStyleAccurate: RadioButton
    private lateinit var rbStylePoetic: RadioButton
    private var isPoeticStyle = false
    private val KEY_POETIC_STYLE = "TRANSLATION_STYLE_POETIC"
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutBtnLoading: View
    private lateinit var btnSend: Button
    private lateinit var btnSettings: ImageButton

    private lateinit var tvSourceLang: TextView
    private lateinit var tvTargetLang: TextView
    private lateinit var btnSwapLang: ImageView

    private val KEY_SOURCE_LANG = "SOURCE_LANG"
    private val KEY_TARGET_LANG = "TARGET_LANG"
    private val DEFAULT_SOURCE_LANG = "🌐 Tự động nhận diện"
    private val DEFAULT_TARGET_LANG = "🇻🇳 Tiếng Việt"
    private var currentSourceLang = DEFAULT_SOURCE_LANG
    private var currentTargetLang = DEFAULT_TARGET_LANG

    private val sourceLanguages = arrayOf(
        "🌐 Tự động nhận diện",
        "🇬🇧 Tiếng Anh",
        "🇨🇳 Tiếng Trung",
        "🇯🇵 Tiếng Nhật",
        "🇰🇷 Tiếng Hàn",
        "🇻🇳 Tiếng Việt",
        "🇫🇷 Tiếng Pháp",
        "🇩🇪 Tiếng Đức",
        "🇷🇺 Tiếng Nga",
        "🇪🇸 Tiếng Tây Ban Nha",
        "🇹🇭 Tiếng Thái"
    )

    private val targetLanguages = arrayOf(
        "🇻🇳 Tiếng Việt",
        "🇬🇧 Tiếng Anh",
        "🇨🇳 Tiếng Trung",
        "🇯🇵 Tiếng Nhật",
        "🇰🇷 Tiếng Hàn",
        "🇫🇷 Tiếng Pháp",
        "🇩🇪 Tiếng Đức",
        "🇷🇺 Tiếng Nga",
        "🇪🇸 Tiếng Tây Ban Nha",
        "🇹🇭 Tiếng Thái"
    )

    private lateinit var nestedScrollViewMain: NestedScrollView
    private lateinit var cardResult: View
    private lateinit var tvResult: TextView
    private lateinit var btnCopyResult: Button
    private lateinit var btnShareResult: ImageButton
    private lateinit var btnClearResult: ImageButton
    private lateinit var layoutResultMeta: View
    private lateinit var tvResultModel: TextView
    private lateinit var tvResultSpeed: TextView

    private val selectedBitmaps = mutableListOf<Bitmap>()
    private val MAX_IMAGE_COUNT = 5
    private var lastResultText: String? = null

    // Multiple API keys support
    private var apiKeys = mutableListOf<String>()
    private var currentKeyIndex = 0

    // Multiple models support with selection
    private var selectedModels = mutableListOf<String>()
    private var currentModelIndex = 0

    // Cached models (apiKey:modelName -> GenerativeModel)
    private val modelCache = mutableMapOf<String, GenerativeModel>()

    private val PREF_NAME_LEGACY = "AppPrefs"
    private val PREF_NAME_SECURE = "secure_app_prefs"
    private val KEY_API_KEYS = "GEMINI_API_KEYS"
    private val KEY_SELECTED_MODELS = "SELECTED_MODELS"
    private val KEY_MODEL_1_NAME = "MODEL_1_NAME"
    private val KEY_MODEL_1_ENABLED = "MODEL_1_ENABLED"
    private val KEY_MODEL_2_NAME = "MODEL_2_NAME"
    private val KEY_MODEL_2_ENABLED = "MODEL_2_ENABLED"
    private val KEY_MODEL_3_NAME = "MODEL_3_NAME"
    private val KEY_MODEL_3_ENABLED = "MODEL_3_ENABLED"

    private var model1Name = "gemini-3.5-flash"
    private var model1Enabled = true
    private var model2Name = "gemini-3.5-flash-lite"
    private var model2Enabled = true
    private var model3Name = "gemini-3.8-flash"
    private var model3Enabled = true

    private var cachedSecurePrefs: SharedPreferences? = null

    private val pickMultipleMediaLauncher = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            addSelectedImages(uris)
        }
    }

    private val pickMultipleFilesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            addSelectedImages(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvActiveModel = findViewById(R.id.tvActiveModel)
        etInputText = findViewById(R.id.etInputText)
        tvCharCount = findViewById(R.id.tvCharCount)
        btnPickImage = findViewById(R.id.btnPickImage)
        btnPaste = findViewById(R.id.btnPaste)
        btnClearAll = findViewById(R.id.btnClearAll)
        layoutImagePreview = findViewById(R.id.layoutImagePreview)
        tvImageCountBadge = findViewById(R.id.tvImageCountBadge)
        btnClearAllImages = findViewById(R.id.btnClearAllImages)
        layoutThumbnailsContainer = findViewById(R.id.layoutThumbnailsContainer)
        btnClearText = findViewById(R.id.btnClearText)
        rgTranslationStyle = findViewById(R.id.rgTranslationStyle)
        rbStyleAccurate = findViewById(R.id.rbStyleAccurate)
        rbStylePoetic = findViewById(R.id.rbStylePoetic)
        progressBar = findViewById(R.id.progressBar)
        layoutBtnLoading = findViewById(R.id.layoutBtnLoading)
        btnSend = findViewById(R.id.btnSend)
        btnSettings = findViewById(R.id.btnSettings)

        tvSourceLang = findViewById(R.id.tvSourceLang)
        tvTargetLang = findViewById(R.id.tvTargetLang)
        btnSwapLang = findViewById(R.id.btnSwapLang)

        loadLanguageSettings()
        updateLanguageViews()

        tvSourceLang.setOnClickListener {
            showSourceLanguageDialog()
        }

        tvTargetLang.setOnClickListener {
            showTargetLanguageDialog()
        }

        btnSwapLang.setOnClickListener {
            swapLanguages()
        }

        // Load saved translation style preference
        val prefs = getSecurePrefs()
        isPoeticStyle = prefs.getBoolean(KEY_POETIC_STYLE, false)
        if (isPoeticStyle) {
            rbStylePoetic.isChecked = true
        } else {
            rbStyleAccurate.isChecked = true
        }

        rgTranslationStyle.setOnCheckedChangeListener { _, checkedId ->
            isPoeticStyle = (checkedId == R.id.rbStylePoetic)
            prefs.edit().putBoolean(KEY_POETIC_STYLE, isPoeticStyle).apply()
        }

        btnClearText.setOnClickListener {
            etInputText.setText("")
            etInputText.requestFocus()
        }

        nestedScrollViewMain = findViewById(R.id.nestedScrollViewMain)
        cardResult = findViewById(R.id.cardResult)
        tvResult = findViewById(R.id.tvResult)
        btnCopyResult = findViewById(R.id.btnCopyResult)
        btnShareResult = findViewById(R.id.btnShareResult)
        btnClearResult = findViewById(R.id.btnClearResult)
        layoutResultMeta = findViewById(R.id.layoutResultMeta)
        tvResultModel = findViewById(R.id.tvResultModel)
        tvResultSpeed = findViewById(R.id.tvResultSpeed)

        loadSettings()
        updateActiveModelBadge()

        // Character counter
        etInputText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val len = s?.length ?: 0
                tvCharCount.text = "$len ký tự"
                btnClearText.visibility = if (len > 0) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Pick Image (up to 5 images)
        btnPickImage.setOnClickListener {
            launchImagePicker()
        }

        // Paste from clipboard
        btnPaste.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString() ?: ""
                if (text.isNotEmpty()) {
                    etInputText.setText(text)
                    etInputText.setSelection(text.length)
                } else {
                    Toast.makeText(this, "Bộ nhớ tạm không có văn bản!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Bộ nhớ tạm trống!", Toast.LENGTH_SHORT).show()
            }
        }

        // Clear all (input text, images, and result)
        btnClearAll.setOnClickListener {
            clearAll()
        }

        // Clear all images
        btnClearAllImages.setOnClickListener {
            clearSelectedImages()
        }

        // Settings dialog
        btnSettings.setOnClickListener {
            showSettingsDialog()
        }

        // Copy result
        btnCopyResult.setOnClickListener {
            copyResultToClipboard()
        }

        // Share result
        btnShareResult.setOnClickListener {
            shareResult()
        }

        // Clear result
        btnClearResult.setOnClickListener {
            clearResult()
        }

        // Send translation
        btnSend.setOnClickListener {
            val textInput = etInputText.text.toString().trim()

            if (apiKeys.isEmpty()) {
                Toast.makeText(this, "Vui lòng vào Cài đặt (⚙️) để nhập Gemini API Key!", Toast.LENGTH_LONG).show()
                showSettingsDialog()
                return@setOnClickListener
            }

            if (selectedModels.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 model trong Cài đặt!", Toast.LENGTH_SHORT).show()
                showSettingsDialog()
                return@setOnClickListener
            }

            if (textInput.isEmpty() && selectedBitmaps.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập văn bản hoặc chọn ảnh cần dịch!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isNetworkAvailable()) {
                Toast.makeText(this, "Không có kết nối Internet. Vui lòng kiểm tra lại mạng!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Hide keyboard and scroll to result
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.hideSoftInputFromWindow(currentFocus?.windowToken ?: etInputText.windowToken, 0)
            etInputText.clearFocus()

            nestedScrollViewMain.post {
                nestedScrollViewMain.smoothScrollTo(0, cardResult.top)
            }

            callGeminiApi(textInput, selectedBitmaps.toList())
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_SEND) {
            val mimeType = intent.type ?: ""
            if (mimeType.startsWith("text/")) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrBlank()) {
                    etInputText.setText(sharedText)
                    etInputText.setSelection(sharedText.length)
                }
            } else if (mimeType.startsWith("image/")) {
                val imageUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                }
                if (imageUri != null) {
                    addSelectedImages(listOf(imageUri))
                }
            }
        } else if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            val mimeType = intent.type ?: ""
            if (mimeType.startsWith("image/")) {
                val imageUris = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                }
                if (!imageUris.isNullOrEmpty()) {
                    addSelectedImages(imageUris)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        for (bitmap in selectedBitmaps) {
            bitmap.recycle()
        }
        selectedBitmaps.clear()
        modelCache.clear()
    }

    private fun extractLangName(langDisplay: String): String {
        val cleaned = langDisplay.replace(Regex("^[\\p{So}\\p{Sk}\\p{Cs}\\p{Cn}\\s]+"), "").trim()
        return cleaned.ifEmpty { langDisplay }
    }

    private fun showSourceLanguageDialog() {
        val currentIndex = sourceLanguages.indexOf(currentSourceLang).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Chọn ngôn ngữ nguồn")
            .setSingleChoiceItems(sourceLanguages, currentIndex) { dialog, which ->
                val selected = sourceLanguages[which]
                if (selected == currentTargetLang) {
                    currentTargetLang = if (selected == "🇻🇳 Tiếng Việt") "🇬🇧 Tiếng Anh" else "🇻🇳 Tiếng Việt"
                }
                currentSourceLang = selected
                updateLanguageViews()
                saveLanguageSettings()
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showTargetLanguageDialog() {
        val currentIndex = targetLanguages.indexOf(currentTargetLang).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Chọn ngôn ngữ đích")
            .setSingleChoiceItems(targetLanguages, currentIndex) { dialog, which ->
                val selected = targetLanguages[which]
                if (selected == currentSourceLang && !currentSourceLang.contains("Tự động")) {
                    currentSourceLang = if (selected == "🇻🇳 Tiếng Việt") "🇬🇧 Tiếng Anh" else "🇻🇳 Tiếng Việt"
                }
                currentTargetLang = selected
                updateLanguageViews()
                saveLanguageSettings()
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun swapLanguages() {
        if (currentSourceLang.contains("Tự động")) {
            val oldTarget = currentTargetLang
            currentSourceLang = oldTarget
            currentTargetLang = if (oldTarget == "🇻🇳 Tiếng Việt") "🇬🇧 Tiếng Anh" else "🇻🇳 Tiếng Việt"
            updateLanguageViews()
            saveLanguageSettings()
            return
        }
        val temp = currentSourceLang
        currentSourceLang = currentTargetLang
        currentTargetLang = temp
        updateLanguageViews()
        saveLanguageSettings()
    }

    private fun updateLanguageViews() {
        tvSourceLang.text = currentSourceLang
        tvTargetLang.text = currentTargetLang
    }

    private fun loadLanguageSettings() {
        val prefs = getSecurePrefs()
        currentSourceLang = prefs.getString(KEY_SOURCE_LANG, DEFAULT_SOURCE_LANG) ?: DEFAULT_SOURCE_LANG
        currentTargetLang = prefs.getString(KEY_TARGET_LANG, DEFAULT_TARGET_LANG) ?: DEFAULT_TARGET_LANG
    }

    private fun saveLanguageSettings() {
        val prefs = getSecurePrefs()
        prefs.edit()
            .putString(KEY_SOURCE_LANG, currentSourceLang)
            .putString(KEY_TARGET_LANG, currentTargetLang)
            .apply()
    }

    private fun updateActiveModelBadge() {
        val model = getCurrentModel()
        runOnUiThread {
            tvActiveModel.text = model
        }
    }

    private fun launchImagePicker() {
        val remaining = MAX_IMAGE_COUNT - selectedBitmaps.size
        if (remaining <= 0) {
            Toast.makeText(this, "Đã chọn tối đa $MAX_IMAGE_COUNT ảnh. Hãy xóa bớt nếu muốn thêm ảnh mới.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            pickMultipleMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (e: Exception) {
            try {
                pickMultipleFilesLauncher.launch("image/*")
            } catch (e2: Exception) {
                Toast.makeText(this, "Không thể mở bộ chọn ảnh trên thiết bị.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addSelectedImages(uris: List<Uri>) {
        val remaining = MAX_IMAGE_COUNT - selectedBitmaps.size
        if (remaining <= 0) {
            Toast.makeText(this, "Đã chọn tối đa $MAX_IMAGE_COUNT ảnh.", Toast.LENGTH_SHORT).show()
            return
        }

        val urisToProcess = uris.take(remaining)
        lifecycleScope.launch(Dispatchers.IO) {
            val loadedBitmaps = mutableListOf<Bitmap>()
            for (uri in urisToProcess) {
                val bitmap = decodeSampledBitmapFromUri(uri, maxDim = 1024)
                if (bitmap != null) {
                    loadedBitmaps.add(bitmap)
                }
            }
            withContext(Dispatchers.Main) {
                if (loadedBitmaps.isNotEmpty()) {
                    selectedBitmaps.addAll(loadedBitmaps)
                    updateImagePreviews()
                    if (uris.size > remaining) {
                        Toast.makeText(this@MainActivity, "Đã thêm ${loadedBitmaps.size} ảnh (tối đa $MAX_IMAGE_COUNT ảnh).", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Không thể đọc hoặc nén ảnh đã chọn.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateImagePreviews() {
        if (selectedBitmaps.isEmpty()) {
            layoutImagePreview.visibility = View.GONE
            layoutThumbnailsContainer.removeAllViews()
            return
        }

        layoutImagePreview.visibility = View.VISIBLE
        tvImageCountBadge.text = "${selectedBitmaps.size}/$MAX_IMAGE_COUNT"
        layoutThumbnailsContainer.removeAllViews()

        val inflater = LayoutInflater.from(this)

        for ((index, bitmap) in selectedBitmaps.withIndex()) {
            val itemView = inflater.inflate(R.layout.item_selected_image, layoutThumbnailsContainer, false)
            val ivThumbnail = itemView.findViewById<ImageView>(R.id.ivThumbnail)
            val tvIndex = itemView.findViewById<TextView>(R.id.tvThumbnailIndex)
            val btnRemove = itemView.findViewById<ImageButton>(R.id.btnRemoveThumbnail)
            val cardThumbnail = itemView.findViewById<View>(R.id.cardThumbnail)

            ivThumbnail.setImageBitmap(bitmap)
            tvIndex.text = "#${index + 1}"

            btnRemove.setOnClickListener {
                removeSelectedImageAt(index)
            }

            cardThumbnail.setOnClickListener {
                showImagePreviewDialog(bitmap, index + 1)
            }

            layoutThumbnailsContainer.addView(itemView)
        }

        // Add '+ Thêm' slot if not full
        if (selectedBitmaps.size < MAX_IMAGE_COUNT) {
            val addSlotView = inflater.inflate(R.layout.item_add_image_slot, layoutThumbnailsContainer, false)
            addSlotView.setOnClickListener {
                launchImagePicker()
            }
            layoutThumbnailsContainer.addView(addSlotView)
        }
    }

    private fun showImagePreviewDialog(bitmap: Bitmap, index: Int) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Ảnh #$index")
            .setPositiveButton("Đóng", null)
            .setNegativeButton("Xóa ảnh này") { _, _ ->
                val actualIndex = index - 1
                if (actualIndex in selectedBitmaps.indices) {
                    removeSelectedImageAt(actualIndex)
                }
            }
            .create()

        val iv = ImageView(this).apply {
            setImageBitmap(bitmap)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(16, 16, 16, 16)
        }
        dialog.setView(iv)
        dialog.show()
    }

    private fun removeSelectedImageAt(index: Int) {
        if (index in selectedBitmaps.indices) {
            val removed = selectedBitmaps.removeAt(index)
            removed.recycle()
            updateImagePreviews()
        }
    }

    private fun clearSelectedImages() {
        for (bitmap in selectedBitmaps) {
            bitmap.recycle()
        }
        selectedBitmaps.clear()
        updateImagePreviews()
    }

    private fun copyResultToClipboard() {
        val textToCopy = lastResultText ?: tvResult.text.toString()
        if (textToCopy.isNotBlank()) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Translated Text", textToCopy)
            clipboard.setPrimaryClip(clip)
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(this, "Đã sao chép!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareResult() {
        val textToShare = lastResultText ?: tvResult.text.toString()
        if (textToShare.isNotBlank()) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, textToShare)
            }
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ bản dịch qua:"))
        }
    }

    private fun clearAll() {
        etInputText.setText("")
        clearSelectedImages()
        clearResult()
    }

    private fun clearResult() {
        lastResultText = null
        tvResult.text = ""
        btnCopyResult.visibility = View.GONE
        btnShareResult.visibility = View.GONE
        btnClearResult.visibility = View.GONE
        layoutResultMeta.visibility = View.GONE
    }

    private fun getSecurePrefs(): SharedPreferences {
        cachedSecurePrefs?.let { return it }
        val prefs = try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                this,
                PREF_NAME_SECURE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            getSharedPreferences(PREF_NAME_LEGACY, Context.MODE_PRIVATE)
        }
        cachedSecurePrefs = prefs
        return prefs
    }

    private fun loadSettings() {
        val prefs = getSecurePrefs()
        var keysString = prefs.getString(KEY_API_KEYS, "") ?: ""

        // Check legacy migration
        if (keysString.isEmpty()) {
            val legacyPrefs = getSharedPreferences(PREF_NAME_LEGACY, Context.MODE_PRIVATE)
            val legacyKeys = legacyPrefs.getString(KEY_API_KEYS, "") ?: ""
            if (legacyKeys.isNotEmpty()) {
                keysString = legacyKeys
                prefs.edit().putString(KEY_API_KEYS, legacyKeys).apply()
                legacyPrefs.edit().remove(KEY_API_KEYS).apply()
            }
        }

        apiKeys = if (keysString.isNotEmpty()) {
            keysString.split("|||").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
        } else {
            mutableListOf()
        }

        // Load models per slot directly to prevent reset/shift
        val savedM1 = prefs.getString(KEY_MODEL_1_NAME, null)
        val savedM2 = prefs.getString(KEY_MODEL_2_NAME, null)
        val savedM3 = prefs.getString(KEY_MODEL_3_NAME, null)

        if (savedM1 == null && savedM2 == null && savedM3 == null) {
            val defaultModels = "gemini-3.5-flash|||gemini-3.5-flash-lite|||gemini-3.8-flash"
            val modelsString = prefs.getString(KEY_SELECTED_MODELS, defaultModels) ?: defaultModels
            val rawModels = modelsString.split("|||").map { it.trim() }.filter { it.isNotBlank() }
            model1Name = rawModels.getOrElse(0) { "gemini-3.5-flash" }
            model2Name = rawModels.getOrElse(1) { "gemini-3.5-flash-lite" }
            model3Name = rawModels.getOrElse(2) { "gemini-3.8-flash" }
            model1Enabled = true
            model2Enabled = rawModels.size > 1
            model3Enabled = rawModels.size > 2
        } else {
            model1Name = savedM1 ?: "gemini-3.5-flash"
            model2Name = savedM2 ?: "gemini-3.5-flash-lite"
            model3Name = savedM3 ?: "gemini-3.8-flash"
            model1Enabled = prefs.getBoolean(KEY_MODEL_1_ENABLED, true)
            model2Enabled = prefs.getBoolean(KEY_MODEL_2_ENABLED, true)
            model3Enabled = prefs.getBoolean(KEY_MODEL_3_ENABLED, true)
        }

        val deprecatedMap = mapOf(
            "gemini-1.5-flash" to "gemini-3.5-flash",
            "gemini-1.5-pro" to "gemini-3.5-flash-lite",
            "gemini-pro" to "gemini-3.5-flash"
        )
        model1Name = deprecatedMap[model1Name] ?: model1Name
        model2Name = deprecatedMap[model2Name] ?: model2Name
        model3Name = deprecatedMap[model3Name] ?: model3Name

        if (model1Name.isBlank()) model1Name = "gemini-3.5-flash"
        if (model2Name.isBlank()) model2Name = "gemini-3.5-flash-lite"
        if (model3Name.isBlank()) model3Name = "gemini-3.8-flash"

        val activeList = mutableListOf<String>()
        if (model1Enabled && model1Name.isNotBlank()) activeList.add(model1Name)
        if (model2Enabled && model2Name.isNotBlank()) activeList.add(model2Name)
        if (model3Enabled && model3Name.isNotBlank()) activeList.add(model3Name)

        selectedModels = activeList.distinct().toMutableList()
        if (selectedModels.isEmpty()) {
            selectedModels.add(model1Name)
            model1Enabled = true
        }
    }

    private fun decodeSampledBitmapFromUri(uri: Uri, maxDim: Int = 1024): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            val width = options.outWidth
            val height = options.outHeight
            if (width <= 0 || height <= 0) return null

            var inSampleSize = 1
            val largerSide = maxOf(width, height)
            while (largerSide / (inSampleSize * 2) >= maxDim) {
                inSampleSize *= 2
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            options.inPreferredConfig = Bitmap.Config.RGB_565

            var bitmap = contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            if (bitmap != null && (bitmap.width > maxDim || bitmap.height > maxDim)) {
                val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
                val targetW = (bitmap.width * scale).toInt()
                val targetH = (bitmap.height * scale).toInt()
                val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
                if (scaled != bitmap) {
                    bitmap.recycle()
                    bitmap = scaled
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showSettingsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)

        val etApiKey1 = dialogView.findViewById<EditText>(R.id.etApiKey1)
        val etApiKey2 = dialogView.findViewById<EditText>(R.id.etApiKey2)
        val etApiKey3 = dialogView.findViewById<EditText>(R.id.etApiKey3)

        val etModel1 = dialogView.findViewById<EditText>(R.id.etModel1)
        val etModel2 = dialogView.findViewById<EditText>(R.id.etModel2)
        val etModel3 = dialogView.findViewById<EditText>(R.id.etModel3)

        val cbModel1 = dialogView.findViewById<CheckBox>(R.id.cbModel1)
        val cbModel2 = dialogView.findViewById<CheckBox>(R.id.cbModel2)
        val cbModel3 = dialogView.findViewById<CheckBox>(R.id.cbModel3)

        val btnFetchModels = dialogView.findViewById<android.widget.Button>(R.id.btnFetchModels)
        val pbFetchModels = dialogView.findViewById<ProgressBar>(R.id.pbFetchModels)

        // Populate API Keys
        etApiKey1.setText(apiKeys.getOrElse(0) { "" })
        etApiKey2.setText(apiKeys.getOrElse(1) { "" })
        etApiKey3.setText(apiKeys.getOrElse(2) { "" })

        // Populate Models directly from slot variables
        etModel1.setText(model1Name)
        etModel2.setText(model2Name)
        etModel3.setText(model3Name)

        cbModel1.isChecked = model1Enabled
        cbModel2.isChecked = model2Enabled
        cbModel3.isChecked = model3Enabled

        if (!cbModel1.isChecked && !cbModel2.isChecked && !cbModel3.isChecked) {
            cbModel1.isChecked = true
        }

        btnFetchModels?.setOnClickListener {
            val keyToUse = listOf(etApiKey1, etApiKey2, etApiKey3)
                .map { it.text.toString().trim() }
                .firstOrNull { it.isNotEmpty() } ?: apiKeys.firstOrNull()

            if (keyToUse.isNullOrEmpty()) {
                Toast.makeText(this, "Vui lòng nhập API Key trước khi lấy danh sách model!", Toast.LENGTH_SHORT).show()
                etApiKey1.requestFocus()
                return@setOnClickListener
            }

            if (!isNetworkAvailable()) {
                Toast.makeText(this, "Không có kết nối Internet!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            pbFetchModels?.visibility = View.VISIBLE
            btnFetchModels.isEnabled = false
            btnFetchModels.text = "Đang lấy danh sách..."

            lifecycleScope.launch {
                val result = fetchAvailableModelsFromApi(keyToUse)
                pbFetchModels?.visibility = View.GONE
                btnFetchModels.isEnabled = true
                btnFetchModels.text = "🔄 Lấy danh sách model khả dụng"

                result.onSuccess { modelList ->
                    if (modelList.isEmpty()) {
                        Toast.makeText(this@MainActivity, "Không tìm thấy model nào hỗ trợ sinh văn bản!", Toast.LENGTH_SHORT).show()
                        return@onSuccess
                    }
                    showModelSelectionDialog(modelList, etModel1, etModel2, etModel3, cbModel1, cbModel2, cbModel3)
                }.onFailure { ex ->
                    val msg = when {
                        ex.message?.contains("400") == true || ex.message?.contains("403") == true ->
                            "API Key không hợp lệ hoặc bị từ chối truy cập!"
                        ex.message?.contains("Unable to resolve host") == true ->
                            "Không thể kết nối đến máy chủ Google, vui lòng kiểm tra mạng!"
                        else -> "Lỗi lấy danh sách model: ${ex.message}"
                    }
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                }
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Cài đặt Gemini")
            .setView(dialogView)
            .setPositiveButton("Lưu") { dialog, _ ->
                val newKeys = mutableListOf<String>()
                listOf(etApiKey1, etApiKey2, etApiKey3).forEach { et ->
                    val key = et.text.toString().trim()
                    if (key.isNotEmpty()) newKeys.add(key)
                }

                val m1 = etModel1.text.toString().trim()
                val m2 = etModel2.text.toString().trim()
                val m3 = etModel3.text.toString().trim()

                val newModels = mutableListOf<String>()
                if (cbModel1.isChecked && m1.isNotEmpty()) {
                    newModels.add(m1)
                }
                if (cbModel2.isChecked && m2.isNotEmpty()) {
                    newModels.add(m2)
                }
                if (cbModel3.isChecked && m3.isNotEmpty()) {
                    newModels.add(m3)
                }

                if (newKeys.isEmpty()) {
                    Toast.makeText(this, "Cần nhập ít nhất 1 API Key!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newModels.isEmpty()) {
                    Toast.makeText(this, "Cần chọn ít nhất 1 model!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                apiKeys = newKeys
                model1Name = m1.ifEmpty { "gemini-3.5-flash" }
                model1Enabled = cbModel1.isChecked
                model2Name = m2.ifEmpty { "gemini-3.5-flash-lite" }
                model2Enabled = cbModel2.isChecked
                model3Name = m3.ifEmpty { "gemini-3.8-flash" }
                model3Enabled = cbModel3.isChecked

                selectedModels = newModels.distinct().toMutableList()
                currentKeyIndex = 0
                currentModelIndex = 0
                modelCache.clear()

                val prefs = getSecurePrefs()
                prefs.edit()
                    .putString(KEY_API_KEYS, apiKeys.joinToString("|||"))
                    .putString(KEY_MODEL_1_NAME, model1Name)
                    .putBoolean(KEY_MODEL_1_ENABLED, model1Enabled)
                    .putString(KEY_MODEL_2_NAME, model2Name)
                    .putBoolean(KEY_MODEL_2_ENABLED, model2Enabled)
                    .putString(KEY_MODEL_3_NAME, model3Name)
                    .putBoolean(KEY_MODEL_3_ENABLED, model3Enabled)
                    .putString(KEY_SELECTED_MODELS, selectedModels.joinToString("|||"))
                    .apply()

                updateActiveModelBadge()
                Toast.makeText(this, "Đã lưu cài đặt!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }


    private suspend fun fetchAvailableModelsFromApi(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val jsonArray = json.optJSONArray("models") ?: JSONArray()
                val list = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val rawName = obj.optString("name", "")
                    val cleanName = rawName.removePrefix("models/")
                    val methods = obj.optJSONArray("supportedGenerationMethods")
                    var supportsGenerate = false
                    if (methods != null) {
                        for (j in 0 until methods.length()) {
                            if (methods.optString(j) == "generateContent") {
                                supportsGenerate = true
                                break
                            }
                        }
                    }
                    if (supportsGenerate && cleanName.contains("gemini", ignoreCase = true)
                        && !cleanName.contains("embedding", ignoreCase = true)
                        && !cleanName.contains("aqa", ignoreCase = true)
                        && !cleanName.contains("robotics", ignoreCase = true)
                        && !cleanName.contains("computer-use", ignoreCase = true)
                    ) {
                        list.add(cleanName)
                    }
                }

                list.sortWith { a, b ->
                    fun score(name: String): Int = when {
                        name.contains("3.5-flash-lite") -> 10
                        name.contains("3.5-flash") -> 20
                        name.contains("3.8-flash") -> 30
                        name.contains("2.5-flash") -> 40
                        name.contains("2.0-flash") -> 50
                        name.contains("flash") -> 60
                        name.contains("pro") -> 70
                        else -> 90
                    }
                    val sA = score(a)
                    val sB = score(b)
                    if (sA != sB) sA.compareTo(sB) else a.compareTo(b)
                }

                Result.success(list)
            } else {
                val errText = try {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (e: Exception) {
                    ""
                }
                Result.failure(Exception("HTTP $responseCode: $errText"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun showModelSelectionDialog(
        modelList: List<String>,
        etModel1: EditText,
        etModel2: EditText,
        etModel3: EditText,
        cbModel1: CheckBox,
        cbModel2: CheckBox,
        cbModel3: CheckBox
    ) {
        val items = modelList.toTypedArray()
        val checkedArray = BooleanArray(items.size) { i ->
            val name = items[i]
            (cbModel1.isChecked && etModel1.text.toString().trim() == name) ||
            (cbModel2.isChecked && etModel2.text.toString().trim() == name) ||
            (cbModel3.isChecked && etModel3.text.toString().trim() == name)
        }

        var selectionDialog: AlertDialog? = null
        val builder = AlertDialog.Builder(this)
            .setTitle("Chọn model hỗ trợ (${modelList.size})")
            .setMultiChoiceItems(items, checkedArray) { _, which, isChecked ->
                checkedArray[which] = isChecked
                val count = checkedArray.count { it }
                if (count > 3) {
                    checkedArray[which] = false
                    selectionDialog?.listView?.setItemChecked(which, false)
                    Toast.makeText(this, "Chỉ được chọn tối đa 3 model!", Toast.LENGTH_SHORT).show()
                }
            }
            .setPositiveButton("Áp dụng") { _, _ ->
                val selected = modelList.filterIndexed { index, _ -> checkedArray[index] }
                if (selected.isEmpty()) {
                    Toast.makeText(this, "Chưa chọn model nào!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                etModel1.setText(selected[0])
                cbModel1.isChecked = true

                if (selected.size > 1) {
                    etModel2.setText(selected[1])
                    cbModel2.isChecked = true
                } else {
                    cbModel2.isChecked = false
                }

                if (selected.size > 2) {
                    etModel3.setText(selected[2])
                    cbModel3.isChecked = true
                } else {
                    cbModel3.isChecked = false
                }

            }
            .setNeutralButton("Chọn Top 3") { _, _ ->
                if (modelList.isNotEmpty()) {
                    etModel1.setText(modelList[0])
                    cbModel1.isChecked = true
                }
                if (modelList.size > 1) {
                    etModel2.setText(modelList[1])
                    cbModel2.isChecked = true
                }
                if (modelList.size > 2) {
                    etModel3.setText(modelList[2])
                    cbModel3.isChecked = true
                }
            }
            .setNegativeButton("Hủy", null)

        selectionDialog = builder.create()
        selectionDialog.show()
    }

    private fun getNextApiKey(): Pair<Int, String>? {
        if (apiKeys.isEmpty()) return null
        val index = currentKeyIndex % apiKeys.size
        val key = apiKeys[index]
        currentKeyIndex++
        return Pair(index + 1, key)
    }

    private fun getCurrentModel(): String {
        if (selectedModels.isEmpty()) return "gemini-1.5-flash"
        return selectedModels[currentModelIndex % selectedModels.size]
    }

    private fun advanceToNextModel() {
        if (selectedModels.isNotEmpty()) {
            currentModelIndex = (currentModelIndex + 1) % selectedModels.size
            updateActiveModelBadge()
        }
    }

    @Synchronized
    private fun getModel(apiKey: String, modelName: String): GenerativeModel {
        val cacheKey = "$apiKey:$modelName"
        return modelCache.getOrPut(cacheKey) {
            GenerativeModel(
                modelName = modelName,
                apiKey = apiKey
            )
        }
    }

    private fun setTranslatingState(isTranslating: Boolean) {
        if (isTranslating) {
            btnSend.isEnabled = false
            btnPickImage.isEnabled = false
            btnPaste.isEnabled = false
            btnClearAll.isEnabled = false
            btnClearAllImages.isEnabled = false
            btnSend.text = ""
            layoutBtnLoading.visibility = View.VISIBLE
        } else {
            btnSend.isEnabled = true
            btnPickImage.isEnabled = true
            btnPaste.isEnabled = true
            btnClearAll.isEnabled = true
            btnClearAllImages.isEnabled = true
            btnSend.text = "✨  Dịch với Gemini AI"
            layoutBtnLoading.visibility = View.GONE
        }
    }

    private fun callGeminiApi(input: String, bitmaps: List<Bitmap>) {
        val startTime = System.currentTimeMillis()

        tvResult.text = "Đang xử lý dịch thuật..."
        setTranslatingState(true)
        btnCopyResult.visibility = View.GONE
        btnShareResult.visibility = View.GONE
        btnClearResult.visibility = View.GONE
        layoutResultMeta.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            var lastError: Exception? = null
            val maxAttempts = apiKeys.size

            val sourceClean = extractLangName(currentSourceLang)
            val targetClean = extractLangName(currentTargetLang)
            val sourceClause = if (currentSourceLang.contains("Tự động")) "" else "từ $sourceClean "

            val isPoetic = isPoeticStyle
            val hasImages = bitmaps.isNotEmpty()
            val imgCount = bitmaps.size
            val prompt = when {
                hasImages && input.isNotEmpty() ->
                    if (isPoetic) {
                        if (imgCount > 1) {
                            "Dựa vào $imgCount hình ảnh đính kèm (theo thứ tự từ ảnh 1 đến ảnh $imgCount) và yêu cầu sau: \"$input\", hãy dịch đầy đủ nội dung ${sourceClause}sang $targetClean với văn phong hoa mỹ, trau chuốt, giàu cảm xúc và chất thơ. Hãy gợi ý 2-3 phương án dịch hay nhất theo từng sắc thái ngữ cảnh kèm chú thích ngắn gọn."
                        } else {
                            "Dựa vào hình ảnh đính kèm và yêu cầu sau: \"$input\", hãy dịch ${sourceClause}sang $targetClean với văn phong hoa mỹ, trau chuốt, giàu cảm xúc và chất thơ. Hãy gợi ý 2-3 phương án dịch hay nhất theo từng sắc thái ngữ cảnh (nhẹ nhàng, lãng mạn, sâu lắng) kèm chú thích ngắn gọn."
                        }
                    } else {
                        if (imgCount > 1) {
                            "Dựa vào $imgCount hình ảnh đính kèm (theo thứ tự từ ảnh 1 đến ảnh $imgCount) và yêu cầu bổ sung sau: \"$input\", hãy dịch toàn bộ nội dung ${sourceClause}sang $targetClean. YÊU CẦU QUAN TRỌNG: Dịch đầy đủ nội dung theo đúng thứ tự các ảnh. Chỉ cung cấp DUY NHẤT một bản dịch $targetClean chuẩn xác, tự nhiên, đúng nghĩa nhất. KHÔNG giải thích, KHÔNG phân tích từ ngữ, KHÔNG thêm lời bình hay nhiều phương án."
                        } else {
                            "Dựa vào hình ảnh đính kèm và yêu cầu bổ sung sau: \"$input\", hãy dịch nội dung ${sourceClause}sang $targetClean. YÊU CẦU QUAN TRỌNG: Chỉ cung cấp DUY NHẤT một bản dịch $targetClean chuẩn xác, tự nhiên, đúng nghĩa nhất. KHÔNG giải thích, KHÔNG phân tích từ ngữ, KHÔNG thêm lời bình hay nhiều phương án."
                        }
                    }
                hasImages ->
                    if (isPoetic) {
                        if (imgCount > 1) {
                            "Hãy nhận diện toàn bộ chữ (OCR) trong $imgCount hình ảnh đính kèm theo thứ tự từ ảnh 1 đến ảnh $imgCount và dịch ${sourceClause}sang $targetClean với văn phong hoa mỹ, truyền cảm và tinh tế nhất. Gợi ý các phương án dịch hay theo ngữ cảnh kèm giải thích sắc thái từ ngữ."
                        } else {
                            "Hãy nhận diện chữ (OCR) trong ảnh và dịch ${sourceClause}sang $targetClean với văn phong hoa mỹ, truyền cảm và tinh tế nhất. Gợi ý các phương án dịch hay theo ngữ cảnh kèm giải thích sắc thái từ ngữ."
                        }
                    } else {
                        if (imgCount > 1) {
                            "Hãy nhận diện toàn bộ chữ (OCR) trong $imgCount hình ảnh đính kèm theo thứ tự từ ảnh 1 đến ảnh $imgCount và dịch trực tiếp ${sourceClause}sang $targetClean. YÊU CẦU QUAN TRỌNG: Dịch đầy đủ nội dung theo đúng thứ tự các ảnh. Chỉ trả về DUY NHẤT nội dung bản dịch $targetClean chuẩn xác, tự nhiên và đầy đủ nhất. KHÔNG giải thích, KHÔNG phân tích từ ngữ, KHÔNG thêm ghi chú bên lề."
                        } else {
                            "Hãy nhận diện toàn bộ chữ (OCR) trong ảnh và dịch trực tiếp ${sourceClause}sang $targetClean. YÊU CẦU QUAN TRỌNG: Chỉ trả về DUY NHẤT nội dung bản dịch $targetClean chuẩn xác, tự nhiên và đầy đủ nhất. KHÔNG giải thích, KHÔNG phân tích từ ngữ, KHÔNG thêm ghi chú bên lề."
                        }
                    }
                else ->
                    if (isPoetic) {
                        "Hãy dịch đoạn văn bản sau ${sourceClause}sang $targetClean với văn phong hoa mỹ, giàu cảm xúc, đậm chất thơ và trau chuốt tinh tế nhất.\nHãy trình bày các phương án dịch hay nhất theo từng ngữ cảnh biểu đạt (lãng mạn, sâu sắc, ngắn gọn) kèm phân tích ngắn về sắc thái từ vựng để người đọc lựa chọn.\n\nVăn bản cần dịch:\n$input"
                    } else {
                        "Hãy dịch đoạn văn bản sau ${sourceClause}sang $targetClean một cách chuẩn xác, đúng ngữ cảnh và tự nhiên nhất.\n\nQUY TẮC BẮT BUỘC: CHỈ trả về DUY NHẤT bản dịch $targetClean hoàn chỉnh. Tuyệt đối KHÔNG thêm lời giải thích, KHÔNG phân tích cấu trúc ngữ pháp/từ vựng, KHÔNG kèm các phương án lựa chọn khác, KHÔNG thêm lời mở đầu hay kết bài.\n\nVăn bản cần dịch:\n$input"
                    }
            }

            for (attempt in 1..maxAttempts) {
                val keyPair = getNextApiKey() ?: break
                val keyNumber = keyPair.first
                val apiKey = keyPair.second
                val modelName = getCurrentModel()
                android.util.Log.i("AiTranslator", "Attempt $attempt with key #$keyNumber, model: $modelName")

                withContext(Dispatchers.Main) {
                    tvResult.text = "Đang dịch với $modelName (Key $keyNumber)..."
                }

                try {
                    val generativeModel = getModel(apiKey, modelName)

                    val response = if (hasImages) {
                        val inputContent = content {
                            for (bitmap in bitmaps) {
                                image(bitmap)
                            }
                            text(prompt)
                        }
                        generativeModel.generateContent(inputContent)
                    } else {
                        generativeModel.generateContent(prompt)
                    }

                    val resultText = response.text
                    val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0

                    withContext(Dispatchers.Main) {
                        setTranslatingState(false)

                        if (!resultText.isNullOrBlank()) {
                            lastResultText = resultText
                            tvResult.text = resultText
                            btnCopyResult.visibility = View.VISIBLE
                            btnShareResult.visibility = View.VISIBLE
                            btnClearResult.visibility = View.VISIBLE
                            layoutResultMeta.visibility = View.VISIBLE
                            tvResultModel.text = "$modelName • Key $keyNumber"
                            tvResultSpeed.text = "⚡ ${String.format("%.1fs", elapsedSeconds)}"

                            // Smooth scroll to top of result box so user reads from beginning
                            nestedScrollViewMain.post {
                                nestedScrollViewMain.smoothScrollTo(0, cardResult.top)
                            }
                        } else {
                            tvResult.text = "Không nhận được phản hồi từ AI."
                        }
                    }
                    return@launch

                } catch (e: Exception) {
                    android.util.Log.e("AiTranslator", "Gemini error attempt $attempt (Key $keyNumber, Model $modelName): ${e.message}", e)
                    lastError = e
                    val errorMsg = e.message ?: e.toString()

                    // Check for network errors
                    if (e is UnknownHostException || e is SocketTimeoutException || e is ConnectException ||
                        errorMsg.contains("Unable to resolve host") || errorMsg.contains("timeout")) {
                        withContext(Dispatchers.Main) {
                            setTranslatingState(false)
                            btnClearResult.visibility = View.VISIBLE
                            tvResult.text = "❌ Lỗi mạng: Không thể kết nối tới Google AI. Vui lòng kiểm tra lại kết nối Internet!"
                        }
                        return@launch
                    }

                    // Check for Rate limit / Quota errors
                    val isQuotaExhausted = errorMsg.contains("429") ||
                            errorMsg.contains("rate limit", ignoreCase = true) ||
                            errorMsg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                            errorMsg.contains("quota", ignoreCase = true)

                    if (isQuotaExhausted) {
                        withContext(Dispatchers.Main) {
                            tvResult.text = "⚠️ API Key $keyNumber hết quota, đang tự động chuyển sang Key tiếp theo..."
                        }
                        continue
                    }

                    // Check if model not found (404)
                    if (errorMsg.contains("404") || errorMsg.contains("NOT_FOUND")) {
                        advanceToNextModel()
                        val nextModel = getCurrentModel()
                        withContext(Dispatchers.Main) {
                            tvResult.text = "⚠️ Model $modelName không khả dụng, đang thử chuyển sang $nextModel..."
                        }
                        continue
                    }

                    // Other error
                    withContext(Dispatchers.Main) {
                        tvResult.text = "⚠️ Key $keyNumber gặp lỗi: $errorMsg. Thử key khác..."
                    }
                }
            }

            // All keys failed
            withContext(Dispatchers.Main) {
                setTranslatingState(false)
                btnClearResult.visibility = View.VISIBLE
                val errMsg = lastError?.message ?: lastError?.toString() ?: "Unknown error"
                tvResult.text = "❌ Tất cả API Keys (${apiKeys.size} keys) đều thất bại:\n\n$errMsg"
            }
        }
    }
}
