# Preview APK signing

`preview.keystore` is an intentionally public development key used only for
installable preview APKs. Keeping this key stable lets a tester update the app
without uninstalling it after every CI build.

Production bundles must use Google Play App Signing and must never reuse this
keystore or its credentials.
