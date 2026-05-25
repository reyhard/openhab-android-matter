package org.openhab.matter.companion.controller;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class ConnectedHomeIpPlatformControllerProvider implements ConnectedHomeIpControllerProvider {
    private static final int TEST_VENDOR_ID = 0xFFF4;

    private static final String CHIP_DEVICE_CONTROLLER_CLASS = "chip.devicecontroller.ChipDeviceController";
    private static final String CONTROLLER_PARAMS_CLASS = "chip.devicecontroller.ControllerParams";
    private static final String ANDROID_CHIP_PLATFORM_CLASS = "chip.platform.AndroidChipPlatform";
    private static final String ANDROID_BLE_MANAGER_CLASS = "chip.platform.AndroidBleManager";
    private static final String ANDROID_NFC_COMMISSIONING_MANAGER_CLASS = "chip.platform.AndroidNfcCommissioningManager";
    private static final String PREFERENCES_KEY_VALUE_STORE_MANAGER_CLASS =
            "chip.platform.PreferencesKeyValueStoreManager";
    private static final String PREFERENCES_CONFIGURATION_MANAGER_CLASS =
            "chip.platform.PreferencesConfigurationManager";
    private static final String NSD_MANAGER_SERVICE_RESOLVER_CLASS = "chip.platform.NsdManagerServiceResolver";
    private static final String NSD_MANAGER_SERVICE_BROWSER_CLASS = "chip.platform.NsdManagerServiceBrowser";
    private static final String CHIP_MDNS_CALLBACK_IMPL_CLASS = "chip.platform.ChipMdnsCallbackImpl";
    private static final String DIAGNOSTIC_DATA_PROVIDER_IMPL_CLASS = "chip.platform.DiagnosticDataProviderImpl";

    private final Object context;
    private final Class<?> chipDeviceControllerClass;
    private final Class<?> controllerParamsClass;
    private final Class<?> androidChipPlatformClass;
    private final Class<?> androidBleManagerClass;
    private final Class<?> androidNfcCommissioningManagerClass;
    private final Class<?> preferencesKeyValueStoreManagerClass;
    private final Class<?> preferencesConfigurationManagerClass;
    private final Class<?> nsdManagerServiceResolverClass;
    private final Class<?> nsdManagerServiceBrowserClass;
    private final Class<?> chipMdnsCallbackImplClass;
    private final Class<?> diagnosticDataProviderImplClass;
    private Object platform;
    private Object controller;
    private Exception initializationFailure;

    public ConnectedHomeIpPlatformControllerProvider(Object context) throws ClassNotFoundException {
        this(context, ConnectedHomeIpPlatformControllerProvider.class.getClassLoader());
    }

    public ConnectedHomeIpPlatformControllerProvider(Object context, ClassLoader classLoader) throws ClassNotFoundException {
        this(
                context,
                Class.forName(CHIP_DEVICE_CONTROLLER_CLASS, false, classLoader),
                Class.forName(CONTROLLER_PARAMS_CLASS, false, classLoader),
                Class.forName(ANDROID_CHIP_PLATFORM_CLASS, false, classLoader),
                Class.forName(ANDROID_BLE_MANAGER_CLASS, false, classLoader),
                Class.forName(ANDROID_NFC_COMMISSIONING_MANAGER_CLASS, false, classLoader),
                Class.forName(PREFERENCES_KEY_VALUE_STORE_MANAGER_CLASS, false, classLoader),
                Class.forName(PREFERENCES_CONFIGURATION_MANAGER_CLASS, false, classLoader),
                Class.forName(NSD_MANAGER_SERVICE_RESOLVER_CLASS, false, classLoader),
                Class.forName(NSD_MANAGER_SERVICE_BROWSER_CLASS, false, classLoader),
                Class.forName(CHIP_MDNS_CALLBACK_IMPL_CLASS, false, classLoader),
                Class.forName(DIAGNOSTIC_DATA_PROVIDER_IMPL_CLASS, false, classLoader));
    }

    public ConnectedHomeIpPlatformControllerProvider(
            Object context,
            Class<?> chipDeviceControllerClass,
            Class<?> controllerParamsClass,
            Class<?> androidChipPlatformClass,
            Class<?> androidBleManagerClass,
            Class<?> androidNfcCommissioningManagerClass,
            Class<?> preferencesKeyValueStoreManagerClass,
            Class<?> preferencesConfigurationManagerClass,
            Class<?> nsdManagerServiceResolverClass,
            Class<?> nsdManagerServiceBrowserClass,
            Class<?> chipMdnsCallbackImplClass,
            Class<?> diagnosticDataProviderImplClass) {
        if (context == null) {
            throw new IllegalArgumentException("context is required");
        }
        this.context = context;
        this.chipDeviceControllerClass = requireClass(chipDeviceControllerClass, "chipDeviceControllerClass");
        this.controllerParamsClass = requireClass(controllerParamsClass, "controllerParamsClass");
        this.androidChipPlatformClass = requireClass(androidChipPlatformClass, "androidChipPlatformClass");
        this.androidBleManagerClass = requireClass(androidBleManagerClass, "androidBleManagerClass");
        this.androidNfcCommissioningManagerClass = requireClass(
                androidNfcCommissioningManagerClass,
                "androidNfcCommissioningManagerClass");
        this.preferencesKeyValueStoreManagerClass = requireClass(
                preferencesKeyValueStoreManagerClass,
                "preferencesKeyValueStoreManagerClass");
        this.preferencesConfigurationManagerClass = requireClass(
                preferencesConfigurationManagerClass,
                "preferencesConfigurationManagerClass");
        this.nsdManagerServiceResolverClass = requireClass(nsdManagerServiceResolverClass, "nsdManagerServiceResolverClass");
        this.nsdManagerServiceBrowserClass = requireClass(nsdManagerServiceBrowserClass, "nsdManagerServiceBrowserClass");
        this.chipMdnsCallbackImplClass = requireClass(chipMdnsCallbackImplClass, "chipMdnsCallbackImplClass");
        this.diagnosticDataProviderImplClass = requireClass(
                diagnosticDataProviderImplClass,
                "diagnosticDataProviderImplClass");
    }

    @Override
    public synchronized Object controller() throws Exception {
        if (initializationFailure != null) {
            throw initializationFailure;
        }
        if (controller == null) {
            try {
                chipDeviceControllerClass.getMethod("loadJni").invoke(null);
                Object initializedPlatform = newPlatform();
                Object initializedController = chipDeviceControllerClass
                        .getConstructor(controllerParamsClass)
                        .newInstance(newControllerParams());
                platform = initializedPlatform;
                controller = initializedController;
            } catch (Exception exception) {
                initializationFailure = exception;
                platform = null;
                controller = null;
                throw exception;
            }
        }
        return controller;
    }

    public synchronized Object platform() throws Exception {
        if (platform == null) {
            controller();
        }
        return platform;
    }

    private Object newPlatform() throws Exception {
        Object ble = newWithContext(androidBleManagerClass);
        Object nfc = newNoArg(androidNfcCommissioningManagerClass);
        Object keyValueStore = newWithContext(preferencesKeyValueStoreManagerClass);
        Object configuration = newWithContext(preferencesConfigurationManagerClass);
        Object resolver = newWithContext(nsdManagerServiceResolverClass);
        Object browser = newWithContext(nsdManagerServiceBrowserClass);
        Object mdnsCallback = newNoArg(chipMdnsCallbackImplClass);
        Object diagnosticProvider = newWithContext(diagnosticDataProviderImplClass);
        return findConstructor(
                androidChipPlatformClass,
                ble,
                nfc,
                keyValueStore,
                configuration,
                resolver,
                browser,
                mdnsCallback,
                diagnosticProvider)
                .newInstance(
                        ble,
                        nfc,
                        keyValueStore,
                        configuration,
                        resolver,
                        browser,
                        mdnsCallback,
                        diagnosticProvider);
    }

    private Object newControllerParams() throws Exception {
        Method newBuilder = controllerParamsClass.getMethod("newBuilder");
        Object builder = newBuilder.invoke(null);
        findSingleParameterMethod(builder.getClass(), "setControllerVendorId", int.class)
                .invoke(builder, TEST_VENDOR_ID);
        findSingleParameterMethod(builder.getClass(), "setEnableServerInteractions", boolean.class)
                .invoke(builder, true);
        return builder.getClass().getMethod("build").invoke(builder);
    }

    private Object newWithContext(Class<?> targetClass) throws Exception {
        return findConstructor(targetClass, context).newInstance(context);
    }

    private static Object newNoArg(Class<?> targetClass) throws Exception {
        return targetClass.getConstructor().newInstance();
    }

    private static Constructor<?> findConstructor(Class<?> targetClass, Object... args) throws NoSuchMethodException {
        for (Constructor<?> constructor : targetClass.getConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length != args.length) {
                continue;
            }
            boolean matches = true;
            for (int index = 0; index < parameterTypes.length; index++) {
                if (args[index] == null || !wrap(parameterTypes[index]).isInstance(args[index])) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return constructor;
            }
        }
        throw new NoSuchMethodException(targetClass.getName());
    }

    private static Method findSingleParameterMethod(Class<?> targetClass, String name, Class<?> parameterType)
            throws NoSuchMethodException {
        for (Method method : targetClass.getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (method.getName().equals(name)
                    && parameterTypes.length == 1
                    && wrap(parameterTypes[0]).equals(wrap(parameterType))) {
                return method;
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static Class<?> requireClass(Class<?> type, String name) {
        if (type == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return type;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (boolean.class.equals(type)) {
            return Boolean.class;
        }
        if (int.class.equals(type)) {
            return Integer.class;
        }
        if (long.class.equals(type)) {
            return Long.class;
        }
        if (double.class.equals(type)) {
            return Double.class;
        }
        if (float.class.equals(type)) {
            return Float.class;
        }
        if (short.class.equals(type)) {
            return Short.class;
        }
        if (byte.class.equals(type)) {
            return Byte.class;
        }
        if (char.class.equals(type)) {
            return Character.class;
        }
        return type;
    }
}
