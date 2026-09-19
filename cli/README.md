# Loupdate CLI

Publish signed APKs + `update.json` to a private S3-compatible bucket.

```bash
cd cli
python3 -m venv .venv && source .venv/bin/activate
pip install -e .

export LOUPDATE_ENDPOINT=https://s3.eu-central-003.backblazeb2.com
export LOUPDATE_REGION=eu-central-003
export LOUPDATE_BUCKET=Loupdate
export LOUPDATE_ACCESS_KEY_ID=...       # publish key (read+write)
export LOUPDATE_SECRET_ACCESS_KEY=...

loupdate publish \
  --app my-shop \
  --apk ./app-release.apk \
  --force \
  --title "Mise à jour obligatoire" \
  --message "Cette version corrige un bug critique." \
  --changelog "Fix crash login"
```

If `aapt` / `aapt2` is on PATH, versionCode/Name are read from the APK.
Otherwise pass `--version-code` and `--version-name`.
