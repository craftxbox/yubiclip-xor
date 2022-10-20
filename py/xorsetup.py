import sys
import getpass
import base64
import time
from yubikit.core.otp import OtpConnection
from yubikit.yubiotp import (
    YubiOtpSession,
    SLOT,
    NDEF_TYPE,
    StaticPasswordSlotConfiguration,
)
from ykman.device import connect_to_device, scan_devices, list_all_devices

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

print("YubiKey inserted!")

with connection:
    session = YubiOtpSession(connection)
    slot = SLOT.ONE if sys.argv[1] == "1" else SLOT.TWO if sys.argv[1] == "2" else None
    if slot is None:
        raise ValueError("Slot must be 1 or 2")

    print("!!!!! CONFIRM !!!!!")
    print(f"This will overwrite the SLOT {sys.argv[1]}")
    print("Do you wish to continue? y/n")
    answer = input()
    if answer == "y":
        print("Continuing...")
    else:
        print("ABORT")
        sys.exit()

    session.put_configuration(slot, StaticPasswordSlotConfiguration(base64.a85encode(bytes.fromhex(sys.argv[2]))))
    session.set_ndef_configuration(slot,"x-yubixor-provision://#", None, NDEF_TYPE.URI)
    print("Loaded provision key. Remove your YubiKey and scan it with the Yubiclip-XOR app.")

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
    slot = SLOT.ONE if sys.argv[1] == "1" else SLOT.TWO if sys.argv[1] == "2" else None
    if slot is None:
        raise ValueError("Slot must be 1 or 2")

    while True:
        password = getpass.getpass("Enter password (max 30 bytes): ")
        passconf = getpass.getpass("Confirm password: ")
        if password != passconf:
            print("Passwords do not match, Try again.")
        elif len(password.encode("ascii")) > 30:
            print("Password too long, Try again.")
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

    session.put_configuration(slot, StaticPasswordSlotConfiguration(base64.a85encode(xorBytes(password.encode("ascii"),bytes.fromhex(sys.argv[2])))))
    session.set_ndef_configuration(slot,"x-yubixor://#", None, NDEF_TYPE.URI)
    print("Setup complete. Remove your YubiKey and scan it with the Yubiclip-XOR app.")
