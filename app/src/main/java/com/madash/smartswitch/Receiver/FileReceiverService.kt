package com.madash.smartswitch.Receiver

import android.content.Context
import kotlinx.coroutines.flow.SharedFlow

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow

import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.ObjectInputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.BindException

class FileReceiverService(private val context: Context) {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    // State flows for UI updates
    private val _transferState = MutableSharedFlow<FileTransferState>()
    val transferState: SharedFlow<FileTransferState> get() = _transferState

    private val _log = MutableSharedFlow<String>()
    val log: SharedFlow<String> get() = _log

    // Server components
    private var serverSocket: ServerSocket? = null
    private var isListening = false
    private var receiverJob: Job? = null
    private var currentPort = DEFAULT_PORT

    companion object {
        private const val TAG = "FileReceiverService"
        private const val DEFAULT_PORT = 8080
        private const val MAX_PORT_ATTEMPTS = 10
        private const val BUFFER_SIZE = 1024 * 1024 // 1MB buffer
        private const val SOCKET_TIMEOUT = 15000 // 15 seconds
    }

    /**
     * Start listening for incoming file transfers with improved port management
     */
    fun startListener(port: Int = DEFAULT_PORT) {
        // Stop any existing listener first
        stopListener()

        val job = receiverJob
        if (job != null && job.isActive) {
            serviceScope.launch { log("File receiver already running") }
            return
        }

        receiverJob = serviceScope.launch {
            _transferState.emit(FileTransferState.Idle)

            var serverSocket: ServerSocket? = null

            try {
                _transferState.emit(FileTransferState.Connecting)

                // Try to find an available port
                var attemptPort = port
                var socketCreated = false
                var attempts = 0

                while (!socketCreated && attempts < MAX_PORT_ATTEMPTS) {
                    try {
                        log("Attempting to start server on port $attemptPort")

                        serverSocket = ServerSocket()
                        serverSocket.reuseAddress = true
                        serverSocket.soTimeout = SOCKET_TIMEOUT
                        serverSocket.bind(InetSocketAddress(attemptPort))

                        socketCreated = true
                        currentPort = attemptPort
                        this@FileReceiverService.serverSocket = serverSocket
                        isListening = true

                        log("Socket server successfully started on port $currentPort")

                    } catch (e: BindException) {
                        log("Port $attemptPort is in use, trying port ${attemptPort + 1}")
                        serverSocket?.close()
                        attemptPort++
                        attempts++

                        if (attempts >= MAX_PORT_ATTEMPTS) {
                            throw Exception("Could not find available port after $MAX_PORT_ATTEMPTS attempts")
                        }
                    }
                }

                log("Socket listening on port $currentPort, waiting for connections...")

                while (isListening) {
                    try {
                        // Accept client connection
                        val client = serverSocket?.accept()
                        if (client != null) {
                            serviceScope.launch { log("Client connected from ${client.remoteSocketAddress}") }
                            // Handle the client in a separate launch to allow multiple connections
                            handleClientConnection(client)
                        }

                    } catch (e: java.net.SocketTimeoutException) {
                        // Timeout is normal, continue listening
                        if (isListening) {
                            log("Waiting for connections...")
                        }
                    } catch (e: java.net.SocketException) {
                        if (isListening && serverSocket?.isClosed == false) {
                            serviceScope.launch { log("Socket error: ${e.message}") }
                            _transferState.emit(FileTransferState.Failed(e))
                        }
                        break
                    } catch (e: Exception) {
                        if (isListening) {
                            serviceScope.launch { log("Error accepting connection: ${e.message}") }
                            _transferState.emit(FileTransferState.Failed(e))
                        }
                        break
                    }
                }

            } catch (e: Exception) {
                log("Server error: ${e.message}")
                _transferState.emit(FileTransferState.Failed(e))
            } finally {
                // Cleanup resources
                cleanup(serverSocket)
            }
        }
    }

    /**
     * Handle individual client connections
     */
    private suspend fun handleClientConnection(client: Socket) {
        var clientInputStream: InputStream? = null
        var objectInputStream: ObjectInputStream? = null
        var fileOutputStream: FileOutputStream? = null

        try {
            _transferState.emit(FileTransferState.Receiving)

            // Handle file transfer
            clientInputStream = client.getInputStream()
            objectInputStream = ObjectInputStream(clientInputStream)

            // Read file metadata
            val fileTransfer = objectInputStream.readObject() as FileTransferInfo
            val file = File(getCacheDir(), fileTransfer.fileName)

            log("Connection successful, receiving file: ${fileTransfer.fileName}")
            log("File will be saved to: ${file.absolutePath}")
            log("File size: ${fileTransfer.fileSize} bytes")
            log("Starting file transfer...")

            fileOutputStream = FileOutputStream(file)
            val buffer = ByteArray(BUFFER_SIZE)
            var totalReceived = 0L

            // Receive file data
            while (true) {
                val length = clientInputStream.read(buffer)
                if (length > 0) {
                    fileOutputStream.write(buffer, 0, length)
                    totalReceived += length

                    // Calculate progress
                    val progress = if (fileTransfer.fileSize > 0) {
                        (totalReceived * 100 / fileTransfer.fileSize).toInt()
                    } else 0

                    log("Receiving file... ${totalReceived} bytes received (${progress}%)")
                    _transferState.emit(FileTransferState.Progress(progress, totalReceived))

                } else {
                    break
                }
            }

            _transferState.emit(FileTransferState.Success(file))
            log("File received successfully: ${file.absolutePath}")

        } catch (e: Exception) {
            if (isListening) {
                log("Error handling client: ${e.message}")
                _transferState.emit(FileTransferState.Failed(e))
            }
        } finally {
            // Close client connections
            try {
                clientInputStream?.close()
                objectInputStream?.close()
                fileOutputStream?.close()
                client.close()
            } catch (e: Exception) {
                log("Error closing client connection: ${e.message}")
            }
        }
    }

    /**
     * Stop the file receiver service with proper cleanup
     */
    fun stopListener() {
        serviceScope.launch { log("Stopping file receiver service...") }
        isListening = false

        // Cancel the receiver job
        receiverJob?.cancel()
        receiverJob = null

        // Close server socket
        val socket = serverSocket
        if (socket != null && !socket.isClosed) {
            try {
                socket.close()
                serviceScope.launch { log("Server socket closed") }
            } catch (e: Exception) {
                serviceScope.launch { log("Error closing server socket: ${e.message}") }
            }
        }

        serverSocket = null

        serviceScope.launch {
            log("File receiver stopped")
            _transferState.emit(FileTransferState.Stopped)
        }
    }

    /**
     * Complete cleanup with proper resource management
     */
    fun cleanup() {
        serviceScope.launch { log("Cleaning up FileReceiverService...") }
        stopListener()

        // Give time for socket to be released
        Thread.sleep(100)

        // Reset state
        currentPort = DEFAULT_PORT
    }

    /**
     * Internal cleanup method
     */
    private fun cleanup(serverSocket: ServerSocket?) {
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error during server socket cleanup: ${e.message}")
        }

        isListening = false
        this.serverSocket = null
    }

    /**
     * Get cache directory for received files
     */
    private fun getCacheDir(): File {
        val cacheDir = File(context.cacheDir, "FileTransfer")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return cacheDir
    }

    /**
     * Log messages
     */
    private suspend fun log(message: String) {
        Log.d(TAG, message)
        _log.emit("$message\n")
    }

   

    /**
     * Check if receiver is currently active
     */
    fun isActive(): Boolean {
        return isListening && receiverJob?.isActive == true
    }

    /**
     * Get current server port
     */
    fun getCurrentPort(): Int {
        return currentPort
    }

    /**
     * Force close any existing socket (for emergency cleanup)
     */
    fun forceCloseSocket() {
        try {
            serverSocket?.close()
            serverSocket = null
            isListening = false
            serviceScope.launch { log("Socket force closed") }
        } catch (e: Exception) {
            serviceScope.launch { log("Error force closing socket: ${e.message}") }
        }
    }
}

/**
 * File transfer states
 */
sealed class FileTransferState {
    object Idle : FileTransferState()
    object Connecting : FileTransferState()
    object Receiving : FileTransferState()
    data class Progress(val percentage: Int, val bytesReceived: Long) : FileTransferState()
    data class Success(val file: File) : FileTransferState()
    data class Failed(val error: Throwable) : FileTransferState()
    object Stopped : FileTransferState()
}

/**
 * File transfer metadata
 */
data class FileTransferInfo(
    val fileName: String,
    val fileSize: Long,
    val fileType: String = "unknown",
    val timestamp: Long = System.currentTimeMillis()
) : java.io.Serializable