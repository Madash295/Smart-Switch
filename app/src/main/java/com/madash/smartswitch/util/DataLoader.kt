package com.madash.smartswitch.util


import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.ContactsContract
import android.provider.MediaStore
import com.madash.smartswitch.DataClass.AppItem
import com.madash.smartswitch.DataClass.ContactItem
import com.madash.smartswitch.DataClass.FileItem
import com.madash.smartswitch.DataClass.MediaItem
import com.madash.smartswitch.Layouts.MediaType

import java.io.File


public fun loadPhotos(context: Context): List<MediaItem> {
    val photos = mutableListOf<MediaItem>()
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.DATE_ADDED
    )

    val cursor = context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        "${MediaStore.Images.Media.DATE_ADDED} DESC"
    )

    cursor?.use {
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
        val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

        while (it.moveToNext()) {
            val id = it.getLong(idColumn)
            val name = it.getString(nameColumn)
            val size = formatFileSize(it.getLong(sizeColumn))
            val dateAdded = it.getLong(dateColumn)
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

            photos.add(MediaItem(id, name, uri, size, dateAdded, MediaType.PHOTO))
        }
    }

    return photos
}

public fun loadVideos(context: Context): List<MediaItem> {
    val videos = mutableListOf<MediaItem>()
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.DATE_ADDED
    )

    val cursor = context.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        "${MediaStore.Video.Media.DATE_ADDED} DESC"
    )

    cursor?.use {
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val dateColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

        while (it.moveToNext()) {
            val id = it.getLong(idColumn)
            val name = it.getString(nameColumn)
            val size = formatFileSize(it.getLong(sizeColumn))
            val dateAdded = it.getLong(dateColumn)
            val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

            videos.add(MediaItem(id, name, uri, size, dateAdded, MediaType.VIDEO))
        }
    }

    return videos
}

public fun loadMusic(context: Context): List<MediaItem> {
    val music = mutableListOf<MediaItem>()
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.DATE_ADDED
    )

    val cursor = context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        "${MediaStore.Audio.Media.IS_MUSIC} = 1",
        null,
        "${MediaStore.Audio.Media.DATE_ADDED} DESC"
    )

    cursor?.use {
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val nameColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val dateColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

        while (it.moveToNext()) {
            val id = it.getLong(idColumn)
            val name = it.getString(nameColumn)
            val size = formatFileSize(it.getLong(sizeColumn))
            val dateAdded = it.getLong(dateColumn)
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

            music.add(MediaItem(id, name, uri, size, dateAdded, MediaType.MUSIC))
        }
    }

    return music
}

public fun loadContacts(context: Context): List<ContactItem> {
    val contacts = mutableListOf<ContactItem>()
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )

    val cursor = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        projection,
        null,
        null,
        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
    )

    cursor?.use {
        val idColumn = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        val nameColumn = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberColumn = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

        val addedContacts = mutableSetOf<Long>()

        while (it.moveToNext()) {
            val id = it.getLong(idColumn)
            if (!addedContacts.contains(id)) {
                val name = it.getString(nameColumn) ?: "Unknown"
                val number = it.getString(numberColumn)

                contacts.add(ContactItem(id, name, number))
                addedContacts.add(id)
            }
        }
    }

    return contacts
}


@SuppressLint("QueryPermissionsNeeded")
fun loadApps(context: Context): List<AppItem> {
    val apps = mutableListOf<AppItem>()
    val packageManager = context.packageManager

    // Query only apps that have a launcher activity (real apps user can open)
    val intent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val resolvedApps = packageManager.queryIntentActivities(intent, 0)

    for (resolveInfo in resolvedApps) {
        val appInfo = resolveInfo.activityInfo.applicationInfo
        val name = packageManager.getApplicationLabel(appInfo).toString()
        val icon = try {
            packageManager.getApplicationIcon(appInfo)
        } catch (e: Exception) {
            null
        }
        val apkPath = appInfo.sourceDir
        val size = try {
            formatFileSize(File(apkPath).length())
        } catch (e: Exception) {
            "N/A"
        }

        apps.add(AppItem(appInfo.packageName, name, icon, size, apkPath, false))
    }

    return apps.sortedBy { it.name }
}



public fun loadDocuments(context: Context): List<MediaItem> {
    val documents = mutableListOf<MediaItem>()
    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.DATE_ADDED
    )

    val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
            "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
            "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?"
    val selectionArgs = arrayOf("application/pdf", "application/msword", "text/%")

    val cursor = context.contentResolver.query(
        MediaStore.Files.getContentUri("external"),
        projection,
        selection,
        selectionArgs,
        "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
    )

    cursor?.use {
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val nameColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
        val dateColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)

        while (it.moveToNext()) {
            val id = it.getLong(idColumn)
            val name = it.getString(nameColumn) ?: "Unknown"
            val size = formatFileSize(it.getLong(sizeColumn))
            val dateAdded = it.getLong(dateColumn)
            val uri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)

            documents.add(MediaItem(id, name, uri, size, dateAdded, MediaType.DOCUMENT))
        }
    }

    return documents
}

public fun loadArchives(context: Context): List<MediaItem> {
    val archives = mutableListOf<MediaItem>()
    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.DATE_ADDED
    )

    val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
            "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
            "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
    val selectionArgs = arrayOf("application/zip", "application/x-rar-compressed", "%.zip")

    val cursor = context.contentResolver.query(
        MediaStore.Files.getContentUri("external"),
        projection,
        selection,
        selectionArgs,
        "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
    )

    cursor?.use {
        val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val nameColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
        val dateColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)

        while (it.moveToNext()) {
            val id = it.getLong(idColumn)
            val name = it.getString(nameColumn) ?: "Unknown"
            val size = formatFileSize(it.getLong(sizeColumn))
            val dateAdded = it.getLong(dateColumn)
            val uri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)

            archives.add(MediaItem(id, name, uri, size, dateAdded, MediaType.ARCHIVE))
        }
    }

    return archives
}

public fun loadFiles(context: Context): List<FileItem> {
    val files = mutableListOf<FileItem>()

    // Load files from different directories based on Android version
    val directories = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            context.getExternalFilesDir(null)
        )
    } else {
        listOf(
            Environment.getExternalStorageDirectory(),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        )
    }

    directories.filterNotNull().forEach { directory ->
        if (directory.exists() && directory.canRead()) {
            loadFilesFromDirectory(directory, files)
        }
    }

    return files.sortedBy { it.name }
}

private fun loadFilesFromDirectory(directory: File, files: MutableList<FileItem>) {
    try {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                files.add(
                    FileItem(
                        name = file.name,
                        path = file.absolutePath,
                        size = "",
                        isDirectory = true,
                        type = MediaType.FOLDER
                    )
                )
            } else {
                val type = getFileType(file.extension.lowercase())
                files.add(
                    FileItem(
                        name = file.name,
                        path = file.absolutePath,
                        size = formatFileSize(file.length()),
                        isDirectory = false,
                        type = type
                    )
                )
            }
        }
    } catch (e: SecurityException) {
        // Directory not accessible
    }
}

private fun getFileType(extension: String): MediaType {
    return when (extension) {
        "jpg", "jpeg", "png", "gif", "bmp", "webp" -> MediaType.PHOTO
        "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm" -> MediaType.VIDEO
        "mp3", "wav", "flac", "aac", "ogg", "m4a" -> MediaType.MUSIC
        "pdf", "doc", "docx", "txt", "rtf", "odt" -> MediaType.DOCUMENT
        "zip", "rar", "7z", "tar", "gz" -> MediaType.ARCHIVE
        "apk" -> MediaType.APK
        else -> MediaType.FILE
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.1f GB".format(gb)
}
