package com.madash.smartswitch.util

import android.content.Context
import android.os.Environment
import android.os.StatFs
import kotlin.math.roundToInt

data class StorageInfo(
    val totalGB: Int,
    val usedGB: Int,
    val usedPercentage: Float
)

object StorageUtils {

    fun getStorageInfo(context: Context): StorageInfo {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)

        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalBytes = totalBlocks * blockSize
        val availableBytes = availableBlocks * blockSize
        val usedBytes = totalBytes - availableBytes

        val totalGB = (totalBytes / (1024f * 1024f * 1024f)).roundToInt()
        val usedGB = (usedBytes / (1024f * 1024f * 1024f)).roundToInt()

        val percentageUsed = usedBytes.toFloat() / totalBytes.toFloat()

        return StorageInfo(
            totalGB = totalGB,
            usedGB = usedGB,
            usedPercentage = percentageUsed
        )
    }
}
