# GitHub

This repository is ready to push:

```bash
cd ~/loupdate
git remote add origin git@github.com:YOUR_USER/loupdate.git
git add -A
git status   # confirm no secrets.properties / .env
git commit -m "Initial Loupdate SDK (Android + Flutter + CLI)"
git push -u origin main
```

Do **not** commit:

- `local.properties`
- `examples/kotlin-app/secrets.properties`
- `cli/.env`
- any Application Key

Use the `*.example` files as templates.
