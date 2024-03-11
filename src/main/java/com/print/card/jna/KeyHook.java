package com.print.card.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface KeyHook extends Library {
    KeyHook instance = Native.load("C:\\print\\bin\\KeyboardHook.dll", KeyHook.class);

    int installHook();
    void unHook();
    String getCard();

}
