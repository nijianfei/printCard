package com.print.card.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.WString;

public interface DllLoadIn extends Library {
    DllLoadIn instance = Native.load("C:\\print\\bin\\MaticaSDKLiteRight.dll", DllLoadIn.class);

    WString callFunc(WString xmlCommand);

    WString setResource(WString xmlCommand, byte[] data, int dataLength);
}
