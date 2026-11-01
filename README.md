# AK SUPER AGENT AI — Stage 2

What is in this folder:

- `android/` — Kotlin + Jetpack Compose app with the ANIKA logo as app icon and on the first screen.
  - First screen: PUBLIC USER and OWNER / ADMIN buttons (login comes in Stage 3).
  - "Server & device check": calls `<your server>/health` and shows this phone's RAM, storage, CPU and which offline model size fits.
- `backend/` — FastAPI with `/health` and tests.
- `.github/workflows/build.yml` — builds the APK and runs backend tests on GitHub for free.

## Get the APK from your phone (no computer needed)

1. Create a free GitHub account, then a new **private** repository.
2. Upload every file in this folder, keeping the folders (`android`, `backend`, `.github`).
   Tip: on a phone, use github.com in "Desktop site" mode → "Add file" → "Upload files".
3. Open the **Actions** tab. The "Build APK and test backend" run starts by itself (or press "Run workflow").
4. When it shows a green tick, open the run → **Artifacts** → download `AKSuperAgent-debug-apk`.
5. Unzip it and install `app-debug.apk` (allow "Install unknown apps" for your browser/files app).

This is a **debug** APK for testing. A signed release APK/AAB for Play Store comes at Stage 13.

## Run the backend on a computer or server

```
cd backend
pip install -r requirements.txt
python -m pytest -q
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

In the app, enter `http://<server-ip>:8000` (debug build allows plain HTTP for testing).

## Honest status

- The Android code and workflow were written but **not compiled in Claude's workspace** (no Android SDK, downloads blocked there). The first GitHub build is the real test; if it fails, send the error log.
- Backend tests are written; they run on GitHub (they could not run in Claude's workspace).
- No passwords or API keys are in this project.
