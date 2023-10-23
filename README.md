# YubiClip XOR [![Java CI with Gradle](https://github.com/craftxbox/yubiclip-xor/actions/workflows/gradle.yml/badge.svg)](https://github.com/craftxbox/yubiclip-xor/actions/workflows/gradle.yml)

A fork of [YubiClip](https://github.com/Yubico/yubiclip-android)  
Present static passwords over NFC securely from your NFC enabled Yubikey.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/com.craftxbox.yubiclip.xor/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=com.craftxbox.yubiclip.xor)

Or download the latest APK from the [Releases Section](https://github.com/craftxbox/yubiclip-xor/releases/latest).

## First-time XOR Setup & Password changes

If you're just using the app for OTP codes, or do not wish to enable encryption, Disable the "XOR Decryption" option in the settings.
You can safely ignore this section.

**If you wish to setup XOR protection, keep reading**
You will need to be at a computer with [Python 3 and Pip](https://www.python.org/downloads/) installed.  
Setup for XOR cannot be performed from the app itself.

Download [the latest release](https://github.com/craftxbox/yubiclip-xor/zipball/master) and extract it.
Open a terminal or command prompt in the extracted folder and run the following commands:

```
  pip install -r requirements.txt
  py xorsetup.py <slot: 1|2> [optional XOR Key]
```

Follow the prompts given in the terminal.
We recommend provisioning slot 2, as using slot 1 will destroy the factory Yubico OTP keys.

### Example setup process:

```
  PS D:\yubiclip-xor\py> python .\xorsetup.py 2
  Generated XOR key: 1a2b3c4d5e6f1a2b3c4d5e6f1a2b3c4d5e6f1a2b3c4d5e6f1a2b3c4d5e6f
  Note this in a secure place, if you lose it you will not be able to recover your password!
  Waiting for YubiKey insertion...
  YubiKey inserted!
  !!!!! CONFIRM !!!!!
  This will overwrite the SLOT 2
  Do you wish to continue? y/n
  y
  Continuing...
  Loaded provision key. Remove your YubiKey and scan it with the Yubiclip-XOR app before continuing.
  Continue setup? y/n
  y
  Waiting for YubiKey insertion...
  Enter password (max 30 bytes, min 8): ********
  Confirm password: ********
  !!!!! CONFIRM !!!!!
  This will overwrite the SLOT 2
  Do you wish to continue? y/n
  y
  Continuing...
  Setup complete. Remove your YubiKey and scan it with the Yubiclip-XOR app.
```

**IMPORTANT:** If you change your password, it is HIGHLY DISCOURAGED to use the same XOR key as before.  
If you use the same key twice, you will **_ruin_** the security of the XOR encryption!  
Simply: Do not use the second argument to `xorsetup.py` unless you _really_ know what you are doing.

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
