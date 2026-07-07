package com.example.models

import java.io.File

data class FileModel(
    val name: String,
    val path: String,
    val isDirectory: Boolean = false,
    val file: File,
    val level: Int = 0
)
