"""Loupdate CLI — publish APKs to private S3-compatible storage (B2, R2, …)."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

try:
    import boto3
    from botocore.config import Config
except ImportError:
    print("Install deps: pip install boto3", file=sys.stderr)
    sys.exit(1)


def env(name: str, default: str | None = None) -> str:
    value = os.environ.get(name, default)
    if not value:
        raise SystemExit(f"Missing env {name}")
    return value


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def parse_apk_badging(apk: Path) -> tuple[int | None, str | None]:
    aapt = shutil.which("aapt") or shutil.which("aapt2")
    if not aapt:
        return None, None
    try:
        out = subprocess.check_output(
            [aapt, "dump", "badging", str(apk)],
            stderr=subprocess.DEVNULL,
            text=True,
        )
    except Exception:
        return None, None
    code = None
    name = None
    m = re.search(r"versionCode='(\d+)'", out)
    if m:
        code = int(m.group(1))
    m = re.search(r"versionName='([^']*)'", out)
    if m:
        name = m.group(1)
    return code, name


def client_from_env():
    endpoint = env("LOUPDATE_ENDPOINT")
    region = env("LOUPDATE_REGION")
    return boto3.client(
        "s3",
        endpoint_url=endpoint,
        region_name=region,
        aws_access_key_id=env("LOUPDATE_ACCESS_KEY_ID"),
        aws_secret_access_key=env("LOUPDATE_SECRET_ACCESS_KEY"),
        config=Config(signature_version="s3v4"),
    )


def cmd_publish(args: argparse.Namespace) -> None:
    apk = Path(args.apk).expanduser().resolve()
    if not apk.is_file():
        raise SystemExit(f"APK not found: {apk}")

    version_code = args.version_code
    version_name = args.version_name
    parsed_code, parsed_name = parse_apk_badging(apk)
    if version_code is None:
        version_code = parsed_code
    if version_name is None:
        version_name = parsed_name or (str(version_code) if version_code else None)
    if version_code is None:
        raise SystemExit(
            "Could not detect versionCode. Pass --version-code (and ideally --version-name)."
        )

    app_id = args.app
    bucket = env("LOUPDATE_BUCKET")
    digest = sha256_file(apk)
    apk_key = f"{app_id}/app-{version_code}.apk"
    manifest_key = f"{app_id}/update.json"

    manifest = {
        "versionCode": int(version_code),
        "versionName": version_name,
        "apkObject": apk_key,
        "force": bool(args.force),
        "minVersionCode": int(args.min_version_code)
        if args.min_version_code is not None
        else 0,
        "title": args.title,
        "message": args.message,
        "changelog": args.changelog or [],
        "sha256": digest,
    }

    s3 = client_from_env()
    print(f"Uploading s3://{bucket}/{apk_key} …")
    s3.upload_file(
        str(apk),
        bucket,
        apk_key,
        ExtraArgs={"ContentType": "application/vnd.android.package-archive"},
    )
    body = json.dumps(manifest, indent=2, ensure_ascii=False).encode("utf-8")
    print(f"Writing s3://{bucket}/{manifest_key} …")
    s3.put_object(
        Bucket=bucket,
        Key=manifest_key,
        Body=body,
        ContentType="application/json",
    )
    print("Done.")
    print(json.dumps(manifest, indent=2, ensure_ascii=False))


def main(argv: list[str] | None = None) -> None:
    parser = argparse.ArgumentParser(
        prog="loupdate",
        description="Publish Android updates to private S3-compatible storage for Loupdate.",
    )
    sub = parser.add_subparsers(dest="command", required=True)

    p = sub.add_parser("publish", help="Upload APK + update.json")
    p.add_argument("--app", required=True, help="App id / folder prefix (e.g. my-shop)")
    p.add_argument("--apk", required=True, help="Path to signed APK")
    p.add_argument("--force", action="store_true", help="Force update (block app)")
    p.add_argument("--min-version-code", type=int, default=None)
    p.add_argument("--version-code", type=int, default=None)
    p.add_argument("--version-name", default=None)
    p.add_argument("--title", default="Update available")
    p.add_argument("--message", default="A new version is ready.")
    p.add_argument("--changelog", action="append", default=[])
    p.set_defaults(func=cmd_publish)

    args = parser.parse_args(argv)
    args.func(args)


if __name__ == "__main__":
    main()
