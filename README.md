# YubiClip XOR
A fork of [YubiClip](https://github.com/Yubico/yubiclip-android)
Present static passwords over NFC securely from your NFC enabled Yubikey.

## First-time Setup
Run the [`xorsetup.py`](./py) utility
```
  py xorsetup.py <slot: 1|2> <Key in hex format>
```

## Build from source
Building can be done through `gradlew` as follows:
```
./gradlew assemble
```
Resulting APKs can be found in app/build/outputs/apk or you can install the Debug APK via ADB with:
```
./gradlew installDebug
```
Note: To install Release APKs they need to be signed first!
Debug APKs do not need to be signed (as they are signed with the debug key)
