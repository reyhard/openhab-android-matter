package org.openhab.matter.companion.controller;

public final class SystemNativeLibraryLoader implements NativeLibraryLoader {
    @Override
    public void load(String libraryName) {
        System.loadLibrary(libraryName);
    }
}
