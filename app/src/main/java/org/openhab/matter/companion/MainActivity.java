package org.openhab.matter.companion;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.openhab.matter.companion.controller.ChipMatterController;
import org.openhab.matter.companion.controller.ChipMatterControllerStatus;
import org.openhab.matter.companion.config.AppConfig;
import org.openhab.matter.companion.config.AppConfigRepository;
import org.openhab.matter.companion.config.SharedPreferencesAppConfigRepository;
import org.openhab.matter.companion.controller.FakeMatterController;
import org.openhab.matter.companion.controller.MatterController;
import org.openhab.matter.companion.controller.MatterControllerSelection;
import org.openhab.matter.companion.controller.MatterControllerSelector;
import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.MatterSetupPayloadParser;
import org.openhab.matter.companion.domain.OpenHabInstructions;
import org.openhab.matter.companion.domain.ThreadDataset;
import org.openhab.matter.companion.openhab.HttpOpenHabClient;
import org.openhab.matter.companion.openhab.HttpOpenHabInboxClient;
import org.openhab.matter.companion.openhab.OpenHabClient;
import org.openhab.matter.companion.openhab.OpenHabInboxClient;
import org.openhab.matter.companion.openhab.OpenHabInboxSseClient;
import org.openhab.matter.companion.openhab.OpenHabInboxStatus;
import org.openhab.matter.companion.openhab.OpenHabStatus;
import org.openhab.matter.companion.permissions.CommissioningPermissionPlanner;
import org.openhab.matter.companion.qr.QrScanIntentFactory;
import org.openhab.matter.companion.ui.AppState;

import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    private static final String KEY_DATASET = "dataset";
    private static final String KEY_SETUP_PAYLOAD = "setupPayload";
    private static final String KEY_OPENHAB_BASE_URL = "openHabBaseUrl";
    private static final String KEY_LOGS = "logs";
    private static final String KEY_TEMPORARY_CODE = "temporaryCode";
    private static final String KEY_COMMISSIONED_NODE_ID = "commissionedNodeId";
    private static final String KEY_NATIVE_CONTROLLER_SELECTED = "nativeControllerSelected";
    private static final int COMMISSIONING_PERMISSION_REQUEST = 2001;
    private static final int QR_SCAN_REQUEST = 3001;
    private static final int DATASET_INPUT_ID = 1001;
    private static final int PAYLOAD_INPUT_ID = 1002;
    private static final int OPENHAB_INPUT_ID = 1003;
    private static final int PANEL_COLOR = Color.rgb(255, 251, 240);
    private static final int TEXT_COLOR = Color.rgb(36, 50, 48);
    private static final int MUTED_COLOR = Color.rgb(74, 94, 90);

    private final AppState state = new AppState();
    private final MatterController fakeMatterController = new FakeMatterController();
    private final ChipMatterController chipMatterController = new ChipMatterController();
    private final OpenHabClient openHabClient = new HttpOpenHabClient();
    private final OpenHabInboxClient openHabInboxClient = new HttpOpenHabInboxClient();
    private final OpenHabInboxSseClient openHabInboxSseClient = new OpenHabInboxSseClient();
    private MatterController controller = fakeMatterController;
    private AppConfigRepository configRepository;
    private TextView output;
    private EditText datasetInput;
    private EditText payloadInput;
    private EditText openHabInput;
    private boolean nativeMatterControllerSelected;
    private boolean restoreNativeControllerSelection;
    private boolean persistedThreadDatasetUnreadable;
    private volatile boolean sseWatchActive;
    private Thread sseWatchThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configRepository = new SharedPreferencesAppConfigRepository(this);
        loadPersistedConfig();
        restoreState(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(24));
        root.setBackgroundColor(Color.rgb(247, 243, 234));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = label("openHAB Matter Helper", 28, TEXT_COLOR);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        TextView subtitle = label(
                "Native commissioning guide for Matter-over-Thread and Wi-Fi/multi-admin handoff.",
                16,
                MUTED_COLOR);
        TextView demoNotice = panel(
                "Demo mode: this installable MVP uses FakeMatterController. It simulates the flow only; it does not perform real BLE scanning, Thread provisioning, PASE, attestation, or Matter commissioning.");

        datasetInput = input("Thread Active Operational Dataset hex", true);
        datasetInput.setId(DATASET_INPUT_ID);
        datasetInput.setText(state.dataset);
        payloadInput = input("Matter setup payload: MT:... or pin=20202021;disc=3840;vendor=Aqara;product=U200", false);
        payloadInput.setId(PAYLOAD_INPUT_ID);
        payloadInput.setText(state.setupPayload);
        openHabInput = input("openHAB base URL, for example http://openhab.local:8080", false);
        openHabInput.setId(OPENHAB_INPUT_ID);
        openHabInput.setText(state.openHabBaseUrl);

        Button commissionThread = button("Simulate Thread commissioning");
        Button openWindow = button("Simulate open commissioning window");
        Button wifiHandoff = button("Wi-Fi / multi-admin openHAB handoff");
        Button scanQr = button("Scan Matter QR with installed scanner");
        Button checkOpenHab = button("Check openHAB readiness");
        Button checkPermissions = button("Check commissioning permissions");
        Button checkInbox = button("Check openHAB Inbox");
        Button watchInboxSse = button("Watch openHAB Inbox SSE");
        Button checkChip = button("Check native CHIP controller");
        Button useNativeChip = button("Use native CHIP controller if ready");
        Button saveConfig = button("Save dataset and openHAB URL");
        output = label("", 15, TEXT_COLOR);
        output.setTextIsSelectable(true);

        commissionThread.setOnClickListener(view -> runCommissioning());
        openWindow.setOnClickListener(view -> runOpenCommissioningWindow());
        wifiHandoff.setOnClickListener(view -> showWifiInstructions());
        scanQr.setOnClickListener(view -> scanMatterQrWithExternalScanner());
        checkOpenHab.setOnClickListener(view -> checkOpenHabReadiness());
        checkPermissions.setOnClickListener(view -> checkCommissioningPermissions());
        checkInbox.setOnClickListener(view -> checkOpenHabInbox());
        watchInboxSse.setOnClickListener(view -> watchOpenHabInboxSse());
        checkChip.setOnClickListener(view -> checkNativeChipController());
        useNativeChip.setOnClickListener(view -> useNativeChipControllerIfReady());
        saveConfig.setOnClickListener(view -> saveConfiguration());

        root.addView(title);
        root.addView(subtitle);
        root.addView(demoNotice);
        root.addView(section("Thread dataset"));
        root.addView(datasetInput);
        root.addView(section("Matter setup payload"));
        root.addView(payloadInput);
        root.addView(section("openHAB readiness"));
        root.addView(openHabInput);
        root.addView(commissionThread);
        root.addView(openWindow);
        root.addView(wifiHandoff);
        root.addView(scanQr);
        root.addView(checkOpenHab);
        root.addView(checkPermissions);
        root.addView(checkInbox);
        root.addView(watchInboxSse);
        root.addView(checkChip);
        root.addView(useNativeChip);
        root.addView(saveConfig);
        root.addView(section("Guide output"));
        root.addView(output);

        setContentView(scrollView);
        output.setText(state.logs);
        if (restoreNativeControllerSelection) {
            restoreNativeChipControllerSelectionAsync();
        }
        if (state.logs.isEmpty()) {
            append("Paste your OTBR Thread dataset and Matter setup payload. Sensitive input is validated but not echoed in this log.");
            if (persistedThreadDatasetUnreadable) {
                append(MainActivityPresentation.threadDatasetUnreadable());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != COMMISSIONING_PERMISSION_REQUEST) {
            return;
        }

        append(MainActivityPresentation.runtimePermissionRequestResult(permissions, grantResults));
    }

    @Override
    protected void onStop() {
        stopOpenHabInboxSseWatch();
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != QR_SCAN_REQUEST) {
            return;
        }
        if (resultCode != RESULT_OK) {
            append("QR scanner did not return a Matter payload.");
            return;
        }
        String scanResult = QrScanIntentFactory.extractMatterSetupPayload(data);
        if (scanResult.isEmpty()) {
            append(MainActivityPresentation.invalidExternalQrScannerResult());
            return;
        }
        state.setupPayload = scanResult;
        payloadInput.setText(scanResult);
        append("Scanned Matter QR payload and populated setup payload input.");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        state.dataset = datasetInput.getText().toString();
        state.setupPayload = payloadInput.getText().toString();
        state.openHabBaseUrl = openHabInput.getText().toString();
        outState.putString(KEY_DATASET, state.dataset);
        outState.putString(KEY_SETUP_PAYLOAD, state.setupPayload);
        outState.putString(KEY_OPENHAB_BASE_URL, state.openHabBaseUrl);
        outState.putString(KEY_LOGS, state.logs);
        outState.putString(KEY_TEMPORARY_CODE, state.temporaryCode);
        outState.putLong(KEY_COMMISSIONED_NODE_ID, state.commissionedNodeId);
        outState.putBoolean(KEY_NATIVE_CONTROLLER_SELECTED, nativeMatterControllerSelected);
    }

    private void runCommissioning() {
        state.dataset = datasetInput.getText().toString();
        state.setupPayload = payloadInput.getText().toString();

        ThreadDataset dataset;
        MatterSetupPayload payload;
        try {
            dataset = ThreadDataset.parse(state.dataset);
            payload = MatterSetupPayloadParser.parse(state.setupPayload);
        } catch (Exception ex) {
            append("Commissioning input could not be validated. Check the dataset and setup payload format; sensitive values are not repeated in this log.");
            return;
        }

        MatterController selectedController = controller;
        append(nativeMatterControllerSelected
                ? "Starting native CHIP Thread commissioning."
                : "Starting simulated Thread commissioning. No real BLE, Thread, or Matter operation will be performed.");
        append("Validated Thread dataset without displaying it.");
        append(payloadSummary(payload));
        new Thread(() -> {
            try {
                long nodeId = selectedController.commissionBleThread(dataset, payload, step -> appendFromWorker(step.message()));
                runOnUiThread(() -> {
                    state.commissionedNodeId = nodeId;
                    append("Bootstrap node id: " + state.commissionedNodeId);
                });
            } catch (Exception ex) {
                appendFromWorker(MainActivityPresentation.matterControllerOperationFailed(ex.getMessage()));
            }
        }, "matter-commissioning").start();
    }

    private void runOpenCommissioningWindow() {
        if (state.commissionedNodeId < 0) {
            append("Run simulated Thread commissioning first so the demo has a bootstrap node id.");
            return;
        }

        MatterController selectedController = controller;
        long nodeId = state.commissionedNodeId;
        append(nativeMatterControllerSelected
                ? "Opening native CHIP commissioning window."
                : "Opening a simulated commissioning window. This does not call a real Matter controller.");
        new Thread(() -> {
            try {
                String temporaryCode = selectedController.openCommissioningWindow(nodeId, 300, 3840,
                        step -> appendFromWorker(step.message()));
                runOnUiThread(() -> {
                    state.temporaryCode = temporaryCode;
                    append(OpenHabInstructions.scanInputInstructions(state.temporaryCode));
                    append(OpenHabInstructions.troubleshooting());
                });
            } catch (Exception ex) {
                appendFromWorker(MainActivityPresentation.matterControllerOperationFailed(ex.getMessage()));
            }
        }, "matter-open-commissioning-window").start();
    }

    private void showWifiInstructions() {
        state.setupPayload = payloadInput.getText().toString();
        if (state.setupPayload == null || state.setupPayload.trim().isEmpty()) {
            append("Paste or enter a Matter setup or multi-admin code first.");
            return;
        }

        append("Wi-Fi / multi-admin handoff selected. The phone is not commissioning this device in demo mode.");
        append(OpenHabInstructions.scanInputInstructionsWithoutEcho());
    }

    private void scanMatterQrWithExternalScanner() {
        new AlertDialog.Builder(this)
                .setTitle("External QR scanner")
                .setMessage(MainActivityPresentation.externalQrScannerTrustNotice())
                .setPositiveButton("Open scanner", (dialog, which) -> launchExternalQrScanner())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void launchExternalQrScanner() {
        try {
            startActivityForResult(QrScanIntentFactory.createScanIntent(), QR_SCAN_REQUEST);
        } catch (ActivityNotFoundException ex) {
            append(MainActivityPresentation.externalQrScannerMissing());
        }
    }

    private void checkNativeChipController() {
        new Thread(() -> {
            ChipMatterControllerStatus status = chipMatterController.readiness();
            appendFromWorker(MainActivityPresentation.nativeChipReadiness(status));
        }, "matter-chip-readiness").start();
    }

    private void useNativeChipControllerIfReady() {
        selectNativeChipControllerAsync(true);
    }

    private void saveConfiguration() {
        state.dataset = datasetInput.getText().toString();
        state.openHabBaseUrl = openHabInput.getText().toString();
        try {
            if (state.dataset != null && !state.dataset.trim().isEmpty()) {
                ThreadDataset.parse(state.dataset);
            }
            configRepository.save(new AppConfig(state.dataset, state.openHabBaseUrl));
            append(MainActivityPresentation.encryptedConfigSaved());
        } catch (Exception ex) {
            append("Configuration was not saved. Check the Thread dataset format; sensitive values are not repeated in this log.");
        }
    }

    private void checkOpenHabReadiness() {
        state.openHabBaseUrl = openHabInput.getText().toString();
        if (state.openHabBaseUrl == null || state.openHabBaseUrl.trim().isEmpty()) {
            append("Enter an openHAB base URL first.");
            return;
        }

        append("Checking openHAB REST readiness at " + MainActivityPresentation.safeUrlForLog(state.openHabBaseUrl) + " ...");
        new Thread(() -> {
            OpenHabStatus status;
            try {
                status = openHabClient.checkReadiness(state.openHabBaseUrl);
            } catch (Exception ex) {
                status = new OpenHabStatus(false, "openHAB readiness check failed", ex.getMessage());
            }
            OpenHabStatus finalStatus = status;
            runOnUiThread(() -> {
                append(finalStatus.message());
                if (finalStatus.details() != null && !finalStatus.details().isEmpty()) {
                    append(MainActivityPresentation.safeTextForLog(finalStatus.details()));
                }
            });
        }, "openhab-readiness-check").start();
    }

    private void checkCommissioningPermissions() {
        List<String> requiredPermissions = CommissioningPermissionPlanner.requiredPermissions(Build.VERSION.SDK_INT);
        if (requiredPermissions.isEmpty()) {
            append(MainActivityPresentation.runtimePermissionsNotNeeded());
            return;
        }

        List<String> missingPermissions = new ArrayList<>();
        for (String permission : requiredPermissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (missingPermissions.isEmpty()) {
            append(MainActivityPresentation.runtimePermissionsAlreadyGranted(requiredPermissions));
            return;
        }

        append(MainActivityPresentation.runtimePermissionsRequested(missingPermissions));
        requestPermissions(missingPermissions.toArray(new String[0]), COMMISSIONING_PERMISSION_REQUEST);
    }

    private void checkOpenHabInbox() {
        state.openHabBaseUrl = openHabInput.getText().toString();
        if (state.openHabBaseUrl == null || state.openHabBaseUrl.trim().isEmpty()) {
            append("Enter an openHAB base URL first.");
            return;
        }

        String baseUrl = state.openHabBaseUrl.trim();
        append("Checking openHAB Inbox at " + MainActivityPresentation.safeUrlForLog(baseUrl) + " ...");
        new Thread(() -> {
            OpenHabInboxStatus status;
            try {
                status = openHabInboxClient.checkInbox(baseUrl);
            } catch (Exception ex) {
                status = new OpenHabInboxStatus(false, false, "openHAB Inbox check failed", ex.getMessage());
            }
            OpenHabInboxStatus finalStatus = status;
            runOnUiThread(() -> {
                append(MainActivityPresentation.openHabInboxResult(finalStatus));
                append(finalStatus.message());
                if (finalStatus.details() != null && !finalStatus.details().isEmpty()) {
                    append(MainActivityPresentation.safeTextForLog(finalStatus.details()));
                }
            });
        }, "openhab-inbox-check").start();
    }

    private void watchOpenHabInboxSse() {
        state.openHabBaseUrl = openHabInput.getText().toString();
        if (state.openHabBaseUrl == null || state.openHabBaseUrl.trim().isEmpty()) {
            append("Enter an openHAB base URL first.");
            return;
        }
        if (sseWatchThread != null && sseWatchThread.isAlive()) {
            append("openHAB Inbox SSE watch is already running.");
            return;
        }

        String baseUrl = state.openHabBaseUrl.trim();
        append("Watching openHAB Inbox SSE at " + MainActivityPresentation.safeUrlForLog(baseUrl) + " ...");
        sseWatchActive = true;
        Thread watcher = new Thread(() -> {
            try {
                openHabInboxSseClient.observe(baseUrl, event -> {
                    runOnUiThread(() -> append(MainActivityPresentation.openHabInboxSseEvent(
                            event.matterEntryDetected())));
                    return !event.matterEntryDetected();
                }, () -> sseWatchActive && !Thread.currentThread().isInterrupted());
            } catch (Exception ex) {
                if (sseWatchActive) {
                    runOnUiThread(() -> append("openHAB Inbox SSE observation failed: "
                            + MainActivityPresentation.safeTextForLog(ex.getMessage())));
                }
            } finally {
                sseWatchActive = false;
                if (Thread.currentThread() == sseWatchThread) {
                    sseWatchThread = null;
                }
            }
        }, "openhab-inbox-sse");
        sseWatchThread = watcher;
        watcher.start();
    }

    private void stopOpenHabInboxSseWatch() {
        sseWatchActive = false;
        if (sseWatchThread != null) {
            sseWatchThread.interrupt();
        }
    }

    private String payloadSummary(MatterSetupPayload payload) {
        if (payload.rawPayload().startsWith("MT:")) {
            return "Decoded Matter QR payload: discriminator " + payload.discriminator()
                    + ", vendor ID " + payload.vendorId()
                    + ", product ID " + payload.productId()
                    + ", commissioning flow " + payload.commissioningFlow()
                    + ", discovery capabilities " + payload.discoveryCapabilities()
                    + ". Setup PIN decoded and hidden.";
        }
        return "Validated explicit Matter setup PIN and discriminator without displaying the PIN.";
    }

    private EditText input(String hint, boolean multiLine) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setTextColor(TEXT_COLOR);
        editText.setHintTextColor(Color.rgb(94, 111, 107));
        editText.setSingleLine(!multiLine);
        editText.setMinLines(multiLine ? 3 : 1);
        editText.setInputType(InputType.TYPE_CLASS_TEXT
                | (multiLine ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS));
        editText.setPadding(dp(12), dp(10), dp(12), dp(10));
        editText.setBackgroundColor(PANEL_COLOR);
        editText.setLayoutParams(blockParams());
        return editText;
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(TEXT_COLOR);
        button.setLayoutParams(blockParams());
        return button;
    }

    private TextView section(String text) {
        TextView textView = label(text, 18, TEXT_COLOR);
        textView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return textView;
    }

    private TextView panel(String text) {
        TextView textView = label(text, 15, TEXT_COLOR);
        textView.setBackgroundColor(Color.rgb(232, 241, 237));
        textView.setPadding(dp(14), dp(12), dp(14), dp(12));
        textView.setLayoutParams(blockParams());
        return textView;
    }

    private TextView label(String text, int sizeSp, int color) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(sizeSp);
        textView.setTextColor(color);
        textView.setPadding(0, dp(8), 0, dp(8));
        textView.setLineSpacing(0.0f, 1.08f);
        return textView;
    }

    private LinearLayout.LayoutParams blockParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(6), 0, dp(10));
        return params;
    }

    private void append(String message) {
        state.logs = state.logs + message + "\n";
        output.setText(state.logs);
    }

    private void appendFromWorker(String message) {
        runOnUiThread(() -> append(message));
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        state.dataset = savedInstanceState.getString(KEY_DATASET, "");
        state.setupPayload = savedInstanceState.getString(KEY_SETUP_PAYLOAD, "");
        state.openHabBaseUrl = savedInstanceState.getString(KEY_OPENHAB_BASE_URL, "");
        state.logs = savedInstanceState.getString(KEY_LOGS, "");
        state.temporaryCode = savedInstanceState.getString(KEY_TEMPORARY_CODE, "");
        state.commissionedNodeId = savedInstanceState.getLong(KEY_COMMISSIONED_NODE_ID, -1L);
        restoreNativeControllerSelection = savedInstanceState.getBoolean(KEY_NATIVE_CONTROLLER_SELECTED, false);
    }

    private void loadPersistedConfig() {
        AppConfig config = configRepository.load();
        state.dataset = config.threadDataset();
        state.openHabBaseUrl = config.openHabBaseUrl();
        persistedThreadDatasetUnreadable = config.threadDatasetUnreadable();
    }

    private void restoreNativeChipControllerSelectionAsync() {
        restoreNativeControllerSelection = false;
        selectNativeChipControllerAsync(false);
    }

    private void selectNativeChipControllerAsync(boolean appendWhenSelected) {
        new Thread(() -> {
            MatterControllerSelection selection = MatterControllerSelector.select(
                    fakeMatterController,
                    chipMatterController,
                    true);
            runOnUiThread(() -> {
                controller = selection.controller();
                nativeMatterControllerSelected = selection.nativeSelected();
                if (appendWhenSelected || !selection.nativeSelected()) {
                    append(MainActivityPresentation.matterControllerSelection(selection));
                }
            });
        }, "matter-controller-selection").start();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
