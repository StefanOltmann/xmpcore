package de.stefan_oltmann.xmp

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import platform.posix.FILE
import platform.posix.SEEK_END
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.perror
import platform.posix.rewind

@OptIn(UnsafeNumber::class, ExperimentalForeignApi::class)
internal fun readFileAsByteArray(filePath: String): ByteArray? = memScoped {

    /* Note: Mode "rb" is for reading binary files. */
    val file: CPointer<FILE>? = fopen(filePath, "rb")

    if (file == null) {
        perror("Failed to open file: $filePath")
        return null
    }

    /* Move the cursor to the end of the file. */
    fseek(file, 0, SEEK_END)

    /*
     * ftell returns Int on Windows and Long on Unix. Normalize to ULong so
     * the buffer size and the expected read count share one portable type.
     */
    val fileSize = ftell(file).toULong()
    rewind(file)

    val buffer = ByteArray(fileSize.toInt())

    val bytesReadCount: ULong = fread(
        buffer.refTo(0),
        1.toULong(), // Number of items
        fileSize, // Size to read
        file
    )

    fclose(file)

    if (bytesReadCount != fileSize) {
        perror("Did not read file completely: $bytesReadCount != $fileSize")
        return null
    }

    return buffer
}
