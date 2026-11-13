# Dash Drift Android project

## এই সংস্করণে যা আছে
- আগের Dash Drift HTML runner Android WebView assets-এ যুক্ত করা হয়েছে।
- Android package: `com.dashdrift.game`
- Google Play Billing Library 9.1.0-এর native Kotlin integration scaffold যুক্ত।
- ছয়টি one-time consumable product ID নির্ধারণ করা হয়েছে:
  - `coins_101` — $1.01
  - `coins_201` — $2.01
  - `coins_301` — $3.01
  - `coins_401` — $4.01
  - `coins_501` — $5.01
  - `coins_551` — $5.51
- GitHub Actions workflow দিয়ে debug APK build করা যাবে।

## গুরুত্বপূর্ণ: এখনো যা সম্পূর্ণ হয়নি
1. Coin rewards আপাতত অস্থায়ীভাবে 100/200/300/400/500/550 রাখা হয়েছে; চূড়ান্ত করার আগে ব্যবহারকারীর অনুমোদন প্রয়োজন।
2. Play Console-এ একই product ID-গুলো তৈরি করে প্রতিটির USD base price এবং Bangladesh-সহ প্রয়োজনীয় দেশের price সেট করতে হবে। Google Play checkout-এ স্থানীয়ভাবে প্রযোজ্য দাম দেখাবে।
3. এটি development scaffold। Production release-এর আগে secure backend purchase-token verification, duplicate/refund/revocation handling, account-linked coin balance এবং server-side consumption যোগ করা জরুরি। বর্তমানে client-side consume + local purchase ledger আছে; এটি production-grade fraud protection নয়।
4. Play Console developer account এবং payment profile না থাকলে real purchase checkout চালু হবে না।
5. Release signing, app icon, privacy policy, Data safety, content rating, internal testing এবং Play review এখনো বাকি।

## Windows 10-এ build
- Android Studio (সম্ভব হলে) অথবা GitHub repository-তে project upload করে Actions tab থেকে `Build Dash Drift APK` চালানো যাবে।
- Build artifact হিসেবে `app-debug.apk` পাওয়া যাবে।
- 4 GB RAM-এর ল্যাপটপে Android Studio ভারী হতে পারে; GitHub Actions cloud build ব্যবহার করলে স্থানীয় RAM কম লাগবে।

## Build
JDK 17 এবং Gradle 8.13 দিয়ে:

```bash
gradle assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`
