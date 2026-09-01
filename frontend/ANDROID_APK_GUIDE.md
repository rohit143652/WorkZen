# WORKZEN - Android APK बनवण्यासाठी मार्गदर्शक

Capacitor चा संपूर्ण setup आधीच project मध्ये तयार करून ठेवलाय (`frontend/android` folder, GPS permissions, geolocation plugin — सगळं). आता फक्त खालच्या पायऱ्या तुमच्या स्वतःच्या laptop वर, Android Studio install करून पूर्ण करायच्या आहेत.

## आवश्यक Software (एकदाच install करा)

1. **Android Studio** — https://developer.android.com/studio (त्यासोबत Android SDK आपोआप येतो)
2. **Node.js** (आधीच project साठी लागतोच)
3. **JDK 17** (Android Studio सोबत बहुतेक वेळा आधीच येतो)

## पायऱ्या

### Step 1 — Project fresh करा
```powershell
cd frontend
npm install
```

### Step 2 — Angular चा production build करा
```powershell
ng build --configuration=production
```

### Step 3 — नवीन build Android project मध्ये copy करा
```powershell
npx cap sync android
```
(हे command दर वेळी नवीन बदल केल्यावर, आणि APK बनवण्याआधी चालवा — सगळ्यात महत्त्वाची पायरी.)

### Step 4 — Android Studio मध्ये उघडा
```powershell
npx cap open android
```
हे थेट Android Studio उघडेल (आधी install केलेलं असेल तर).

### Step 5 — Android Studio मध्ये APK बनवा
वरच्या मेनूमधून:
**Build → Build Bundle(s) / APK(s) → Build APK(s)**

काही मिनिटांनी खालच्या-उजव्या कोपऱ्यात एक notification येईल — त्यावर **"locate"** क्लिक केलं की APK सापडेल (साधारण location: `frontend/android/app/build/outputs/apk/debug/app-debug.apk`).

हाच APK file फोन वर पाठवून थेट install करता येईल (testing साठी — "Unknown sources" ची परवानगी द्यावी लागेल फोन वर).

---

## महत्त्वाच्या गोष्टी (आधीच सेट करून ठेवल्यात, फक्त माहितीसाठी)

| काय | कुठे | स्थिती |
|---|---|---|
| Backend URL | `frontend/src/environments/environment.prod.ts` | ✅ आधीच तुमच्या Render backend कडे point करतंय |
| App चं नाव | `capacitor.config.ts` → `appName` | "WORKZEN" |
| Package ID | `capacitor.config.ts` → `appId` | `com.workzen.app` (Play Store वर टाकायचं असेल तर हे बदलू शकता) |
| GPS Permission | `android/app/src/main/AndroidManifest.xml` | ✅ जोडलेली — Attendance च्या GPS check साठी आवश्यक |
| Geolocation Plugin | `@capacitor/geolocation` | ✅ Install केलेला |

## पुढे (ऐच्छिक) काय करता येईल

- **App Icon बदलणे** — सध्या Capacitor चा default icon आहे. तुमचा WORKZEN logo वापरून icon बनवायचा असेल तर `@capacitor/assets` tool वापरता येईल.
- **Play Store वर टाकणे** — त्यासाठी APK ऐवजी **signed AAB (Android App Bundle)** लागेल, आणि एक keystore बनवून sign करावं लागेल — हे Android Studio मधूनच करता येतं (**Build → Generate Signed Bundle/APK**).

## समस्या आल्यास

- **"App मध्ये काहीच दिसत नाही (पांढरी screen)"** → Step 2-3 (`ng build` + `npx cap sync android`) परत केलं का ते तपासा — Android project मध्ये जुनाच build राहिलेला असू शकतो.
- **"GPS काम करत नाही"** → फोन वर app ला Location permission manually द्यावी लागेल (Settings → Apps → WORKZEN → Permissions).
- **"Network error / API call fails"** → `environment.prod.ts` मधला backend URL बरोबर आहे का, आणि backend प्रत्यक्षात चालू आहे का ते तपासा.
