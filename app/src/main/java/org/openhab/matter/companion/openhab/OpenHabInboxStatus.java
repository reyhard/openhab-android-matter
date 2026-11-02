package org.openhab.matter.companion.openhab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class OpenHabInboxStatus {
    private final boolean reachable;
    private final boolean matterEntryDetected;
    private final String message;
    private final String details;
    private final Set<String> matterEntryIds;
    private final List<OpenHabInboxEntry> entries;
    private final boolean responseIncomplete;

    public OpenHabInboxStatus(boolean reachable, boolean matterEntryDetected, String message, String details) {
        this(reachable, matterEntryDetected, message, details, Collections.emptySet());
    }

    public OpenHabInboxStatus(
            boolean reachable,
            boolean matterEntryDetected,
            String message,
            String details,
            Set<String> matterEntryIds) {
        this(reachable, matterEntryDetected, message, details, matterEntryIds, Collections.emptyList(), false);
    }

    public OpenHabInboxStatus(
            boolean reachable,
            boolean matterEntryDetected,
            String message,
            String details,
            List<OpenHabInboxEntry> entries) {
        this(reachable, matterEntryDetected, message, details, entries, false);
    }

    public OpenHabInboxStatus(
            boolean reachable,
            boolean matterEntryDetected,
            String message,
            String details,
            List<OpenHabInboxEntry> entries,
            boolean responseIncomplete) {
        this(reachable, matterEntryDetected, message, details, matterEntryIds(entries), entries, responseIncomplete);
    }

    private OpenHabInboxStatus(
            boolean reachable,
            boolean matterEntryDetected,
            String message,
            String details,
            Set<String> matterEntryIds,
            List<OpenHabInboxEntry> entries,
            boolean responseIncomplete) {
        this.reachable = reachable;
        this.matterEntryDetected = matterEntryDetected;
        this.message = message;
        this.details = details;
        this.matterEntryIds = Collections.unmodifiableSet(new LinkedHashSet<>(matterEntryIds));
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
        this.responseIncomplete = responseIncomplete;
    }

    public boolean reachable() {
        return reachable;
    }

    public boolean matterEntryDetected() {
        return matterEntryDetected;
    }

    public String message() {
        return message;
    }

    public String details() {
        return details;
    }

    public Set<String> matterEntryIds() {
        return matterEntryIds;
    }

    public List<OpenHabInboxEntry> entries() {
        return entries;
    }

    public boolean responseIncomplete() {
        return responseIncomplete;
    }

    public List<OpenHabInboxEntry> matterEntries() {
        List<OpenHabInboxEntry> matterEntries = new ArrayList<>();
        for (OpenHabInboxEntry entry : entries) {
            if (entry.matterEntry()) {
                matterEntries.add(entry);
            }
        }
        return Collections.unmodifiableList(matterEntries);
    }

    public boolean containsNodeId(long nodeId) {
        for (OpenHabInboxEntry entry : entries) {
            if (entry.matterEntry() && entry.matchesNodeId(nodeId)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> matterEntryIds(List<OpenHabInboxEntry> entries) {
        Set<String> ids = new LinkedHashSet<>();
        for (OpenHabInboxEntry entry : entries) {
            if (entry.matterEntry() && !entry.thingUid().isEmpty()) {
                ids.add(entry.thingUid());
            }
        }
        return ids;
    }
}
