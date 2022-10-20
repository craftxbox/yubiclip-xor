/*
 * Copyright (c) 2013 Yubico AB
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.yubico.yubiclip;

import android.app.*;
import android.content.*;
import android.text.Html;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.widget.Toast;
import com.github.fzakaria.ascii85.Ascii85;
import com.yubico.yubiclip.scancode.KeyboardLayout;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HandleOTPActivity extends Activity {
    private static final String URL_PREFIX = "https://my.yubico.com/";
    private static final byte URL_NDEF_RECORD = (byte) 0xd1;
    private static final byte[] URL_PREFIX_BYTES = new byte[URL_PREFIX.length() + 2 - 8];

    private static final Pattern OTP_PATTERN = Pattern.compile("^https://my\\.yubico\\.com/[a-z]+/#?([a-zA-Z0-9!]+)$");
    private static final Pattern XOR_PATTERN = Pattern.compile("^x-yubixor://#(.*)$");
    private static final Pattern PROVISON_PATTERN = Pattern.compile("^x-yubixor-provision://#(.*)$");

    private SharedPreferences prefs;

    static {
        URL_PREFIX_BYTES[0] = 85;
        URL_PREFIX_BYTES[1] = 4;
        System.arraycopy(URL_PREFIX.substring(8).getBytes(), 0, URL_PREFIX_BYTES, 2, URL_PREFIX_BYTES.length - 2);
    }

    @Override
    public void onResume() {
        super.onResume();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        String ids = getIntent().getDataString();

        Matcher otpMatcher = OTP_PATTERN.matcher(ids);
        Matcher provisionMatcher = PROVISON_PATTERN.matcher(ids);
        Matcher xorMatcher = XOR_PATTERN.matcher(ids);
        if (otpMatcher.matches()) {
            handleOTP(otpMatcher.group(1));
        } else if (provisionMatcher.matches()) {
            provisionXOR(provisionMatcher.group(1));
        } else if (xorMatcher.matches()) {
            handleXOR(xorMatcher.group(1));
        } else {
            Parcelable[] raw = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            byte[] bytes = ((NdefMessage) raw[0]).toByteArray();
            if (bytes[0] == URL_NDEF_RECORD
                    && Arrays.equals(URL_PREFIX_BYTES, Arrays.copyOfRange(bytes, 3, 3 + URL_PREFIX_BYTES.length))) {
                if (Arrays.equals("/neo/".getBytes(), Arrays.copyOfRange(bytes, 18, 18 + 5))) {
                    bytes[22] = '#';
                }
                for (int i = 0; i < bytes.length; i++) {
                    if (bytes[i] == '#') {
                        bytes = Arrays.copyOfRange(bytes, i + 1, bytes.length);
                        String layout = prefs.getString(getString(R.string.pref_layout), "US");
                        KeyboardLayout kbd = KeyboardLayout.forName(layout);
                        handleOTP(kbd.fromScanCodes(bytes));
                        finish();
                        break;
                    }
                }
            }
        }
    }

    private void provisionXOR(String data) {
        byte[] byteData = Ascii85.decode(data.replaceAll("\\x00+$", "")); // The key will produce 38 bytes of data nomatter what, so trim null bytes from the end so we dont end up with garbage
        String xorKey = bytesToHex(byteData);
        if (xorKey.equals(prefs.getString(getString(R.string.pref_xor_key), "aaaa"))) {
            Toast.makeText(getApplication(), R.string.xor_key_already_provisioned, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.xor_key_provision);
        alert.setMessage(Html.fromHtml(getString(R.string.xor_key_provision_summary)+ xorKey));
        alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                prefs.edit().putString(getString(R.string.pref_xor_key), xorKey).commit();
                dialog.dismiss();
                finish();
            }
        });

        alert.setNegativeButton(R.string.safe_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });

        alert.show();
    }

    private void handleOTP(String data) {
        if (prefs.getBoolean(getString(R.string.pref_clipboard), true)) {
            copyToClipboard(data);
        }
        if (prefs.getBoolean(getString(R.string.pref_notification), false)) {
            displayNotification(data);
        }
        finish();
    }

    private void handleXOR(String data) {
        if (prefs.getBoolean(getString(R.string.pref_xor), true)) {
            String xorKeyString = prefs.getString(getString(R.string.pref_xor_key), "aaaa");
            byte[] xorKey = new byte[xorKeyString.length() / 2];
            for (int i = 0; i < xorKeyString.length()/2; i++) {
                int stringIndex = i * 2;
                xorKey[i] = (byte) Integer.parseInt(xorKeyString.substring(stringIndex, stringIndex + 2), 16);
            }
            byte[] byteData = Ascii85.decode(data.replaceAll("\\x00+$", "")); // The key will produce 38 bytes of data nomatter what, so trim null bytes from the end so we dont end up with garbage
            byte[] decrypted = xorBytes(byteData, xorKey);
            if(decrypted == null){
                return;
            }
            data = new String(decrypted);

        }
        if (prefs.getBoolean(getString(R.string.pref_clipboard), true)) {
            copyToClipboard(data);
        }
        if (prefs.getBoolean(getString(R.string.pref_notification), false)) {
            displayNotification(data);
        }
        finish();
    }

    private byte[] xorBytes(byte[] data, byte[] key) {
        if (key.length < 30) {
            Toast.makeText(getApplication(), R.string.xor_key_too_short, Toast.LENGTH_SHORT).show();
            finish();
            return null;
        }
        // No check for if its longer because if its longer than needed we don't have to
        // do anything special

        byte[] decrypted = new byte[data.length];

        for (int i = 0; i < Math.min(data.length, 30); i++) {
            decrypted[i] = (byte) (data[i] ^ key[i]);
        }
        return decrypted;
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private void copyToClipboard(String data) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(ClearClipboardService.YUBI_CLIP_DATA, data);
        clipboard.setPrimaryClip(clip);
        int timeout = Integer.parseInt(prefs.getString(getString(R.string.pref_timeout), "-1"));
        if (timeout > 0) {
            startService(new Intent(this, ClearClipboardService.class));
        }
        Toast.makeText(getApplication(), R.string.copied, Toast.LENGTH_SHORT).show();
    }

    private void displayNotification(String data) {
        Notification.Builder nBuilder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_yubiclip)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(data);
        int nId = 0;
        NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nManager.notify(nId, nBuilder.getNotification());
    }
}