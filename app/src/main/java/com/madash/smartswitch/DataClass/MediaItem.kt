package com.madash.smartswitch.DataClass

import android.net.Uri
import com.madash.smartswitch.Layouts.MediaType

data class MediaItem(
    val id: Long,
    val name: String,
    val uri: Uri,
    val size: String,
    val dateAdded: Long,
    val type: MediaType
)