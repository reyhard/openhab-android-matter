package org.openhab.matter.companion.openhab;

import java.util.ArrayList;
import java.util.List;

public final class OpenHabInboxSseParser {
    private OpenHabInboxSseParser() {
    }

    public static OpenHabInboxEvent parse(String block) {
        String rawData = collectData(block == null ? "" : block);
        if (rawData.isEmpty()) {
            return new OpenHabInboxEvent("", "", false);
        }
        return new OpenHabInboxEvent(extractTopic(rawData), rawData, containsMatterEntry(rawData));
    }

    private static String collectData(String block) {
        List<String> dataLines = new ArrayList<>();
        String[] lines = block.split("\\r?\\n", -1);
        for (String line : lines) {
            if (line.startsWith("data:")) {
                String data = line.substring("data:".length());
                if (data.startsWith(" ")) {
                    data = data.substring(1);
                }
                dataLines.add(data);
            }
        }
        return joinLines(dataLines);
    }

    private static String joinLines(List<String> lines) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(lines.get(i));
        }
        return builder.toString();
    }

    private static String extractTopic(String rawData) {
        String marker = "\"topic\":\"";
        int start = rawData.indexOf(marker);
        if (start < 0) {
            return "";
        }
        int valueStart = start + marker.length();
        StringBuilder topic = new StringBuilder();
        boolean escaped = false;
        for (int i = valueStart; i < rawData.length(); i++) {
            char current = rawData.charAt(i);
            if (escaped) {
                topic.append(current);
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == '"') {
                return topic.toString();
            } else {
                topic.append(current);
            }
        }
        return "";
    }

    private static boolean containsMatterEntry(String rawData) {
        return rawData.contains("matter:") || rawData.contains("\"bindingId\":\"matter\"");
    }
}
