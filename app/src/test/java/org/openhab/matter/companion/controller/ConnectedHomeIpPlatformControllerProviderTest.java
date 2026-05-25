package org.openhab.matter.companion.controller;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

public final class ConnectedHomeIpPlatformControllerProviderTest {
    @Test
    public void constructsPlatformThenControllerWithExpectedParams() throws Exception {
        resetFakes();
        Object context = "android-context";
        ConnectedHomeIpPlatformControllerProvider provider = fakeProvider(context);

        Object controller = provider.controller();

        assertSame(FakeChipDeviceController.class, controller.getClass());
        assertEquals(1, FakeChipDeviceController.loadJniCalls);
        assertSame(context, FakeAndroidBleManager.context);
        assertSame(context, FakePreferencesKeyValueStoreManager.context);
        assertSame(context, FakePreferencesConfigurationManager.context);
        assertSame(context, FakeNsdManagerServiceResolver.context);
        assertSame(context, FakeNsdManagerServiceBrowser.context);
        assertSame(context, FakeDiagnosticDataProviderImpl.context);
        assertSame(FakeAndroidBleManager.class, FakeAndroidChipPlatform.ble.getClass());
        assertSame(FakeAndroidNfcCommissioningManager.class, FakeAndroidChipPlatform.nfc.getClass());
        assertSame(FakePreferencesKeyValueStoreManager.class, FakeAndroidChipPlatform.kvm.getClass());
        assertSame(FakePreferencesConfigurationManager.class, FakeAndroidChipPlatform.cfg.getClass());
        assertSame(FakeNsdManagerServiceResolver.class, FakeAndroidChipPlatform.resolver.getClass());
        assertSame(FakeNsdManagerServiceBrowser.class, FakeAndroidChipPlatform.browser.getClass());
        assertSame(FakeChipMdnsCallbackImpl.class, FakeAndroidChipPlatform.chipMdnsCallback.getClass());
        assertSame(FakeDiagnosticDataProviderImpl.class, FakeAndroidChipPlatform.dataProvider.getClass());
        assertEquals(0xFFF4, FakeControllerParams.lastBuilder.controllerVendorId);
        assertEquals(true, FakeControllerParams.lastBuilder.enableServerInteractions);
        assertSame(FakeControllerParams.BUILT, FakeChipDeviceController.params);
    }

    @Test
    public void cachesControllerInstance() throws Exception {
        resetFakes();
        ConnectedHomeIpPlatformControllerProvider provider = fakeProvider("android-context");

        Object first = provider.controller();
        Object second = provider.controller();

        assertSame(first, second);
        assertEquals(1, FakeChipDeviceController.loadJniCalls);
        assertEquals(1, FakeChipDeviceController.constructorCalls);
    }

    @Test
    public void controllerConstructionFailureIsTerminalForProviderInstance() {
        resetFakes();
        FakeChipDeviceController.failConstruction = true;
        ConnectedHomeIpPlatformControllerProvider provider = fakeProvider("android-context");

        Exception first = assertThrows(Exception.class, provider::controller);
        Exception second = assertThrows(Exception.class, provider::controller);
        Exception platformFailure = assertThrows(Exception.class, provider::platform);

        assertSame(first, second);
        assertSame(first, platformFailure);
        assertEquals(1, FakeChipDeviceController.loadJniCalls);
        assertEquals(1, FakeChipDeviceController.constructorCalls);
        assertEquals(1, FakeAndroidChipPlatform.constructorCalls);
    }

    private static ConnectedHomeIpPlatformControllerProvider fakeProvider(Object context) {
        return new ConnectedHomeIpPlatformControllerProvider(
                context,
                FakeChipDeviceController.class,
                FakeControllerParams.class,
                FakeAndroidChipPlatform.class,
                FakeAndroidBleManager.class,
                FakeAndroidNfcCommissioningManager.class,
                FakePreferencesKeyValueStoreManager.class,
                FakePreferencesConfigurationManager.class,
                FakeNsdManagerServiceResolver.class,
                FakeNsdManagerServiceBrowser.class,
                FakeChipMdnsCallbackImpl.class,
                FakeDiagnosticDataProviderImpl.class);
    }

    private static void resetFakes() {
        FakeChipDeviceController.loadJniCalls = 0;
        FakeChipDeviceController.constructorCalls = 0;
        FakeChipDeviceController.params = null;
        FakeControllerParams.lastBuilder = null;
        FakeAndroidBleManager.context = null;
        FakePreferencesKeyValueStoreManager.context = null;
        FakePreferencesConfigurationManager.context = null;
        FakeNsdManagerServiceResolver.context = null;
        FakeNsdManagerServiceBrowser.context = null;
        FakeDiagnosticDataProviderImpl.context = null;
        FakeAndroidChipPlatform.ble = null;
        FakeAndroidChipPlatform.nfc = null;
        FakeAndroidChipPlatform.kvm = null;
        FakeAndroidChipPlatform.cfg = null;
        FakeAndroidChipPlatform.resolver = null;
        FakeAndroidChipPlatform.browser = null;
        FakeAndroidChipPlatform.chipMdnsCallback = null;
        FakeAndroidChipPlatform.dataProvider = null;
        FakeAndroidChipPlatform.constructorCalls = 0;
        FakeChipDeviceController.failConstruction = false;
    }

    public interface FakeBleManager {
    }

    public interface FakeNfcCommissioningManager {
    }

    public interface FakeKeyValueStoreManager {
    }

    public interface FakeConfigurationManager {
    }

    public interface FakeServiceResolver {
    }

    public interface FakeServiceBrowser {
    }

    public interface FakeChipMdnsCallback {
    }

    public interface FakeDiagnosticDataProvider {
    }

    public static final class FakeAndroidBleManager implements FakeBleManager {
        private static Object context;

        public FakeAndroidBleManager(Object context) {
            FakeAndroidBleManager.context = context;
        }
    }

    public static final class FakeAndroidNfcCommissioningManager implements FakeNfcCommissioningManager {
    }

    public static final class FakePreferencesKeyValueStoreManager implements FakeKeyValueStoreManager {
        private static Object context;

        public FakePreferencesKeyValueStoreManager(Object context) {
            FakePreferencesKeyValueStoreManager.context = context;
        }
    }

    public static final class FakePreferencesConfigurationManager implements FakeConfigurationManager {
        private static Object context;

        public FakePreferencesConfigurationManager(Object context) {
            FakePreferencesConfigurationManager.context = context;
        }
    }

    public static final class FakeNsdManagerServiceResolver implements FakeServiceResolver {
        private static Object context;

        public FakeNsdManagerServiceResolver(Object context) {
            FakeNsdManagerServiceResolver.context = context;
        }
    }

    public static final class FakeNsdManagerServiceBrowser implements FakeServiceBrowser {
        private static Object context;

        public FakeNsdManagerServiceBrowser(Object context) {
            FakeNsdManagerServiceBrowser.context = context;
        }
    }

    public static final class FakeChipMdnsCallbackImpl implements FakeChipMdnsCallback {
    }

    public static final class FakeDiagnosticDataProviderImpl implements FakeDiagnosticDataProvider {
        private static Object context;

        public FakeDiagnosticDataProviderImpl(Object context) {
            FakeDiagnosticDataProviderImpl.context = context;
        }
    }

    public static final class FakeAndroidChipPlatform {
        private static Object ble;
        private static Object nfc;
        private static Object kvm;
        private static Object cfg;
        private static Object resolver;
        private static Object browser;
        private static Object chipMdnsCallback;
        private static Object dataProvider;
        private static int constructorCalls;

        public FakeAndroidChipPlatform(
                FakeBleManager ble,
                FakeNfcCommissioningManager nfc,
                FakeKeyValueStoreManager kvm,
                FakeConfigurationManager cfg,
                FakeServiceResolver resolver,
                FakeServiceBrowser browser,
                FakeChipMdnsCallback chipMdnsCallback,
                FakeDiagnosticDataProvider dataProvider) {
            constructorCalls++;
            FakeAndroidChipPlatform.ble = ble;
            FakeAndroidChipPlatform.nfc = nfc;
            FakeAndroidChipPlatform.kvm = kvm;
            FakeAndroidChipPlatform.cfg = cfg;
            FakeAndroidChipPlatform.resolver = resolver;
            FakeAndroidChipPlatform.browser = browser;
            FakeAndroidChipPlatform.chipMdnsCallback = chipMdnsCallback;
            FakeAndroidChipPlatform.dataProvider = dataProvider;
        }
    }

    public static final class FakeControllerParams {
        private static final FakeControllerParams BUILT = new FakeControllerParams();
        private static Builder lastBuilder;

        public static Builder newBuilder() {
            lastBuilder = new Builder();
            return lastBuilder;
        }

        public static final class Builder {
            private int controllerVendorId;
            private boolean enableServerInteractions;

            public Builder setControllerVendorId(int controllerVendorId) {
                this.controllerVendorId = controllerVendorId;
                return this;
            }

            public Builder setEnableServerInteractions(boolean enableServerInteractions) {
                this.enableServerInteractions = enableServerInteractions;
                return this;
            }

            public FakeControllerParams build() {
                return BUILT;
            }
        }
    }

    public static final class FakeChipDeviceController {
        private static int loadJniCalls;
        private static int constructorCalls;
        private static Object params;
        private static boolean failConstruction;

        public static void loadJni() {
            loadJniCalls++;
        }

        public FakeChipDeviceController(FakeControllerParams params) {
            constructorCalls++;
            if (failConstruction) {
                throw new IllegalStateException("controller construction failed");
            }
            FakeChipDeviceController.params = params;
        }
    }
}
