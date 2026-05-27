package org.openhab.matter.companion.controller;

import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicReference;

public final class ConnectedHomeIpDiagnostics {
    private static final ThreadLocal<Consumer<String>> LISTENER = new ThreadLocal<>();
    private static final AtomicReference<Consumer<String>> ACTIVE_LISTENER = new AtomicReference<>();

    private ConnectedHomeIpDiagnostics() {
    }

    public static <T> T withListener(Consumer<String> listener, DiagnosticCallable<T> callable) throws Exception {
        Consumer<String> previous = LISTENER.get();
        Consumer<String> previousActive = ACTIVE_LISTENER.get();
        if (listener == null) {
            LISTENER.remove();
            ACTIVE_LISTENER.set(null);
        } else {
            LISTENER.set(listener);
            ACTIVE_LISTENER.set(listener);
        }
        try {
            return callable.call();
        } finally {
            if (previous == null) {
                LISTENER.remove();
            } else {
                LISTENER.set(previous);
            }
            ACTIVE_LISTENER.set(previousActive);
        }
    }

    public static void emit(String message) {
        Consumer<String> listener = LISTENER.get();
        if (listener == null) {
            listener = ACTIVE_LISTENER.get();
        }
        if (listener != null && message != null && !message.isEmpty()) {
            listener.accept(message);
        }
    }

    public interface DiagnosticCallable<T> {
        T call() throws Exception;
    }
}
