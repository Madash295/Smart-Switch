package com.madash.smartswitch.DataClass

import com.madash.smartswitch.Layouts.MediaType

data class FileItem(
    val name: String,
    val path: String,
    val size: String,
    val isDirectory: Boolean,
    val type: MediaType
)