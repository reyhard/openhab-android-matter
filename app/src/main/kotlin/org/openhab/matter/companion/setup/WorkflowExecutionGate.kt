package org.openhab.matter.companion.setup

import java.util.concurrent.atomic.AtomicBoolean

class WorkflowExecutionGate {
    private val running = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)

    fun tryStart(): Boolean {
        if (closed.get()) {
            return false
        }
        return running.compareAndSet(false, true)
    }

    fun finish() {
        running.set(false)
    }

    fun close() {
        closed.set(true)
        running.set(false)
    }

    fun canEmit(): Boolean {
        return !closed.get()
    }
}
