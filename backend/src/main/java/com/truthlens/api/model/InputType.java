package com.truthlens.api.model;

/**
 * Categorization of user input payloads for TruthLens Verification Engine.
 * Enables strict gating to prevent non-verifiable inputs (questions, opinions, fragments, commands)
 * from executing external searches or generating hallucinated scores.
 */
public enum InputType {
    // Standard Specification Taxonomies
    VERIFIABLE_CLAIM,
    NON_VERIFIABLE_QUESTION,
    NON_VERIFIABLE_OPINION,
    NON_VERIFIABLE_COMMAND,
    NON_VERIFIABLE_FRAGMENT,
    NON_VERIFIABLE_META_TEXT,
    OCR_UNRELIABLE,
    NO_VERIFIABLE_CLAIM,
    URL_ARTICLE,
    IMAGE_WITH_CLAIM,
    IMAGE_WITHOUT_CLAIM,
    EMPTY_INPUT,

    // Backward-Compatible Aliases
    FACTUAL_CLAIM,
    QUESTION,
    OPINION,
    REQUEST,
    INSTRUCTION,
    PREDICTION,
    GREETING,
    COMMAND,
    PURE_NOISE
}

