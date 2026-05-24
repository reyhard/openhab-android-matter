package org.openhab.matter.companion;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.openhab.matter.companion.controller.FakeMatterController;
import org.openhab.matter.companion.controller.MatterController;
import org.openhab.matter.companion.domain.MatterSetupPayload;
import org.openhab.matter.companion.domain.MatterSetupPayloadParser;
import org.openhab.matter.companion.domain.OpenHabInstructions;
import org.openhab.matter.companion.domain.ThreadDataset;
import org.openhab.matter.companion.ui.AppState;

public final class MainActivity extends Activity {
    private static final String KEY_DATASET = "dataset";
    private static final String KEY_SETUP_PAYLOAD = "setupPayload";
    private static final String KEY_LOGS = "logs";
    private static final String KEY_TEMPORARY_CODE = "temporaryCode";
    private static final String KEY_COMMISSIONED_NODE_ID = "commissionedNodeId";
    private static final int DATASET_INPUT_ID = 1001;
    private static final int PAYLOAD_INPUT_ID = 1002;
    private static final int PANEL_COLOR = Color.rgb(255, 251, 240);
    private static final int TEXT_COLOR = Color.rgb(36, 50, 48);
    private static final int MUTED_COLOR = Color.rgb(74, 94, 90);

    private final AppState state = new AppState();
    private final MatterController controller = new FakeMatterController();
    private TextView output;
    private EditText datasetInput;
    private EditText payloadInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        Button commissionThread = button("Simulate Thread commissioning");
        Button openWindow = button("Simulate open commissioning window");
        Button wifiHandoff = button("Wi-Fi / multi-admin openHAB handoff");
        output = label("", 15, TEXT_COLOR);
        output.setTextIsSelectable(true);

        commissionThread.setOnClickListener(view -> runCommissioning());
        openWindow.setOnClickListener(view -> runOpenCommissioningWindow());
        wifiHandoff.setOnClickListener(view -> showWifiInstructions());

        root.addView(title);
        root.addView(subtitle);
        root.addView(demoNotice);
        root.addView(section("Thread dataset"));
        root.addView(datasetInput);
        root.addView(section("Matter setup payload"));
        root.addView(payloadInput);
        root.addView(commissionThread);
        root.addView(openWindow);
        root.addView(wifiHandoff);
        root.addView(section("Guide output"));
        root.addView(output);

        setContentView(scrollView);
        output.setText(state.logs);
        if (state.logs.isEmpty()) {
            append("Paste your OTBR Thread dataset and Matter setup payload. Sensitive input is validated but not echoed in this log.");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        state.dataset = datasetInput.getText().toString();
        state.setupPayload = payloadInput.getText().toString();
        outState.putString(KEY_DATASET, state.dataset);
        outState.putString(KEY_SETUP_PAYLOAD, state.setupPayload);
        outState.putString(KEY_LOGS, state.logs);
        outState.putString(KEY_TEMPORARY_CODE, state.temporaryCode);
        outState.putLong(KEY_COMMISSIONED_NODE_ID, state.commissionedNodeId);
    }

    private void runCommissioning() {
        state.dataset = datasetInput.getText().toString();
        state.setupPayload = payloadInput.getText().toString();

        try {
            ThreadDataset dataset = ThreadDataset.parse(state.dataset);
            MatterSetupPayload payload = MatterSetupPayloadParser.parse(state.setupPayload);
            if (payload.requiresChipParser()) {
                append("MT: QR/manual payload detected. The CHIP parser is required to decode it; for this MVP enter explicit fields instead: pin=...;disc=...;vendor=...;product=...");
                return;
            }

            append("Starting simulated Thread commissioning. No real BLE, Thread, or Matter operation will be performed.");
            append("Validated Thread dataset without displaying it.");
            append("Validated explicit Matter PIN and discriminator without displaying the PIN.");
            state.commissionedNodeId = controller.commissionBleThread(dataset, payload, step -> append(step.message()));
            append("Simulated bootstrap node id: " + state.commissionedNodeId);
        } catch (Exception ex) {
            append("Error: " + ex.getMessage());
        }
    }

    private void runOpenCommissioningWindow() {
        try {
            if (state.commissionedNodeId < 0) {
                append("Run simulated Thread commissioning first so the demo has a bootstrap node id.");
                return;
            }

            append("Opening a simulated commissioning window. This does not call a real Matter controller.");
            state.temporaryCode = controller.openCommissioningWindow(state.commissionedNodeId, 300, 3840,
                    step -> append(step.message()));
            append(OpenHabInstructions.scanInputInstructions(state.temporaryCode));
            append(OpenHabInstructions.troubleshooting());
        } catch (Exception ex) {
            append("Error: " + ex.getMessage());
        }
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

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        state.dataset = savedInstanceState.getString(KEY_DATASET, "");
        state.setupPayload = savedInstanceState.getString(KEY_SETUP_PAYLOAD, "");
        state.logs = savedInstanceState.getString(KEY_LOGS, "");
        state.temporaryCode = savedInstanceState.getString(KEY_TEMPORARY_CODE, "");
        state.commissionedNodeId = savedInstanceState.getLong(KEY_COMMISSIONED_NODE_ID, -1L);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
