# Project Guidelines & Rules

- **Không tự động commit hoặc push code**: Tuyệt đối không tự ý chạy `git commit` hay `git push` trừ khi người dùng yêu cầu rõ ràng.
- **Không tự động build APK release**: Chỉ kiểm tra code bằng các lệnh biên dịch như `./gradlew compileDebugKotlin` hoặc `./gradlew compileReleaseKotlin`. Không tự ý chạy `./build-apk.sh release` hay tạo file APK release trừ khi người dùng yêu cầu cụ thể.
