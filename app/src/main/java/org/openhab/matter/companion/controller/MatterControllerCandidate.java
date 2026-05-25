package org.openhab.matter.companion.controller;

public interface MatterControllerCandidate extends MatterController {
    ChipMatterControllerStatus readiness();
}
