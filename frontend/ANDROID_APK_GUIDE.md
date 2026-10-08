# KarmicHR - Production Server + APK - संपूर्ण मार्गदर्शक

> **अपडेट:** Server ची सर्व setup (env file, systemd, Nginx, update) आता **"KarmicHR - Production Deployment & Server Operations Guide"** मध्ये आहे. ही फाईल फक्त Android (APK) भागासाठी ठेवली आहे. API चा पत्ता आता कोडमध्ये कुठेही जुना IP म्हणून लिहिलेला नाही: web ला `/api` (सापेक्ष), आणि APK साठी `environment.apk.ts` मधला `SERVER_HOST` placeholder, जो build आधी बदलायचा आहे.

Code मधले आवश्यक बदल **आधीच केलेले आहेत** (खाली "आधीच झालेलं" section मध्ये बघा). इथून पुढे फक्त deployment/build च्या पायऱ्या आहेत, त्या तुमच्याच server आणि laptop वर कराव्या लागतील.

---

## आधीच झालेलं (Code मध्ये) — फक्त माहितीसाठी

| काय | कुठे | आता |
|---|---|---|
| Web app ला API कुठे सापडतं | `frontend/src/environments/environment.prod.ts` आणि `environment.ts` | सापेक्ष `/api` (कुठलाही IP लिहिलेला नाही; Nginx / `ng serve` proxy ते backend कडे पाठवतात) |
| APK ला API कुठे सापडतं | `frontend/src/environments/environment.apk.ts` | `http://SERVER_HOST/api` (placeholder; build आधी बदला) |
| APK मध्ये HTTP ला परवानगी | `frontend/android/app/src/main/res/xml/network_security_config.xml` | `SERVER_HOST` placeholder; फक्त त्या एका host साठी HTTP allow |

---

# भाग 1 — Backend Server

Server ची सर्व setup (Java, MySQL, env file, systemd, Nginx, update) **"KarmicHR - Production Deployment & Server Operations Guide"** च्या Section 0 ते 8 मध्ये आहे; इथे ती पुन्हा लिहिलेली नाही, म्हणजे दोन ठिकाणी वेगळं काहीतरी सांगितलेलं राहणार नाही.

APK साठी server वर एकच अट: **Nginx चालू असावा आणि Port 80 उघडा असावा** (Security Group मध्ये). APK `http://<SERVER_IP>/api` वर बोलतो; Port 8080 सार्वजनिकपणे उघडायची गरज नाही (backend फक्त त्या मशीनवर ऐकतो).

तपासा (तुमच्या laptop वरून): ब्राउझरमध्ये `http://<SERVER_IP>/api/auth/me` उघडा; `401` (Unauthorized) दिसलं तर server तयार आहे.

---

# भाग 2 — APK Build करा (तुमच्या Laptop वर)

### Step 1 — Android Studio Install करा (एकदाच)
https://developer.android.com/studio — Standard setup निवडा.

### Step 2 — Frontend Folder मध्ये जा
```powershell
cd workforce-auth\frontend
npm install
```

### Step 3 — पत्ता सेट करा आणि APK Build बनवा
दोन फाईल्समधला `SERVER_HOST` तुमच्या server च्या IP/नावाने बदला (PowerShell):
```powershell
$serverHost = "<SERVER_IP>"      # Production Deployment Guide मधला PUBLIC_HOST
(Get-Content src\environments\environment.apk.ts) -replace 'SERVER_HOST', $serverHost | Set-Content src\environments\environment.apk.ts
(Get-Content android\app\src\main\res\xml\network_security_config.xml) -replace 'SERVER_HOST', $serverHost | Set-Content android\app\src\main\res\xml\network_security_config.xml

npm ci
npx ng build --configuration=apk
```
`SERVER_HOST` placeholder मुद्दाम ठेवलेला आहे: हा step विसरलात तर APK जुन्या server शी गुपचूप बोलण्याऐवजी स्पष्ट "host सापडत नाही" असं फुटेल. पत्ता नंतर बदलायचा असेल तर आधी त्या दोन फाईल्स `git checkout -- <file>` ने किंवा हाताने `SERVER_HOST` वर परत आणा.

### Step 4 — Android Project मध्ये Sync करा
```powershell
npx cap sync android
```

### Step 5 — Android Studio उघडा
```powershell
npx cap open android
```

### Step 6 — APK Build करा
Android Studio च्या Menu मधून:
```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```
खालच्या-उजव्या कोपऱ्यातल्या notification मध्ये **"locate"** क्लिक करा.

### Step 7 — APK सापडेल इथे
```
frontend\android\app\build\outputs\apk\debug\app-debug.apk
```

---

# भाग 3 — Phone वर Install करून तपासा

1. `app-debug.apk` file phone वर पाठवा (WhatsApp/Email/USB)
2. Phone वर उघडून Install करा — **"Install from unknown sources"** ची परवानगी एकदा द्यावी लागेल
3. App उघडा → Login करा
4. जर Login/data दिसत असेल → **backend शी जोडणी बरोबर झालीये** ✅

## जर काही चूक झाली, तर इथे बघा

| समस्या | कारण | उपाय |
|---|---|---|
| App उघडतं, पण Login button दाबल्यावर काहीच होत नाही | Backend पोहोचत नाहीये | ब्राउझरमध्ये `http://<SERVER_IP>/api/auth/me` उघडून `401` येतंय का पहा; Port 80 उघडा आहे का, आणि `SERVER_HOST` दोन्ही फाईल्समध्ये बदललात का तेही तपासा |
| "Network Error" येतो | Backend बंद आहे, किंवा Firewall port block करतोय | Server वर `java -jar` अजून चालू आहे का बघा |
| Phone च्या Wi-Fi/Data शी काही संबंध | Phone आणि <SERVER_IP> दोघांनाही Internet द्वारे एकमेकांशी बोलता आलं पाहिजे — दोघेही same local network वर असायची गरज नाही, जोपर्यंत Server public IP वर उघडा आहे |
| GPS Attendance काम करत नाही | Phone Settings → Apps → KarmicHR → Permissions → Location चालू करा |

---

**सगळ्यात महत्त्वाचं लक्षात ठेवा:** server चा IP/नाव बदललं तर web app ला काहीच बदलावं लागत नाही (`/api` सापेक्ष आहे). फक्त APK साठी दोन फाईल्समधला `SERVER_HOST` पुन्हा बदलून APK नव्याने बनवा: `frontend/src/environments/environment.apk.ts` आणि `frontend/android/app/src/main/res/xml/network_security_config.xml`.
