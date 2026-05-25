package org.openhab.matter.companion.controller;

public final class ConnectedHomeIpControllerArtifacts {
    private static final String LIBRARY_NAME = "CHIPController";
    private static final String[] REQUIRED_CLASS_NAMES = {
            "chip.devicecontroller.ChipDeviceController",
            "chip.devicecontroller.ControllerParams",
            "chip.devicecontroller.NetworkCredentials",
            "chip.devicecontroller.CommissionParameters",
            "chip.devicecontroller.OpenCommissioningCallback",
            "chip.platform.AndroidChipPlatform",
            "chip.platform.AndroidBleManager",
            "chip.platform.NsdManagerServiceResolver"
    };

    private final ClassLookup classLookup;
    private final NativeLibraryLoader nativeLibraryLoader;

    public ConnectedHomeIpControllerArtifacts() {
        this(ConnectedHomeIpControllerArtifacts::classExists, new SystemNativeLibraryLoader());
    }

    public ConnectedHomeIpControllerArtifacts(ClassLookup classLookup, NativeLibraryLoader nativeLibraryLoader) {
        this.classLookup = classLookup == null ? ConnectedHomeIpControllerArtifacts::classExists : classLookup;
        this.nativeLibraryLoader = nativeLibraryLoader == null ? new SystemNativeLibraryLoader() : nativeLibraryLoader;
    }

    public ConnectedHomeIpControllerArtifactsStatus check() {
        for (String className : REQUIRED_CLASS_NAMES) {
            if (!classLookup.exists(className)) {
                return new ConnectedHomeIpControllerArtifactsStatus(
                        false,
                        LIBRARY_NAME,
                        "Missing connectedhomeip controller class: " + className);
            }
        }

        try {
            nativeLibraryLoader.load(LIBRARY_NAME);
        } catch (UnsatisfiedLinkError | SecurityException ex) {
            return new ConnectedHomeIpControllerArtifactsStatus(
                    false,
                    LIBRARY_NAME,
                    "Failed to load lib" + LIBRARY_NAME + ": " + safeMessage(ex));
        }

        return new ConnectedHomeIpControllerArtifactsStatus(
                true,
                LIBRARY_NAME,
                "connectedhomeip Android controller artifacts are ready");
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isEmpty() ? throwable.getClass().getSimpleName() : message;
    }

    public interface ClassLookup {
        boolean exists(String className);
    }
}
