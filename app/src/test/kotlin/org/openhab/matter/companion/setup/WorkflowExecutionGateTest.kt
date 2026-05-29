package org.openhab.matter.companion.setup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowExecutionGateTest {
    @Test
    fun duplicateStartIsRejectedWhileWorkflowIsRunning() {
        val gate = WorkflowExecutionGate()

        assertTrue(gate.tryStart())
        assertFalse(gate.tryStart())
    }

    @Test
    fun startIsAllowedAfterWorkflowFinishes() {
        val gate = WorkflowExecutionGate()

        assertTrue(gate.tryStart())
        gate.finish()

        assertTrue(gate.tryStart())
    }

    @Test
    fun closeRejectsFutureStartsAndSuppressesEmissions() {
        val gate = WorkflowExecutionGate()

        gate.close()

        assertFalse(gate.tryStart())
        assertFalse(gate.canEmit())
    }
}
