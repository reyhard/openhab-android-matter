### **1. Prerequisites**

#### **Hardware:**

- **ESP32-H2:** To be used as a Radio Co-Processor (RCP).
- **ESP32-C6:** To host the OpenThread Border Router (OTBR).
- **Raspberry Pi:** To run OpenHAB 

#### **Software:**

- **ESP-IDF (v5.5 branch recommended):** The development framework for ESP32.
- [ESP Thread BR Firmware](https://github.com/espressif/esp-idf/tree/v5.5/examples/openthread/ot_br)**:** For the ESP32-C6.
- [RCP Firmware](https://github.com/espressif/esp-idf/tree/v5.5/examples/openthread/ot_rcp)**:** For the ESP32-H2.

### **2. Network and Border Router Setup**

This section covers flashing the ESP32 devices and establishing the Thread network.

- **On your Development Machine:**

  1. **Build and Flash RCP Firmware:** Compile the RCP firmware for the ESP32-H2 and flash the device.

     - **Note:** You might encounter compile errors. A potential fix involves manually adding the following lines to esp_ot_config.h:

       ```apache
           #define OPENTHREAD_SPINEL_CONFIG_COMPATIBILITY_ERROR_CALLBACK_ENABLE
       #define OPENTHREAD_SPINEL_CONFIG_COPROCESSOR_RESET_FAILURE_CALLBACK_ENABLE
         
       ```

  2. **Connect Devices:** Connect the flashed ESP32-H2 to the ESP32-C6 via UART. See setup on https://github.com/espressif/esp-idf/tree/v5.5/examples/openthread/ot_br

  3. **Configure OTBR Firmware:** Before building, run idf.py menuconfig and set the following to ensure the border router starts  automatically with your network credentials:

     - Enable OpenThread Border Router Example → Enable the automatic start mode in Thread Border Router.
     - Configure Example Connection Configuration → connect using WiFi interface and fill in your Wi-Fi SSID and password.

  4. **Build and Flash OTBR Firmware:** Build and flash the configured ESP Thread BR firmware to the ESP32-C6.

- **On the OTBR CLI (Serial Monitor):**

  1. **Confirm Network Connectivity:** Ensure the ESP32-C6  OTBR has an IPv6 address on your LAN. You can verify this by pinging its IPv6 address from another machine on the same network.

  2. **Get the Operational Dataset:** Run the following command to retrieve the active Thread network credentials:

     ```bash
         dataset active -x
       
     ```

  3. **Copy the Dataset:** A long hexadecimal string will be returned. Copy this string for the pairing process.

### **3. Prepare Commissioning Tools on the Raspberry Pi**

- **Ensure mDNS is Working:** The chip-tool relies on mDNS for device discovery. Install Avahi if it’s missing:

  ```bash
      sudo apt-get install avahi-utils
    
  ```

------