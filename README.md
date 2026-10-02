# AI Translator

App dịch thuật Android sử dụng **Google Gemini AI** — hỗ trợ dịch văn bản và trích xuất dịch chữ từ ảnh (OCR).

## Tính năng

- **Dịch văn bản** — nhập trực tiếp để dịch sang tiếng Việt
- **OCR từ ảnh** — chọn ảnh, trích xuất chữ và dịch sang tiếng Việt
- **Nhiều API Keys** — dùng nhiều Gemini API key, tự động chuyển key khi hết quota
- **Nhiều Models** — chọn model Gemini phù hợp (gemini-2.5-flash, gemini-2.5-pro, ...)

## Yêu cầu

- Android 7.0+ (API 24)
- Gemini API Key ([lấy key tại Google AI Studio](https://aistudio.google.com/apikey))

## Cài đặt API Key

1. Nhấn nút ⚙️ (settings) trên màn hình chính
2. Nhập Gemini API Key vào ô API Key
3. Chọn model muốn sử dụng
4. Nhấn **Lưu**

## Tech Stack

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Kotlin |
| AI SDK | Google GenAI Android SDK `0.9.0` |
| Async | Kotlin Coroutines + Lifecycle |
| Giao diện | Android Views + Material Design |

## Cấu trúc source

```
app/src/main/
├── java/com/example/aitranslator/
│   └── MainActivity.kt          # Logic chính: UI, gọi Gemini API
├── res/
│   ├── layout/
│   │   ├── activity_main.xml    # Giao diện chính
│   │   └── dialog_settings.xml  # Dialog cài đặt
│   ├── mipmap-*/                 # Icon app (adaptive icon)
│   └── values/
│       ├── strings.xml
│       └── colors.xml
└── AndroidManifest.xml
```

## Build & Run

```bash
cd AiTranslator
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## APK

- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
