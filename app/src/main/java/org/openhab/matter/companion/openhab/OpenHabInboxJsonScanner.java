package org.openhab.matter.companion.openhab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class OpenHabInboxJsonScanner {
    private OpenHabInboxJsonScanner() {
    }

    static ScanResult scan(String json) {
        List<OpenHabInboxEntry> entries = new ArrayList<>();
        if (json == null) {
            return new ScanResult(entries, false);
        }

        int length = json.length();
        int index = skipWhitespace(json, 0);
        if (index >= length || json.charAt(index) != '[') {
            return new ScanResult(entries, false);
        }

        index++;
        boolean truncated = false;
        while (true) {
            index = skipWhitespace(json, index);
            if (index >= length) {
                truncated = true;
                break;
            }
            char current = json.charAt(index);
            if (current == ']') {
                break;
            }
            if (current == ',') {
                index++;
                continue;
            }
            if (current != '{') {
                int valueEnd = findJsonValueEnd(json, index);
                if (valueEnd < 0) {
                    truncated = true;
                    break;
                }
                index = valueEnd;
                continue;
            }

            int objectEnd = findObjectEnd(json, index);
            if (objectEnd < 0) {
                truncated = true;
                break;
            }
            entries.add(parseEntry(json, index, objectEnd));
            index = objectEnd + 1;
        }
        return new ScanResult(entries, truncated);
    }

    private static OpenHabInboxEntry parseEntry(String json, int objectStart, int objectEnd) {
        ObjectFields fields = parseObjectScalars(json, objectStart + 1, objectEnd);
        String nodeId = "";
        if (fields.propertiesStart >= 0) {
            ObjectFields properties = parseObjectScalars(
                    json,
                    fields.propertiesStart + 1,
                    fields.propertiesEnd);
            nodeId = properties.values.getOrDefault("nodeId", "");
        }
        return new OpenHabInboxEntry(
                fields.values.get("thingUID"),
                fields.values.get("label"),
                fields.values.get("flag"),
                nodeId);
    }

    private static ObjectFields parseObjectScalars(String json, int start, int end) {
        Map<String, String> fields = new HashMap<>();
        int propertiesStart = -1;
        int propertiesEnd = -1;
        int index = start;
        while (index < end) {
            index = skipWhitespace(json, index);
            if (index >= end || json.charAt(index) == '}') {
                break;
            }
            if (json.charAt(index) != '"') {
                break;
            }

            ParsedString key = parseString(json, index);
            if (key == null) {
                break;
            }
            index = skipWhitespace(json, key.nextIndex);
            if (index >= end || json.charAt(index) != ':') {
                break;
            }
            index = skipWhitespace(json, index + 1);
            if (index >= end) {
                break;
            }

            char valueStart = json.charAt(index);
            if (valueStart == '"') {
                ParsedString value = parseString(json, index);
                if (value == null) {
                    break;
                }
                fields.put(key.value, value.value);
                index = value.nextIndex;
            } else if (valueStart == '{') {
                int valueEnd = findObjectEnd(json, index);
                if (valueEnd < 0 || valueEnd >= end) {
                    break;
                }
                if ("properties".equals(key.value)) {
                    propertiesStart = index;
                    propertiesEnd = valueEnd;
                }
                index = valueEnd + 1;
            } else if (valueStart == '[') {
                int valueEnd = findJsonValueEnd(json, index);
                if (valueEnd < 0 || valueEnd > end) {
                    break;
                }
                index = valueEnd;
            } else {
                int valueEnd = index;
                while (valueEnd < end && json.charAt(valueEnd) != ',' && json.charAt(valueEnd) != '}') {
                    valueEnd++;
                }
                String value = json.substring(index, valueEnd).trim();
                fields.put(key.value, "null".equals(value) ? "" : value);
                index = valueEnd;
            }

            index = skipWhitespace(json, index);
            if (index < end && json.charAt(index) == ',') {
                index++;
            } else if (index >= end || json.charAt(index) != '}') {
                break;
            }
        }
        return new ObjectFields(fields, propertiesStart, propertiesEnd);
    }

    private static ParsedString parseString(String json, int quoteStart) {
        StringBuilder value = new StringBuilder();
        int index = quoteStart + 1;
        while (index < json.length()) {
            char character = json.charAt(index);
            if (character == '"') {
                return new ParsedString(value.toString(), index + 1);
            }
            if (character == '\\') {
                index++;
                if (index >= json.length()) {
                    return null;
                }
                char escaped = json.charAt(index);
                switch (escaped) {
                    case '"':
                    case '\\':
                    case '/':
                        value.append(escaped);
                        break;
                    case 'b':
                        value.append('\b');
                        break;
                    case 'f':
                        value.append('\f');
                        break;
                    case 'n':
                        value.append('\n');
                        break;
                    case 'r':
                        value.append('\r');
                        break;
                    case 't':
                        value.append('\t');
                        break;
                    case 'u':
                        if (index + 4 >= json.length()) {
                            return null;
                        }
                        try {
                            value.append((char) Integer.parseInt(json.substring(index + 1, index + 5), 16));
                        } catch (NumberFormatException exception) {
                            return null;
                        }
                        index += 4;
                        break;
                    default:
                        return null;
                }
                index++;
                continue;
            }
            value.append(character);
            index++;
        }
        return null;
    }

    private static int findObjectEnd(String json, int objectStart) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = objectStart; index < json.length(); index++) {
            char character = json.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (character == '\\') {
                    escaped = true;
                } else if (character == '"') {
                    inString = false;
                }
                continue;
            }
            if (character == '"') {
                inString = true;
            } else if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private static int findJsonValueEnd(String json, int valueStart) {
        char first = json.charAt(valueStart);
        if (first == '"') {
            ParsedString string = parseString(json, valueStart);
            return string == null ? -1 : string.nextIndex;
        }
        if (first == '{' || first == '[') {
            char closing = first == '{' ? '}' : ']';
            int depth = 0;
            boolean inString = false;
            boolean escaped = false;
            for (int index = valueStart; index < json.length(); index++) {
                char character = json.charAt(index);
                if (inString) {
                    if (escaped) {
                        escaped = false;
                    } else if (character == '\\') {
                        escaped = true;
                    } else if (character == '"') {
                        inString = false;
                    }
                    continue;
                }
                if (character == '"') {
                    inString = true;
                } else if (character == first) {
                    depth++;
                } else if (character == closing) {
                    depth--;
                    if (depth == 0) {
                        return index + 1;
                    }
                }
            }
            return -1;
        }

        int index = valueStart;
        while (index < json.length()) {
            char character = json.charAt(index);
            if (Character.isWhitespace(character) || character == ',' || character == ']' || character == '}') {
                break;
            }
            index++;
        }
        return index;
    }

    private static int skipWhitespace(String json, int index) {
        while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
            index++;
        }
        return index;
    }

    static final class ScanResult {
        private final List<OpenHabInboxEntry> entries;
        private final boolean truncated;

        ScanResult(List<OpenHabInboxEntry> entries, boolean truncated) {
            this.entries = entries;
            this.truncated = truncated;
        }

        List<OpenHabInboxEntry> entries() {
            return entries;
        }

        boolean truncated() {
            return truncated;
        }
    }

    private static final class ParsedString {
        private final String value;
        private final int nextIndex;

        ParsedString(String value, int nextIndex) {
            this.value = value;
            this.nextIndex = nextIndex;
        }
    }

    private static final class ObjectFields {
        private final Map<String, String> values;
        private final int propertiesStart;
        private final int propertiesEnd;

        ObjectFields(Map<String, String> values, int propertiesStart, int propertiesEnd) {
            this.values = values;
            this.propertiesStart = propertiesStart;
            this.propertiesEnd = propertiesEnd;
        }
    }
}
