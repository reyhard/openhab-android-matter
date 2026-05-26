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

    public static String troubleshootingGuide() {
        return "Troubleshooting guide\n"
                + "1. openHAB: install the Matter binding, keep the Matter binding controller Thing online, and configure the REST API token in this app when openHAB requires authentication.\n"
                + "2. OTBR: enter the OTBR IPv4/IPv6 address or host name. A pure OpenThread Border Router does not need to host HTTP; optional HTTP URLs are only for diagnostic services.\n"
                + "3. IPv6: confirm the phone, OTBR, and openHAB host can route IPv6 to the Thread network. Router advertisements and route information must reach the openHAB host.\n"
                + "4. mDNS/Avahi: ensure openHAB can discover Matter services across the LAN and Thread border router path; check Avahi or equivalent mDNS reflection when subnets differ.\n"
                + "5. connectedhomeip: real BLE Thread commissioning requires a build that packages CHIPTool-style connectedhomeip artifacts. Missing ChipDeviceController or ChipDeviceControllerNative means rebuild with -ChipControllerArtifactsDir containing CHIPController.jar and jniLibs/<abi>/libCHIPController.so.\n"
                + "6. Pairing flow: after OpenCommissioningWindow, enter the temporary code in openHAB Main UI > Settings > Things > + > Matter > Scan Input, then watch the openHAB Inbox from this app.";
    }
}
