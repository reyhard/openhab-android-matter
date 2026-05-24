# CHIP JNI Integration

## Native Functions Required

`ChipMatterController` needs a native library exposing:

```java
private static native long nativeCommissionBleThread(String datasetHex, long pin, int discriminator);
private static native String nativeOpenCommissioningWindow(long nodeId, int timeoutSeconds, int discriminator);
```

## connectedhomeip Behavior To Mirror

- `chip-tool pairing ble-thread <NODE_ID> hex:<DATASET_HEX> <PIN> <DISCRIMINATOR>`
- `chip-tool pairing open-commissioning-window <NODE_ID> 1 300 1000 <DISCRIMINATOR>`

## Acceptance

- Given a known Thread device in pairing mode and an OTBR Active Operational Dataset, Android commissions the device over BLE.
- OTBR CLI shows the device joined the Thread network.
- Android opens a commissioning window and displays a temporary setup code.
- Entering the temporary code in openHAB Matter Scan Input makes the device appear in the Inbox.
