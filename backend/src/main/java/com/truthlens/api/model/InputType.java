package com.truthlens.api.model;

/**
 * Categorization of user input payloads for TruthLens Verification Engine.
 * Enables strict gating to prevent non-verifiable inputs (questions, opinions, requests)
 * from executing external searches or generating hallucinated scores.
 */
public enum InputType {
    FACTUAL_CLAIM,
    QUESTION,
    OPINION,
    REQUEST,
    INSTRUCTION,
    PREDICTION,
    GREETING,
    COMMAND,
    PURE_NOISE,
    EMPTY_INPUT,
    IMAGE_WITH_CLAIM,
    IMAGE_WITHOUT_CLAIM,
    URL_ARTICLE
}
