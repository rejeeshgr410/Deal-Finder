# Deploy the business app to Firebase Hosting (free)

Everything is prepared. The build (`dist/`), `firebase.json` (with hosting + SPA
rewrites), and `.firebaserc` (pointing at project `gen-lang-client-0615495350`) are
all in place. You just run the deploy from your machine (it needs your Google login,
which can't be done remotely).

## One-time: install the Firebase CLI

```bash
npm install -g firebase-tools
```

## Deploy

From inside the `merchant-app` folder:

```bash
cd "C:\Deal Finder\merchant-app"

# 1. Log in (opens a browser; sign in with rejeeshgr95@gmail.com — the account
#    that owns the Firebase project)
firebase login

# 2. Deploy just the hosting site
firebase deploy --only hosting
```

When it finishes, the CLI prints a **Hosting URL** like:
`https://gen-lang-client-0615495350.web.app`
That's your live admin panel.

## Re-deploying after code changes

Whenever you change the business app:

```bash
cd "C:\Deal Finder\merchant-app"
npm install        # only needed the first time / when deps change
npm run build      # rebuilds dist/
firebase deploy --only hosting
```

## Notes
- The build is fully self-contained — no API keys or env vars are needed. The
  unused `@google/genai` dependency isn't referenced in the source; OCR uses the
  client-side `tesseract.js`.
- Hosting is free up to 10 GB storage and 360 MB/day transfer. An internal admin
  panel will not approach that, so expect $0.
- If `firebase deploy` ever asks you to "associate a site", just accept the default
  site — it matches the project ID.
- Want a nicer domain (e.g. admin.yourdomain.com)? Add it later under
  Hosting → Add custom domain in the Firebase console.
```
