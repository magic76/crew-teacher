# 📌 Crew Teacher Google Play 上架備忘錄 (Release Memo)

**最後更新時間：** 2026-09-04  
**當前發布版本：** `v1.5.9` (`versionCode: 44`, `versionName: 1.5.9`)  
**APK 本機路徑：** `/sdcard/Download/CrewTeacher-v1.5.9.apk` (或 `/sdcard/Download/CrewTeacher.apk`)

---

## 🎯 核心上架文件快速導引

| 文件 | 說明 | 連結 |
| :--- | :--- | :--- |
| **📋 上架檢查清單** | 權限宣告（麥克風/前台服務）、審核影片要求、圖片尺寸規格 | [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) |
| **📝 商店文案包** | 繁中與英文 App 名稱、80字短說明、4000字完整吸引人文案、截圖大標題 | [STORE_LISTING.md](STORE_LISTING.md) |
| **🔒 隱私權政策** | Google Play 上架必填隱私權政策 URL（本機儲存、不留存音訊） | [PRIVACY_POLICY.md](PRIVACY_POLICY.md) |

---

## 🛠️ 上架前需備齊的 3 項素材

### 1. 視覺圖檔 (Graphic Assets)
* **App 圖示**：`512 x 512 px` (PNG 32-bit, < 1MB)
* **宣傳焦點圖 (Feature Graphic)**：`1024 x 500 px` (JPG 或 PNG)
* **手機螢幕截圖**：4 ~ 6 張（尺寸建議 `1080 x 2400 px` 或 `1080 x 1920 px`），包含：
  1. 雙語即時對照（外語原文在上、繁中翻譯與生字筆記在下）
  2. 斯巴達發音體檢診斷卡（標紅錯誤音節與 IPA 音標示範）
  3. Cyber Orb 桌面懸浮靈動氣泡多工對話
  4. 多國語言與外師音色選擇

### 2. 前台服務審核影片 (Foreground Service Demo Video)
* 錄製一段 **15 ~ 30 秒**的手機螢幕錄影：
  1. 開啟 App 點擊開始「桌面氣泡對話」
  2. 跳回手機桌面繼續進行語音對話
  3. 下拉通知欄展示進行中的通知並掛斷
* 上傳至 YouTube (設為不公開) 或雲端硬碟，在 Google Play Console 宣告前台服務時填入連結。

### 3. Google Play Console 填寫
* **隱私權政策網址**：`https://github.com/magic76/crew-teacher/blob/main/docs/PRIVACY_POLICY.md`
* **文案**：直接複製 `docs/STORE_LISTING.md` 中的中英文內容貼上。
* **APK / AAB 上傳**：上傳 `/sdcard/Download/CrewTeacher-v1.5.9.apk`。
