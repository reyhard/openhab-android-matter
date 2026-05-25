package org.openhab.matter.companion.controller;

public final class ConnectedHomeIpControllerArtifacts {
    private static final String LIBRARY_NAME = "CHIPController";
    private static final String[] REQUIRED_CLASS_NAMES = {
            "chip.devicecontroller.ChipDeviceController",
            "chip.devicecontroller.ControllerParams",
            "chip.devicecontroller.NetworkCredentials",
            "chip.devicecontroller.NetworkCredentials$ThreadCredentials",
            "chip.devicecontroller.CommissionParameters",
            "chip.devicecontroller.CommissionParameters$Builder",
            "chip.devicecontroller.ChipDeviceController$CompletionListener",
            "chip.devicecontroller.DeviceAttestationDelegate",
            "chip.devicecontroller.OpenCommissioningCallback",
            "chip.devicecontroller.GetConnectedDeviceCallbackJni$GetConnectedDeviceCallback",
            "chip.platform.AndroidChipPlatform",
            "chip.platform.AndroidBleManager",
            "chip.platform.AndroidNfcCommissioningManager",
            "chip.platform.PreferencesKeyValueStoreManager",
            "chip.platform.PreferencesConfigurationManager",
            "chip.platform.NsdManagerServiceResolver",
            "chip.platform.NsdManagerServiceBrowser",
            "chip.platform.ChipMdnsCallbackImpl",
            "chip.platform.DiagnosticDataProviderImpl",
            "chip.platform.BleCallback"
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
            try {
                if (classLookup.exists(className)) {
                    continue;
                }
                return new ConnectedHomeIpControllerArtifactsStatus(
                        false,
                        LIBRARY_NAME,
                        "Missing connectedhomeip controller class: " + className);
            } catch (LinkageError | SecurityException ex) {
                return new ConnectedHomeIpControllerArtifactsStatus(
                        false,
                        LIBRARY_NAME,
                        "Failed to inspect connectedhomeip controller class "
                                + className + ": " + safeMessage(ex));
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
            Class.forName(className, false, ConnectedHomeIpControllerArtifacts.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        } catch (LinkageError | SecurityException ex) {
            throw ex;
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
