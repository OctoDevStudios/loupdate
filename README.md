# Loupdate

Force or suggest in-app updates for Android apps **outside the Play Store**.

Works with **Kotlin / Java** and **Flutter**. Updates are stored on private **S3-compatible** object storage (Backblaze B2 recommended — free tier, no card, large APKs OK).

```kotlin
Loupdate.init(
    this,
    LoupdateConfig(
        endpoint = "https://s3.eu-central-003.backblazeb2.com",
        region = "eu-central-003",
        bucket = "Loupdate",
        accessKeyId = BuildConfig.LOUPDATE_KEY_ID,       // read-only key
        secretAccessKey = BuildConfig.LOUPDATE_KEY_SECRET,
        appId = "my-shop",                            // folder in the bucket
    )
)
Loupdate.check(this) // call on launch (e.g. MainActivity.onCreate)
```

- Soft update → dismissible dialog  
- Force update → full-screen, app blocked until installed  
- Download + install **inside the app** (progress bar)

## Quick start (5 minutes)

### 1. Storage (Backblaze B2)

1. Create a **private** bucket (e.g. `Loupdate`)
2. Create two [Application Keys](https://secure.backblaze.com/app_keys.htm):
   - `loupdate-app-read` → **Read Only** on that bucket → embed in the app
   - `loupdate-publish` → **Read and Write** → only on your machine / CI (**never** in the APK)

Details: [docs/hosting-b2.md](docs/hosting-b2.md)

### 2. Android (Kotlin)

```gradle
implementation("com.loupdate:loupdate-core:0.1.0")
// or include the module from this repo while developing
```

See [examples/kotlin-app](examples/kotlin-app).

### 3. Publish an update

```bash
cd cli && pip install -e .
export LOUPDATE_ENDPOINT=https://s3.eu-central-003.backblazeb2.com
export LOUPDATE_REGION=eu-central-003
export LOUPDATE_BUCKET=Loupdate
export LOUPDATE_ACCESS_KEY_ID=...      # publish key
export LOUPDATE_SECRET_ACCESS_KEY=...

loupdate publish --app my-shop --apk ./app-release.apk --force false
```

This uploads `my-shop/app-<versionCode>.apk` and writes `my-shop/update.json`.

### 4. Flutter

```yaml
dependencies:
  loupdate:
    path: ../flutter/loupdate   # or pub.dev later
```

```dart
await Loupdate.init(LoupdateConfig(
  endpoint: '...',
  region: '...',
  bucket: 'Loupdate',
  accessKeyId: '...',
  secretAccessKey: '...',
  appId: 'my-shop',
));
await Loupdate.check();
```

## Bucket layout (multi-app)

```
Loupdate/                 # one private bucket
  my-shop/update.json
  my-shop/app-42.apk
  my-crm/update.json
  my-crm/app-7.apk
```

## `update.json`

```json
{
  "versionCode": 42,
  "versionName": "2.1.0",
  "apkObject": "my-shop/app-42.apk",
  "force": false,
  "minVersionCode": 40,
  "title": "Nouvelle version",
  "message": "Corrections et améliorations.",
  "changelog": ["Fix crash", "Perf"],
  "sha256": "..."
}
```

Rules: update if `versionCode` > installed; **force** if `force: true` or installed `< minVersionCode`.

## Important

- Sign the new APK with the **same keystore** as the installed app.
- Use a **read-only** key in the client; never ship the publish / master key.
- Play Store apps: do **not** use sideload OTA (policy). Loupdate is for **off-Play** distribution.

## Repo layout

| Path | Role |
|------|------|
| `loupdate-core/` | Android library |
| `examples/kotlin-app/` | Sample app |
| `cli/` | `loupdate publish` |
| `flutter/loupdate/` | Flutter plugin |
| `docs/` | Hosting guides |

## License

MIT
