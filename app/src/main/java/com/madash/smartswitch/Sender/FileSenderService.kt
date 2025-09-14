package com.madash.smartswitch.Sender

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException

class FileSenderService(private val context: Context) {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    // State flows for UI updates
    private val _transferState = MutableSharedFlow<FileSenderState>()
    val transferState: SharedFlow<FileSenderState> get() = _transferState

    private val _log = MutableSharedFlow<String>()
    val log: SharedFlow<String> get() = _log

    // Transfer components
    private var currentSocket: Socket? = null
    private var senderJob: Job? = null
    private var isTransferring = false

    companion object {
        private const val TAG = "FileSenderService"
        private const val BUFFER_SIZE = 1024 * 1024 // 1MB buffer
        private const val CONNECTION_TIMEOUT = 10000 // 10 seconds
        private const val SOCKET_TIMEOUT = 30000 // 30 seconds
    }

    /**
     * Send files to receiver
     */
    fun sendFiles(
        targetIP: String,
        targetPort: Int,
        fileUris: List<Uri>,
        onProgress: (Int, String) -> Unit = { _, _ -> },
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        // Stop any existing transfer
        stopTransfer()

        if (fileUris.isEmpty()) {
            serviceScope.launch {
                log("No files to send")
                _transferState.emit(FileSenderState.Failed("No files selected"))
                onComplete(false, "No files selected")
            }
            return
        }

        senderJob = serviceScope.launch {
            isTransferring = true
            _transferState.emit(FileSenderState.Connecting)

            try {
                log("Connecting to $targetIP:$targetPort")

                // Create and connect socket
                currentSocket = Socket()
                currentSocket?.soTimeout = SOCKET_TIMEOUT
                currentSocket?.connect(InetSocketAddress(targetIP, targetPort), CONNECTION_TIMEOUT)

                if (currentSocket?.isConnected == true) {
                    log("Connected successfully to receiver")
                    _transferState.emit(FileSenderState.Connected)

                    // Send files one by one
                    var successCount = 0
                    val totalFiles = fileUris.size

                    for ((index, fileUri) in fileUris.withIndex()) {
                        if (!isTransferring) {
                            log("Transfer cancelled by user")
                            break
                        }

                        try {
                            val fileName = getFileName(fileUri)
                            log("Sending file ${index + 1}/$totalFiles: $fileName")

                            _transferState.emit(
                                FileSenderState.Transferring(
                                    fileName = fileName,
                                    fileIndex = index + 1,
                                    totalFiles = totalFiles,
                                    progress = 0
                                )
                            )

                            val success = sendSingleFile(fileUri) { progress ->
                                serviceScope.launch {
                                    _transferState.emit(
                                        FileSenderState.Transferring(
                                            fileName = fileName,
                                            fileIndex = index + 1,
                                            totalFiles = totalFiles,
                                            progress = progress
                                        )
                                    )
                                    onProgress(progress, fileName)
                                }
                            }

                            if (success) {
                                successCount++
                                log("Successfully sent: $fileName")
                            } else {
                                log("Failed to send: $fileName")
                            }

                        } catch (e: Exception) {
                            log("Error sending file ${index + 1}: ${e.message}")
                        }
                    }

                    // Complete transfer
                    if (successCount == totalFiles) {
                        log("All files sent successfully ($successCount/$totalFiles)")
                        _transferState.emit(FileSenderState.Success(successCount, totalFiles))
                        onComplete(true, "All files sent successfully")
                    } else {
                        log("Transfer completed with errors ($successCount/$totalFiles)")
                        _transferState.emit(FileSenderState.PartialSuccess(successCount, totalFiles))
                        onComplete(false, "$successCount out of $totalFiles files sent")
                    }

                } else {
                    throw Exception("Failed to connect to receiver")
                }

            } catch (e: SocketTimeoutException) {
                val error = "Connection timeout - receiver not responding"
                log("Connection timeout: ${e.message}")
                _transferState.emit(FileSenderState.Failed(error))
                onComplete(false, error)
            } catch (e: SocketException) {
                val error = "Network error - check connection"
                log("Socket error: ${e.message}")
                _transferState.emit(FileSenderState.Failed(error))
                onComplete(false, error)
            } catch (e: Exception) {
                val error = "Transfer failed: ${e.message}"
                log("Transfer error: ${e.message}")
                _transferState.emit(FileSenderState.Failed(error))
                onComplete(false, error)
            } finally {
                cleanup()
                isTransferring = false
            }
        }
    }

    /**
     * Send a single file to the receiver
     */
    private suspend fun sendSingleFile(fileUri: Uri, onProgress: (Int) -> Unit): Boolean {
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        var objectOutputStream: ObjectOutputStream? = null

        try {
            // Open file input stream
            inputStream = context.contentResolver.openInputStream(fileUri)
                ?: throw Exception("Cannot open file")

            val fileName = getFileName(fileUri)
            val fileSize = getFileSize(fileUri)

            // Get socket streams
            outputStream = currentSocket?.getOutputStream()
                ?: throw Exception("Socket not connected")

            // Send file metadata first
            objectOutputStream = ObjectOutputStream(outputStream)
            val fileTransferInfo = com.madash.smartswitch.Receiver.FileTransferInfo(
                fileName = fileName,
                fileSize = fileSize,
                fileType = getFileType(fileUri),
                timestamp = System.currentTimeMillis()
            )
            objectOutputStream.writeObject(fileTransferInfo)
            objectOutputStream.flush()

            log("Sent file metadata: $fileName ($fileSize bytes)")

            // Send file data
            val buffer = ByteArray(BUFFER_SIZE)
            var totalSent = 0L
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1 && isTransferring) {
                outputStream.write(buffer, 0, bytesRead)
                outputStream.flush()
                totalSent += bytesRead

                // Calculate and report progress
                val progress = if (fileSize > 0) {
                    ((totalSent * 100) / fileSize).toInt().coerceIn(0, 100)
                } else 0

                onProgress(progress)
                log("Sent $totalSent/$fileSize bytes (${progress}%)")
            }

            if (totalSent == fileSize || fileSize == 0L) {
                log("File sent successfully: $fileName")
                return true
            } else {
                log("File transfer incomplete: $totalSent/$fileSize bytes")
                return false
            }

        } catch (e: Exception) {
            log("Error sending file: ${e.message}")
            return false
        } finally {
            try {
                inputStream?.close()
                objectOutputStream?.close()
            } catch (e: Exception) {
                log("Error closing streams: ${e.message}")
            }
        }
    }

    /**
     * Stop current transfer
     */
    fun stopTransfer() {
        serviceScope.launch { log("Stopping file transfer...") }
        isTransferring = false

        // Cancel sender job
        senderJob?.cancel()
        senderJob = null

        // Close socket
        cleanup()

        serviceScope.launch {
            log("File transfer stopped")
            _transferState.emit(FileSenderState.Stopped)
        }
    }

    /**
     * Clean up resources
     */
    private fun cleanup() {
        try {
            currentSocket?.close()
            currentSocket = null
            serviceScope.launch { log("Socket closed") }
        } catch (e: Exception) {
            serviceScope.launch { log("Error closing socket: ${e.message}") }
        }
    }

    /**
     * Get file name from URI
     */
    private fun getFileName(uri: Uri): String {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        return it.getString(nameIndex) ?: "unknown_file"
                    }
                }
            }
            // Fallback to last path segment
            uri.lastPathSegment ?: "unknown_file"
        } catch (e: Exception) {
            "unknown_file_${System.currentTimeMillis()}"
        }
    }

    /**
     * Get file size from URI
     */
    private fun getFileSize(uri: Uri): Long {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex >= 0) {
                        return it.getLong(sizeIndex)
                    }
                }
            }
            0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Get file type from URI
     */
    private fun getFileType(uri: Uri): String {
        return try {
            context.contentResolver.getType(uri) ?: "application/octet-stream"
        } catch (e: Exception) {
            "application/octet-stream"
        }
    }

    /**
     * Log messages
     */
    private suspend fun log(message: String) {
        Log.d(TAG, message)
        _log.emit("$message\n")
    }

    /**
     * Check if currently transferring
     */
    fun isActive(): Boolean {
        return isTransferring && senderJob?.isActive == true
    }

    /**
     * Force cleanup (for emergency situations)
     */
    fun forceCleanup() {
        try {
            isTransferring = false
            currentSocket?.close()
            currentSocket = null
            senderJob?.cancel()
            senderJob = null
            serviceScope.launch { log("Force cleanup completed") }
        } catch (e: Exception) {
            serviceScope.launch { log("Error during force cleanup: ${e.message}") }
        }
    }
}

/**
 * File transfer states for sender
 */
sealed class FileSenderState {
    object Idle : FileSenderState()
    object Connecting : FileSenderState()
    object Connected : FileSenderState()
    data class Transferring(
        val fileName: String,
        val fileIndex: Int,
        val totalFiles: Int,
        val progress: Int
    ) : FileSenderState()
    data class Success(val filesSent: Int, val totalFiles: Int) : FileSenderState()
    data class PartialSuccess(val filesSent: Int, val totalFiles: Int) : FileSenderState()
    data class Failed(val error: String) : FileSenderState()
    object Stopped : FileSenderState()
}