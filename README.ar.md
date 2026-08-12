<!-- markdownlint-disable MD033 MD060 -->

<div dir="rtl" lang="ar">

<p align="center">
  <img src="assets/fukaha-logo.svg" alt="فكها" width="160" />
</p>

<h1 align="center">فكها — Fukaha</h1>

<p align="center">
  <strong>فكّ الروابط من التتبع. شاركها نظيفة.</strong><br/>
  من قائمة المشاركة في الجهاز — يشيل التتبع، يجهّز رابط معاينة،<br/>
  أو ينزّل الوسائط ويشاركها كملف.<br/>
  <span dir="ltr">Kotlin Multiplatform</span> · أندرويد (+ iOS قيد الإعداد).
</p>

<p align="center">
  <a href="https://github.com/Zyzto/Fukaha/releases/latest"><img alt="release" src="https://img.shields.io/github/v/release/Zyzto/Fukaha?style=flat-square&color=00687A" /></a>
  <a href="https://github.com/Zyzto/Fukaha"><img alt="repo" src="https://img.shields.io/badge/github-Zyzto%2FFukaha-C0C0C0?style=flat-square" /></a>
  <a href="https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/Zyzto/Fukaha/releases"><img alt="Obtainium" src="https://img.shields.io/badge/Obtainium-add-00687A?style=flat-square&logo=android&logoColor=white" /></a>
  <img alt="kotlin" src="https://img.shields.io/badge/Kotlin-Multiplatform-C0C0C0?style=flat-square&logo=kotlin&logoColor=white" />
  <img alt="android" src="https://img.shields.io/badge/Android-26%2B-00687A?style=flat-square&logo=android&logoColor=white" />
  <img alt="license" src="https://img.shields.io/badge/license-CC%20BY--NC--SA%204.0-00687A?style=flat-square" />
</p>

<p align="center">
  <a href="https://github.com/Zyzto/Fukaha/releases/latest"><strong>آخر إصدار</strong></a>
  ·
  <a href="#ماذا-تقدّم">ماذا تقدّم؟</a>
  ·
  <a href="#التثبيت">التثبيت</a>
  ·
  <a href="#التطوير">التطوير</a>
  <br/>
  <a href="README.md"><span dir="ltr">English</span></a>
</p>

<p align="center">
  الاسم من العربية: <strong>فكها</strong>
  (<span dir="ltr"><em>fakkahā</em></span>) — فكّ / إزالة التتبع والزيادة عن الرابط.<br/>
  والاسم اللاتيني <span dir="ltr"><strong>Fukaha</strong></span> مأخوذ منه.
</p>

</div>

---

<div dir="rtl" lang="ar">

## ماذا تقدّم؟

| | |
|---|---|
| **قائمة المشاركة** | تظهر في شير أندرويد للروابط والنص — ورقة سريعة، مو تطبيق تصفّح. |
| **رابط بدون تتبع** | يشيل `utm_*` و`fbclid` و`igshid` وأشباهها، وينظّم اسم المضيف. |
| **رابط معاينة** | يحوّله لخدمات مثل vxTwitter وddinstagram وfxTikTok من [قائمة Lexedia](https://gist.github.com/Lexedia/bbbde4dbbf628b0bfe8476a96a977a8f). |
| **ملف وسائط** | تحميل اختياري عبر Cobalt، وبعدين مشاركة الملف. |
| **الإعدادات** | الإجراء الافتراضي، خدمة المعاينة لكل منصة، Cobalt، الروابط المختصرة، ع/EN، الثيم. |
| **خفيف** | لا مزامنة بالخلفية ولا حسابات — الشبكة بس وقت ما تشارك. |

**عند المشاركة إلى فكها**

| الخيار | السلوك |
|--------|--------|
| **خلّني أختار** | يعرض الورقة (تنظيف / معاينة / تحميل / نسخ). |
| **رابط بدون تتبع / معاينة / حمّل** | ينفّذ فورًا ثم يفتح شير النظام. |

</div>

---

<div dir="rtl" lang="ar">

## التثبيت

### أندرويد

| الخيار | |
|--------|--|
| **Obtainium** (موصى به) | [![Obtainium](https://img.shields.io/badge/Obtainium-add-00687A?style=flat-square&logo=android&logoColor=white)](https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/Zyzto/Fukaha/releases) — يتابع [إصدارات GitHub](https://github.com/Zyzto/Fukaha/releases) |
| **APK** | حمّل من [آخر إصدار](https://github.com/Zyzto/Fukaha/releases/latest) |

### iOS

المصادر تحت `iosApp/` و`iosShareExtension/`. اربط إطار <span dir="ltr">Shared</span> في Xcode — التفاصيل في [iosApp/README.md](iosApp/README.md). ما في بناء متجر بعد.

</div>

---

<div dir="rtl" lang="ar">

## التطوير

**المتطلبات:** JDK 17 · Android SDK 35 · جهاز/محاكي (minSdk 26)

```bash
export ANDROID_HOME=~/Android/Sdk
./gradlew :shared:testDebugUnitTest
./gradlew :composeApp:assembleDebug
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

افتح **فكها** للإعدادات، أو شارك أي رابط سوشال إليه من تطبيق ثاني.

**تحميل الوسائط:** حط عنوان خادم Cobalt خاص في الإعدادات (ومفتاح إن لزم). الخدمة العامة محمية وما تنفع للتطبيقات.

</div>

---

<div dir="rtl" lang="ar">

## الرخصة

[CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/) — مشاركة وتعديل مع نسب العمل، **لغير التجاري** فقط، وبنفس الرخصة للمشتقات.  
النص الكامل: [LICENSE](LICENSE).

</div>
