package org.openhab.matter.companion.openhab;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class OpenHabInboxStatus {
    private final boolean reachable;
    private final boolean matterEntryDetected;
    private final String message;
    private final String details;
    private final Set<String> matterEntryIds;

    public OpenHabInboxStatus(boolean reachable, boolean matterEntryDetected, String message, String details) {
        this(reachable, matterEntryDetected, message, details, Collections.emptySet());
    }

    public OpenHabInboxStatus(
            boolean reachable,
            boolean matterEntryDetected,
            String message,
            String details,
            Set<String> matterEntryIds) {
        this.reachable = reachable;
        this.matterEntryDetected = matterEntryDetected;
        this.message = message;
        this.details = details;
        this.matterEntryIds = Collections.unmodifiableSet(new LinkedHashSet<>(matterEntryIds));
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
}
