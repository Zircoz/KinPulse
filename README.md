# KinPulse

KinPulse is an Android app for tracking blood sugar and blood pressure readings for yourself and
your family, in one place.

## Features

- Log blood sugar (with a meal context: fasting, before/after meal, random, bedtime) and blood
  pressure (systolic/diastolic + optional pulse) readings.
- Track multiple people — parents, kids, yourself — each as their own profile.
- Family sharing: invite people by email as **viewer** (view only) or **editor** (add & view).
- Invites are accepted in-app once the invitee signs up and verifies their email.
- A trend chart and running averages for each profile.
- Colour-coded ranges so an out-of-range reading is obvious at a glance.
- Export any profile's history to CSV.
- Built on Firestore's offline cache, so viewing and logging readings works without a connection;
  changes sync once you're back online.

## Setup

KinPulse talks to your own Firebase project — no data goes anywhere else.

1. Create a project at [the Firebase console](https://console.firebase.google.com/).
2. Add an Android app to it with package name `com.kinpulse.app`.
3. Download the generated `google-services.json` and place it at `app/google-services.json`
   (this file is per-developer and intentionally not committed; the Gradle build only applies
   the Google Services plugin when it finds it — see `app/build.gradle.kts`).
4. In **Authentication → Sign-in method**, enable the **Email/Password** and **Google** providers.
   For Google sign-in, also add your signing key's SHA-1 under **Project settings → Your apps →
   SHA certificate fingerprints** (for the debug key: `./gradlew signingReport`), then download
   `google-services.json` again so it contains the OAuth client. The "Continue with Google" button
   only appears when that client is present; email/password keeps working without it.
5. In **Firestore Database**, create a database (any region; production or test mode, since the
   rules below lock it down either way).
6. Deploy the security rules and indexes from the repo root using the
   [Firebase CLI](https://firebase.google.com/docs/cli) (`npm install -g firebase-tools`,
   then `firebase login`):

   ```sh
   firebase deploy --only firestore --project <your-project-id>
   ```

   This publishes `firestore.rules` (access control) and `firestore.indexes.json` (the
   single-field index override the invite lookup needs).
7. Open the project in Android Studio (or run `./gradlew assembleDebug` / `./gradlew
   installDebug` from the command line) and run it on a device or emulator with API 26+.

## Continuous integration

`.github/workflows/android.yml` runs the unit tests and builds a debug APK on every push and pull
request, uploading it as the `kinpulse-debug-apk` artifact.

If you'd like CI's APK to be wired to your own Firebase project, add the contents of your
`google-services.json` as a repository secret named `GOOGLE_SERVICES_JSON`; the workflow writes it
to `app/google-services.json` before building. This is optional — CI builds and tests fine without
it, since the app degrades to a "Firebase not configured" screen when the file is missing.
Note that each CI run signs the APK with a fresh debug key, so Google sign-in fails on CI builds
(email/password still works); build locally, or with a fixed signing key, for Google sign-in.

## Data model

Firestore layout (mirrored in `data/HealthRepository.kt` and enforced by `firestore.rules`):

```
profiles/{profileId}
    name, ownerId, memberIds[], roles{uid: "OWNER"|"EDITOR"|"VIEWER"}, memberNames{uid: name}, createdAt

profiles/{profileId}/readings/{readingId}
    one sugar or BP measurement: type, takenAt, sugarMgDl?, sugarContext?, systolic?, diastolic?,
    pulse?, note, addedBy, addedByName

profiles/{profileId}/invites/{emailLowercase}
    a pending invite: email, role, profileId, profileName, invitedBy, invitedByName, createdAt
```

The owner of a profile creates it and invites family members by email with a role. An invitee
signs up (or signs in) with that email, verifies it, and sees the invite waiting on their home
screen; accepting it adds them to the profile's `memberIds`/`roles`/`memberNames` and removes the
invite, atomically. The owner can change roles, remove members, cancel invites, and rename or
delete the profile; any other member can leave on their own. Owners and editors can add, edit and
delete readings; viewers can only read them.

## Privacy

Your health data lives in your own Firebase project — KinPulse doesn't run a backend of its own.
`firestore.rules` restricts every read and write to the members of a profile (and invites to the
person they're addressed to), so nobody else can see it.

KinPulse is not a medical device. The colour-coded ranges are a general guide based on ADA
(glucose) and ACC/AHA 2017 (blood pressure) adult guidelines — they are not a diagnosis, and a
doctor may set different targets for a specific person.

## Roadmap ideas

- Reminders/notifications to log a reading.
- A preference to display sugar readings in mmol/L instead of mg/dL.
- PDF report export, alongside the existing CSV export.
- Importing readings from Health Connect.
