package org.openhab.matter.companion.domain;

public final class OpenHabInstructions {
    private OpenHabInstructions() {
    }

    public static String scanInputInstructions(String temporaryCode) {
        return "Open openHAB Main UI > Settings > Things > + > Matter > Scan Input, then enter the temporary setup code: "
                + temporaryCode;
    }

    public static String scanInputInstructionsWithoutEcho() {
        return "Open openHAB Main UI > Settings > Things > + > Matter > Scan Input, then enter the setup or multi-admin code from your device or other ecosystem. The app does not repeat that code here to avoid exposing setup credentials.";
    }

    public static String troubleshooting() {
        return "If the device does not appear in openHAB Inbox, verify the Matter binding controller is online, IPv6 routing works through the OTBR, and mDNS/Avahi is working between openHAB and the Thread network.";
    }
}
