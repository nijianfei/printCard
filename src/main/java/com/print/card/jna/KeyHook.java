package com.print.card.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface KeyHook extends Library {
    KeyHook instance = Native.loadLibrary("KeyboardHook", KeyHook.class);

    void setOutTime(Integer outTime);
    int installHook();
    void unHook();
    String getCard();
}
