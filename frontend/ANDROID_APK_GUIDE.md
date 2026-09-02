# WORKZEN - Production Server (43.204.237.48) + APK - संपूर्ण मार्गदर्शक

Code मधले आवश्यक बदल **आधीच केलेले आहेत** (खाली "आधीच झालेलं" section मध्ये बघा). इथून पुढे फक्त deployment/build च्या पायऱ्या आहेत, त्या तुमच्याच server आणि laptop वर कराव्या लागतील.

---

## आधीच झालेलं (Code मध्ये) — फक्त माहितीसाठी

| काय | कुठे | बदल |
|---|---|---|
| Frontend backend ला कुठे शोधतं | `frontend/src/environments/environment.prod.ts` | `http://43.204.237.48:8080/api` |
| APK मध्ये HTTP ला परवानगी | `frontend/android/app/src/main/res/xml/network_security_config.xml` | फक्त याच IP साठी HTTP allow केलं (APK डीफॉल्टपणे प्लेन HTTP block करतो) |

---

# भाग 1 — Backend Server (43.204.237.48) वर सुरू करा

### Step 1 — Server ला SSH ने जोडा
```bash
ssh <तुमचं-username>@43.204.237.48
```

### Step 2 — Java 21 install आहे का तपासा (नसेल तर install करा)
```bash
java -version
```
नसेल तर:
```bash
sudo apt update
sudo apt install openjdk-21-jdk -y
```

### Step 3 — MySQL तयार आहे का तपासा
- Server वरच MySQL असेल, किंवा
- वेगळा (उदा. Aiven/RDS) database वापरत असाल

एकतर, तुम्हाला हे 4 values लागतील:
- Database URL (उदा. `jdbc:mysql://localhost:3306/workforce_auth`)
- Database Username
- Database Password
- एक strong JWT Secret (base64 string)

### Step 4 — Project Server वर आणा
```bash
# ZIP आधीच server वर upload केला असेल तर:
unzip workforce-auth-updated.zip
cd workforce-auth/backend
```

### Step 5 — Backend Build करा (JAR file बनवा)
```bash
mvn clean package -DskipTests
```
यामुळे `target/workzen.jar` तयार होईल.

### Step 6 — Environment Variables सेट करा
```bash
export DB_URL="jdbc:mysql://<तुमचा-db-host>:3306/workforce_auth?useSSL=true"
export DB_USERNAME="<तुमचं db username>"
export DB_PASSWORD="<तुमचं db password>"
export JWT_SECRET="<तुमचा base64 secret>"
export PORT=8080
export SPRING_PROFILES_ACTIVE=prod
export CORS_ALLOWED_ORIGINS="http://43.204.237.48"
```
**टीप:** `CORS_ALLOWED_ORIGINS` इथे तुमचं **Frontend (website)** जिथे host आहे तो actual URL टाका (browser मधून access करताना हे लागतं — APK ला थेट फरक पडत नाही, पण website साठी आवश्यक आहे).

### Step 7 — Backend सुरू करा
```bash
java -jar target/workzen.jar
```
Startup logs मध्ये हे दिसेल की नाही ते बघा:
```
Started Application in ... seconds
```

**कायमस्वरूपी चालू ठेवण्यासाठी** (SSH बंद केलं तरी चालू राहावं म्हणून), `nohup` किंवा `systemd service` वापरा:
```bash
nohup java -jar target/workzen.jar > app.log 2>&1 &
```

### Step 8 — Server च्या Firewall मध्ये Port 8080 उघडा
- जर AWS EC2 असेल → **Security Group** मध्ये Inbound Rule: Port `8080`, Source `0.0.0.0/0` (किंवा गरजेनुसार मर्यादित)
- जर plain Linux firewall असेल:
```bash
sudo ufw allow 8080
```

### Step 9 — बाहेरून तपासा (server सोडून, तुमच्या स्वतःच्या laptop वरून)
Browser मध्ये उघडा:
```
http://43.204.237.48:8080/api/auth/login
```
"Method Not Allowed" किंवा तत्सम JSON error आलं तरी चालेल — याचा अर्थ **backend पोहोचतंय**, चूक फक्त GET ऐवजी POST लागतो एवढीच आहे. काहीच न उघडणं (timeout) म्हणजे Port बंद आहे — Step 8 परत तपासा.

---

# भाग 2 — APK Build करा (तुमच्या Laptop वर)

### Step 1 — Android Studio Install करा (एकदाच)
https://developer.android.com/studio — Standard setup निवडा.

### Step 2 — Frontend Folder मध्ये जा
```powershell
cd workforce-auth\frontend
npm install
```

### Step 3 — Production Build बनवा
```powershell
ng build --configuration=production
```
हे आधीच `http://43.204.237.48:8080/api` कडे point करणारा build बनवेल (Code मध्ये आधीच सेट आहे).

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
| App उघडतं, पण Login button दाबल्यावर काहीच होत नाही | Backend पोहोचत नाहीये | भाग 1 चा Step 9 परत तपासा — Port 8080 उघडा आहे का |
| "Network Error" येतो | Backend बंद आहे, किंवा Firewall port block करतोय | Server वर `java -jar` अजून चालू आहे का बघा |
| Phone च्या Wi-Fi/Data शी काही संबंध | Phone आणि 43.204.237.48 दोघांनाही Internet द्वारे एकमेकांशी बोलता आलं पाहिजे — दोघेही same local network वर असायची गरज नाही, जोपर्यंत Server public IP वर उघडा आहे |
| GPS Attendance काम करत नाही | Phone Settings → Apps → WORKZEN → Permissions → Location चालू करा |

---

**सगळ्यात महत्त्वाचं लक्षात ठेवा:** पुढच्या वेळी backend चा IP/Port बदलला, तर **फक्त एकच file बदलावी लागेल**: `frontend/src/environments/environment.prod.ts`, आणि मग भाग 2 च्या Step 3-6 परत कराव्या लागतील (नवीन APK बनवावं लागेल).
