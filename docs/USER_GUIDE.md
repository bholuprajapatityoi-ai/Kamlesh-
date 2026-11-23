# GOLU — Complete User Guide (Phases 1–15)

Welcome, **Boss**! GOLU is your full-stack personal AI voice assistant supporting **Hindi**, **Hinglish**, and **English**, powered by a hybrid architecture (on-device offline command engine + Android system actions + FastAPI/PostgreSQL/Redis/OpenAI backend).

---

## 1. Installation & Automatic Backend Connection
1. Install the GOLU Android app (`com.aistudio.golu.xkmqzp`) and open it.
2. Offline commands (`Time kya hua?`, `Battery kitni hai?`, `Alarm lagao 07:00`, opening apps, volume/media control, local reminders) work immediately.
3. **Automatic Cloud Connection:** GOLU connects automatically to its preconfigured HTTPS FastAPI backend on launch and automatically retries when your network recovers—no manual URL entry is required. You can inspect **API Health**, **Database Health**, **Last Connected**, or tap **Retry Connection** under **Settings → Automatic Backend Connection**.
4. Open the **Profile** tab to Register or Sign In for Cloud AI and cross-device PostgreSQL sync.

---

## 2. Wake Phrase & Voice Controls (Phases 4–5)
- **Activation Phrase:** Say `"Hey Golu"` when Wake-Word monitoring is enabled in the **Assistant** tab, or tap the glowing GOLU orb / **Talk to GOLU** button.
- **Greeting:** `"Hello! Boss, GOLU ready hai!"`
- **Audio Controls:** Pause, Resume, or Stop speech output anytime.
- **Privacy:** Local wake-word monitoring runs only when explicitly turned on by you, uses a visible foreground microphone notification, never uploads continuous background audio, and never bypasses your Android lock screen.

---

## 3. Smart Personal Memory (Phase 7)
- **Explicit Save Only:** Say `"GOLU, ye yaad rakhna..."` (e.g., `"GOLU, ye yaad rakhna meri favourite app Spotify hai"`).
- GOLU proposes the category and content and waits for your **Confirm Save Memory** tap before saving.
- **Zero Silent Persistence & Secret Blocking:** Regular conversations are never saved silently; passwords, OTPs, PINs, private keys, and API tokens are automatically blocked.

---

## 4. Smart Phone Control & WhatsApp Assistant (Phases 8–9)
- **Phone Controls:** Adjust media volume, playback, OS settings, inspect device/OS info, set alarms, and open apps.
- **Contacts & Calls:** Grant runtime Contacts permission when prompted, save relationship nicknames (`Papa`, `Mummy`, `Brother`, `Sister`), resolve ambiguous matches cleanly, and confirm before opening the Android Phone Dialer (`ACTION_DIAL`).
- **WhatsApp Composer:** Draft replies or messages in **Hindi**, **Hinglish**, or **English**, edit freely, and tap **Confirm & Open WhatsApp** before handing off to WhatsApp.

---

## 5. Daily Life Assistant (Phase 10)
- Manage **Reminders** (local `WorkManager` notifications with **Done** and **Snooze 10m** actions + PostgreSQL sync), **Tasks**, **Timetable & Calendar Events**, and **Daily Routines & Morning/Evening Briefings**.

---

## 6. Family Mode & Papa-Friendly Interface (Phase 11)
- Create a family group, generate invite codes, and join with separate accounts while keeping private memories and chats strictly isolated.
- Enable **Papa / Elder-Friendly Mode** for extra-large high-contrast buttons and simplified Hindi/Hinglish controls.

---

## 7. Security Dashboard & Smart Automation (Phases 12–13)
- Configure a Keystore-encrypted 4–6 digit **App PIN Lock**, inspect/revoke active JWT sessions, wipe personal data, and build safe `Trigger → Condition → Action` automations.

---

## 8. Troubleshooting
| Symptom | Cause | Fix |
|---|---|---|
| `Offline` or `Error` status pill | Device offline or backend service unreachable | Check internet connection and tap **Retry Connection** under **Settings → Automatic Backend Connection** (offline commands remain active) |
| `401 Unauthorized` on Memory/Family/Security | Not signed in or expired session | Sign in under **Profile** tab |
| Contact search asks permission | `READ_CONTACTS` not granted | Tap Search → Allow Contacts permission, or add a Nickname Alias with phone hint |
| Reminders don't show banner on Android 13+ | Notification permission denied | Tap **Allow Reminder Notifications** in **Daily Life** |
| Forgotten App PIN | Keystore vault locked | Clear app storage in Android Settings or sign back in after reinstall |
