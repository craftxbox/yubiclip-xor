import sys
import getpass
import base64
import secrets
import time
from yubikit.core.otp import OtpConnection
from yubikit.yubiotp import (
    YubiOtpSession,
    SLOT,
    NDEF_TYPE,
    StaticPasswordSlotConfiguration,
)
from ykman.device import connect_to_device, scan_devices, list_all_devices

slot = None
xorKeyGenerated = False
didProvision = False

if(len(sys.argv) == 1):
    print("Usage: py xorsetup.py <slot 1|2> [XOR key]")
    print("If no XOR key is provided, a cryptographically secure one will be generated")
    sys.exit(1)
if(len(sys.argv) > 2):
    xorKey = sys.argv[2]
else:
    xorKey = secrets.token_hex(30)
    xorKeyGenerated = True
    print("Generated XOR key: " + xorKey)
    print("Note this in a secure place, if you lose it you will not be able to recover your password!")

def xorBytes(data,key):
    result = bytearray()
    for i in range(min(len(data),30)):
        result.append(data[i] ^ key[i])
    return result

def waitForDevice():
    state = None
    got_Device = False

    print("Waiting for YubiKey insertion...")

    while True:
        pids, new_state = scan_devices()
        if new_state != state:
            state = new_state
            for device, info in list_all_devices():
                got_Device = True

            if got_Device:
                break
        else:
            time.sleep(1.0)  # No change, sleep for 1 second.

waitForDevice()

connection, device, info = connect_to_device(
    connection_types=[OtpConnection], 
)

slot = SLOT.ONE if sys.argv[1] == "1" else SLOT.TWO if sys.argv[1] == "2" else None

print("YubiKey inserted!")

#ask if they need to provision
if xorKeyGenerated == False:
    print("You have provided an XOR key, do you want to use automatic key provisioning? (y/n)")
    if(input() == "y"):
        with connection:
            session = YubiOtpSession(connection)
            if slot is None:
                raise ValueError("Slot must be 1 or 2")
            if slot is SLOT.ONE:
                print("!!!!! CONFIRM !!!!!")
                print("This will overwrite the SLOT 1")
                print("Slot 1 stores FACTORY PROGRAMMED (cc prefix) YubiOTP credentials (ex, cccccbcbrigfcdnudidhjcvhbkdulvvibgedbjdbljvl)")
                print("If you have used YubiOTP on any accounts (ie, LastPass), you may lose access to those accounts!")
                print("Some services may not accept vv prefix YubiOTP credentials. You CANNOT get the cc prefix back later!")
                print("Do you wish to continue? y/n")
                answer = input()
                if answer != "y":
                    print("ABORT")
                    sys.exit()

            print("!!!!! CONFIRM !!!!!")
            print(f"This will overwrite the SLOT {sys.argv[1]}")
            print("Do you wish to continue? y/n")
            answer = input()
            if answer == "y":
                print("Continuing...")
            else:
                print("ABORT")
                sys.exit()

            session.put_configuration(slot, StaticPasswordSlotConfiguration(base64.a85encode(bytes.fromhex(xorKey))))
            session.set_ndef_configuration(slot,"x-yubixor-provision://#", None, NDEF_TYPE.URI)
            print("Loaded provision key. Remove your YubiKey and scan it with the Yubiclip-XOR app.")

            didProvision = True
        print("Continue setup? y/n")
        answer = input()
        if answer != "y":
            sys.exit()

        waitForDevice()

        connection, device, info = connect_to_device(
            connection_types=[OtpConnection], 
        )

with connection:
    session = YubiOtpSession(connection)

    if slot is SLOT.ONE and didProvision is False:
        print("!!!!! CONFIRM !!!!!")
        print("This will overwrite the SLOT 1")
        print("Slot 1 stores FACTORY PROGRAMMED (cc prefix) YubiOTP credentials (ex, cccccbcbrigfcdnudidhjcvhbkdulvvibgedbjdbljvl)")
        print("If you have used YubiOTP on any accounts (ie, LastPass), you may lose access to those accounts!")
        print("Some services may not accept vv prefix YubiOTP credentials. You CANNOT get the cc prefix back later!")
        print("Do you wish to continue? y/n")
        answer = input()
        if answer != "y":
            print("ABORT")
            sys.exit()

    while True:
        password = getpass.getpass("Enter password (max 30 bytes, min 8): ")
        passconf = getpass.getpass("Confirm password: ")
        if password != passconf:
            print("Passwords do not match, Try again.")
        elif len(password.encode("ascii")) > 30:
            print("Password too long, Try again.")
        elif(len(password.encode("ascii")) < 8): # this is arbitrary, if we really wanted to we could store a single byte, but I cant think of a use case for less than 8.
            print("Password too short (min 8 bytes), Try again.")
        else:
            break

    print("!!!!! CONFIRM !!!!!")
    print(f"This will overwrite the SLOT {sys.argv[1]}")
    print("Do you wish to continue? y/n")
    answer = input()
    if answer == "y":
        print("Continuing...")
    else:
        print("ABORT")
        sys.exit()

    session.put_configuration(slot, StaticPasswordSlotConfiguration(base64.a85encode(xorBytes(password.encode("ascii"),bytes.fromhex(xorKey)))))
    session.set_ndef_configuration(slot,"x-yubixor://#", None, NDEF_TYPE.URI)
    print("Setup complete. Remove your YubiKey and scan it with the Yubiclip-XOR app.")
