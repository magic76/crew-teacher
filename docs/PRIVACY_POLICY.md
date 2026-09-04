# Privacy Policy for Crew Teacher

**Last Updated:** September 4, 2026

**Crew Teacher** ("we", "our", or "the App") is an AI-powered language tutor mobile application designed to help users practice oral speaking and pronunciation. We take your privacy seriously. This Privacy Policy outlines how your information is handled.

---

### 1. Information Collection & Usage

#### A. Microphone & Audio Data (`RECORD_AUDIO`)
* **Purpose**: The App requires microphone access solely for real-time oral language practice, speech-to-text processing, and pronunciation assessment.
* **Storage**: We **DO NOT** store, record, sell, or archive your audio data on external permanent storage servers.
* **Transmission**: When you initiate a live conversation, your audio is streamed via secure end-to-end encrypted connections (TLS/WSS) directly to the AI service provider (Google Gemini Live API) for real-time speech synthesis and conversational feedback.

#### B. Foreground Service with Microphone (`FOREGROUND_SERVICE_MICROPHONE`)
* **Purpose**: When you enable the Floating Desktop Bubble mode, the App runs a foreground service with a visible persistent notification to keep the 1-on-1 voice tutoring session active while you switch to other apps or the home screen.
* **User Control**: You can hang up or mute the call at any time directly from the floating bubble or notification.

#### C. Floating Bubble Overlay (`SYSTEM_ALERT_WINDOW`)
* **Purpose**: Optional permission to display the interactive Cyber Orb floating window over other applications during hands-free speaking practice.

#### D. API Key & Local Preferences
* **Storage**: Your personal API key (e.g. Gemini API Key) and user preferences (voice tone, language selection, noise suppression) are stored **locally** on your device using Android encrypted `SharedPreferences`. They are never transmitted to our own third-party databases.

---

### 2. Third-Party Services
The App integrates with Google Generative AI (Gemini Live API) to provide intelligent conversational voice capabilities. Your interactions with the Gemini API are governed by Google's Privacy Policy and API Terms of Service.

---

### 3. Data Security
* All network traffic is encrypted using industry-standard HTTPS/WSS protocols.
* The App does not collect personal identity data (PII) such as phone numbers, real names, or location data.

---

### 4. Children's Privacy
The App does not knowingly collect personal data from children under the age of 13.

---

### 5. Contact Us
If you have any questions or feedback regarding this Privacy Policy, please reach out to us via GitHub:
[https://github.com/magic76/crew-teacher](https://github.com/magic76/crew-teacher)
