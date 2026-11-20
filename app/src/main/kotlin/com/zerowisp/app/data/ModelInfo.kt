package com.zerowisp.app.data

data class ModelInfo(
    val id: String,
    val displayName: String,
    val description: String,
    val filename: String,
    val url: String,
    val sizeBytes: Long
)

object ModelCatalog {
    val models = listOf(
        ModelInfo(
            id = "tiny-en",
            displayName = "English Fast",
            description = "Tiny English-only model (~32 MB). Best for speed.",
            filename = "ggml-tiny.en-q5_1.bin",
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en-q5_1.bin",
            sizeBytes = 33_000_000L
        ),
        ModelInfo(
            id = "base-en",
            displayName = "English Balanced",
            description = "Base English-only model (~57 MB). Good balance.",
            filename = "ggml-base.en-q5_1.bin",
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.en-q5_1.bin",
            sizeBytes = 57_000_000L
        ),
        ModelInfo(
            id = "base-multi",
            displayName = "Multilingual Fast",
            description = "Base multilingual model (~60 MB). Supports many languages.",
            filename = "ggml-base-q5_1.bin",
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin",
            sizeBytes = 60_000_000L
        ),
        ModelInfo(
            id = "small-multi",
            displayName = "Bangla / Multilingual Pro",
            description = "Small multilingual model (~190 MB). Best for Bangla, Hindi, Arabic.",
            filename = "ggml-small-q5_1.bin",
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small-q5_1.bin",
            sizeBytes = 190_000_000L
        )
    )
}
