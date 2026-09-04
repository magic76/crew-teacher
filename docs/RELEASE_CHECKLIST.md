# Google Play Console 上架發布檢查與操作指引 (Release Checklist)

---

## 📋 1. Google Play Console 核心資料填寫指引

### A. 應用程式詳細資訊 (App Details)
* **應用程式名稱**: `Crew Teacher - AI 口語外教`
* **簡短說明**: `雙語即時字幕對照、全外語沉浸與斯巴達糾音特訓的 1 對 1 AI 口說導師`
* **完整說明**: 複製 `docs/STORE_LISTING.md` 中的完整文案。
* **隱私權政策網址 (Privacy Policy URL)**:
  建議放置於 GitHub Pages 或公開 URL，例如：
  `https://github.com/magic76/crew-teacher/blob/main/docs/PRIVACY_POLICY.md`

### B. 圖像與視覺素材規格 (Store Graphic Assets)
1. **應用程式圖示 (App Icon)**:
   * 尺寸: `512 x 512 px` (PNG 格式，32 位元色彩，最大 1MB)
2. **宣傳焦點圖 (Feature Graphic)**:
   * 尺寸: `1024 x 500 px` (JPG 或 PNG，最大 15MB)
3. **螢幕截圖 (Screenshots)**:
   * 至少 2~8 張手機直式截圖 (建議尺寸: `1080 x 2400 px` 或 `1080 x 1920 px`)
   * 建議截圖內容：
     1. 首頁多種教學模式選擇與自訂教材
     2. 雙語對照模式（英文語音 + 即時繁中翻譯與生字筆記）
     3. 斯巴達發音體檢診斷卡（標紅發音盲點與 IPA 音標）
     4. Cyber Orb 桌面懸浮靈動氣泡多工對話畫面

---

## 🔒 2. Google Play 權限與宣告聲明 (Permissions Declaration)

Google Play 審核對背景服務與麥克風權限非常嚴格，請依照以下說明填寫審核表單：

### ① 麥克風權限 (`RECORD_AUDIO`)
* **用途宣告**: 核心功能 (Core Feature)
* **說明**: 用於讓使用者與 AI 口語外師進行即時 1 對 1 語音對話與口說發音診斷。

### ② 前台服務 - 麥克風 (`FOREGROUND_SERVICE_MICROPHONE`)
* **用途宣告**: `Microphone (語音通話與多工對話)`
* **說明 (User-facing feature description)**:
  > "Crew Teacher provides a floating desktop widget that allows users to practice oral speaking continuously while reading reference materials or navigating other apps. The foreground service displays an ongoing notification with active call indicators and audio controls to ensure transparency."
* **測試用影片 (Demo Video)**: 錄製一段「點擊開始桌面氣泡對話 -> 切換到桌面繼續語音互動 -> 從通知欄/氣泡掛斷」的 15~30 秒操作影片連結。

### ③ 系統懸浮窗權限 (`SYSTEM_ALERT_WINDOW`)
* **用途宣告**: 桌面懸浮球互動介面（使用者可在設定中手動授權啟用）。

---

## 📦 3. 打包與發布二進位檔案 (Artifacts)

* **目前版本**: `v1.5.9` (`versionCode: 44`, `versionName: 1.5.9`)
* **Target SDK**: `Android 14 (API 34)`
* **64-bit 相容性**: 支援 `arm64-v8a`（含 16KB Page Size 支援）
* **編譯指令**:
  ```bash
  ./build.sh
  ```
  產出之 APK：`/sdcard/Download/CrewTeacher-v1.5.9.apk`

---

## 🔑 4. 生產環境發布金鑰（Production Keystore 建議）
* 提交至 Google Play 時，建議透過 Google Play App Signing（由 Google 管理金鑰），或使用專屬保管的正式簽名金鑰。
