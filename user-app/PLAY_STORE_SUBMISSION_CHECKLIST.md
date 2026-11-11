# Play Store Submission Checklist — Deal Finder UAE

Tailored to this app (package `com.aistudio.dealfinderuae.jlkqpz`, app name
"Deal Finder UAE"). Account type chosen: **Personal** (no D-U-N-S needed).

---

## 0. Developer account (one-time)
- [ ] Create a Google Play Developer account at https://play.google.com/console
- [ ] Pay the **one-time $25** registration fee (credit/debit card; no prepaid)
- [ ] Complete **identity verification** (legal name, address, phone). Google is
      phasing this in as mandatory — do it now.
- [ ] Sign up as **Personal** (an individual). Skip the organization/D-U-N-S path.

## 1. Build artifact
- [ ] Produce a **signed release `.aab`** — see `RELEASE_SIGNING_GUIDE.md`
- [ ] Confirm `versionCode = 1`, `versionName = "1.0"` (bump versionCode each update)
- [ ] Keep the upload keystore backed up safely

## 2. Create the app in Play Console
- [ ] Console → **Create app**
- [ ] App name: **Deal Finder UAE** · Default language · Type: **App** · Free
- [ ] Accept the developer program policies / US export declarations

## 3. Store listing assets (prepare these before you start)
| Asset | Requirement |
|---|---|
| App icon | 512×512 PNG, 32-bit |
| Feature graphic | 1024×500 PNG/JPG (shown at top of listing) |
| Phone screenshots | 2–8 images, min 320px, 16:9 or 9:16 |
| Short description | ≤ 80 characters |
| Full description | ≤ 4000 characters |
| (Optional) 7-inch & 10-inch tablet screenshots | if you support tablets |

Listing copy ideas: emphasise *UAE supermarket flyers, weekly deals, price compare,
shopping list, coupons, nearby store offers*.

## 4. Privacy Policy (MANDATORY)
- [ ] Host a privacy policy at a public URL and enter it in the listing.
      Required because the app collects **location** and **account/email** data.
- [ ] It must cover: what's collected (email, approximate + precise location),
      why (auth, nearby deals), Firebase/Google as a processor, and how users can
      request deletion. (A simple hosted page or a Firebase Hosting page works.)

## 5. Data Safety form (answers tailored to this app)
The app's manifest declares: INTERNET, POST_NOTIFICATIONS, ACCESS_COARSE_LOCATION,
ACCESS_FINE_LOCATION. Declare the following:

**Data collected**
- [ ] **Location → Approximate location** — Collected. Purpose: App functionality
      (show nearby deals/stores). Not shared with third parties. Can mark optional.
- [ ] **Location → Precise location** — Collected (FINE_LOCATION). Purpose: App
      functionality. Optional (user grants at runtime).
- [ ] **Personal info → Email address** — Collected. Purpose: Account management /
      app functionality (Firebase Auth email login).
- [ ] **App activity / identifiers** — declare an FCM registration token if you treat
      it as a device identifier (used for push notifications).

**For each:** Is it shared? → No (processed by Google/Firebase as your service
provider, which is not "sharing"). Is it encrypted in transit? → Yes. Can users
request deletion? → Yes (describe how).

- [ ] Confirm whether data collection is **optional** (location is, since it's a
      runtime permission) vs required (email, for login).

## 6. App content / declarations
- [ ] **Content rating** questionnaire → likely rated **Everyone** (a shopping app)
- [ ] **Target audience & content** → adults / general audience (NOT directed at children)
- [ ] **Ads** → declare whether the app shows ads (currently: No)
- [ ] **Government apps / financial features** → No
- [ ] **Permissions declaration** → no sensitive permissions needing extra forms
      (location is standard; you'll just justify it in Data Safety). You are NOT using
      background location, so no special background-location review is required.
- [ ] **News app?** → No

## 7. Release
- [ ] Start with **Internal testing** track (instant, up to 100 testers) to sanity-
      check the signed build end-to-end against your live Firebase backend
- [ ] Then **Closed testing** if you want a wider beta
- [ ] Then **Production** → submit for review

> New personal accounts may be subject to a **closed-testing requirement** (a period
> of testing with a minimum number of testers) before they can publish to Production.
> Check the current rule in Play Console when you reach the release step.

## 8. Pre-launch sanity checks
- [ ] Install the signed `.aab`/APK on a real device, sign up with email, confirm
      flyers/products load from Firestore and flyer images load from Storage
- [ ] Confirm location prompt appears and nearby content works
- [ ] Confirm a flyer published in the business app appears in the consumer app

---

### Reminder about the backend
The app talks to Firebase project `gen-lang-client-0615495350` (Blaze). Publishing to
Play does not change that — Play only hosts the app binary. Keep the budget alert
(already set) so traffic growth doesn't surprise you.
