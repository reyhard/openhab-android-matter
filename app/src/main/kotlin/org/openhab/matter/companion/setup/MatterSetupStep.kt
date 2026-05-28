package org.openhab.matter.companion.setup

data class MatterSetupStep(
    val label: String,
    val status: MatterSetupStepStatus,
    val detail: String = ""
)
