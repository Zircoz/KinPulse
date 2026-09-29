# Firestore documents are mapped by hand (see data/Mappers.kt), so no model keep rules are needed.

# Credential Manager loads its Play Services provider reflectively.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}
