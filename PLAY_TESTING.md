# Releasing Clock It to Google Play internal testing

Package name: **`com.zieuro.clock_it_demo`**. You can't change it after the first upload.

## 1. Signing key (keep this safe)
Release builds are signed with an **upload key** that lives outside git:

```
signing/upload-keystore.jks     # the key
signing/keystore.properties     # its passwords + alias (read by app/build.gradle.kts)
```

Back up both files somewhere private, such as a password manager. Play App Signing holds the real app-signing key, so if you lose the upload key you can ask Google to reset it. That takes a few days.

To build on another machine, put the two files in `signing/` at the project root. You can also point the `CLOCKIT_KEYSTORE_PROPERTIES` env var at the properties file; `storeFile` in it is relative to `app/`.

## 2. Build a bundle
```
./gradlew bundleRelease -PversionCode=2 -PversionName=1.0.1
# -> app/build/outputs/bundle/release/app-release.aab
```
**Each upload needs a higher `versionCode`.** The first one is 1, which is the default.

## 3. One-time Play Console setup
1. **Create app.** Name: Clock It. Choose App, Free. Accept the declarations.
2. **Testing → Internal testing → Testers.** Create an email list, add your Google account(s) and save. Copy the **opt-in link**.
3. **Internal testing → Create new release.**
   - Play App Signing: choose *Use Google-generated key*, which is the default.
   - Upload the `.aab`, add release notes, then **Save → Review release → Start rollout**.
4. Play will ask you to finish some **App content** items first:
   - **Privacy policy.** A URL is required. The app collects no data, so a one-paragraph page saying that is enough, e.g. a GitHub Pages or Gist page.
   - **Data safety.** No data collected or shared.
   - **Ads:** No. **Target audience:** 18+ (avoids extra children's-policy review). **Content rating:** fill in the questionnaire (Utility).
   - **Exact alarm permission (`USE_EXACT_ALARM`).** Declare that the app's core function is an **alarm clock**.
   - **Full-screen intent (`USE_FULL_SCREEN_INTENT`).** Declare **alarm clock** usage.
   - **Foreground service (`mediaPlayback`).** Explain: *"Plays the alarm sound and vibration while a user-scheduled alarm is ringing, until the user dismisses or snoozes it."* Play may ask for a short screen recording of an alarm ringing.
5. Open the opt-in link on your phone and accept. Then install from the Play Store link on that page. The first internal release usually becomes available within minutes to a few hours.

## 4. Later uploads
Bump `versionCode`, run `bundleRelease`, then create a new release on the internal testing track.
