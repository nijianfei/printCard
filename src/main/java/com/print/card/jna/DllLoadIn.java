package com.print.card.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.WString;

public interface DllLoadIn extends Library {
    DllLoadIn instance = Native.loadLibrary("MaticaSDKLiteRight", DllLoadIn.class);

    WString callFunc(WString xmlCommand);

    WString setResource(WString xmlCommand, byte[] data, int dataLength);
}
