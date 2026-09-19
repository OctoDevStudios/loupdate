# Hosting on Backblaze B2

Loupdate talks to any **S3-compatible** private bucket. B2 is the recommended free starting point (no credit card for the free tier, large files OK).

## 1. Create a private bucket

1. [Backblaze B2](https://www.backblaze.com/cloud-storage) → Buckets → **Create a Bucket**
2. Name e.g. `Loupdate`
3. Type: **Private**
4. Note the **Endpoint**, e.g. `s3.eu-central-003.backblazeb2.com`

## 2. Application keys (two keys)

| Key | Access | Where it lives |
|-----|--------|----------------|
| `loupdate-app-read` | **Read Only** on bucket `Loupdate` | Inside the Android / Flutter app |
| `loupdate-publish` | **Read and Write** on bucket `Loupdate` | Only CI / your laptop (CLI) |

Never put the publish key or the Master Application Key in an APK.

Optional: set **File name prefix** to `my-shop/` on the read key to lock it to one app folder.

## 3. Multi-app layout

One bucket, many apps:

```
Loupdate/
  my-shop/update.json
  my-shop/app-42.apk
  my-crm/update.json
  my-crm/app-7.apk
```

Each mobile app uses the same endpoint/bucket/keys (or a prefix-scoped read key) and a different `appId`.

## 4. Wire the SDK

```kotlin
Loupdate.init(
    context,
    LoupdateConfig(
        endpoint = "https://s3.eu-central-003.backblazeb2.com",
        region = "eu-central-003",
        bucket = "Loupdate",
        accessKeyId = "…",      // read-only keyID
        secretAccessKey = "…",  // read-only applicationKey
        appId = "my-shop",
    )
)
Loupdate.check(activity)
```

## 5. Publish

See [cli/README.md](../cli/README.md).

## Notes

- Free tier: 10 GB storage; egress free up to ~3× stored volume / month.
- APK must be signed with the **same keystore** as the installed app.
- Public buckets are not required and not recommended.
