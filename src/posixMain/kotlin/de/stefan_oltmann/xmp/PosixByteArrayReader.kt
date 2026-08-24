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

    /*
     * Move the cursor to the end of the file and determine its size.
     * ftell returns Int on Windows and Long on Unix. A negative result
     * signals that the size could not be determined, which happens when
     * the file is too large for the return type (e.g. over 2 GiB on Windows).
     */
    fseek(file, 0, SEEK_END)

    val rawFileSize = ftell(file)

    if (rawFileSize < 0) {
        fclose(file)
        perror("Could not determine file size, it may be too large: $filePath")
        return null
    }

    val fileSize = rawFileSize.toULong()

    rewind(file)

    /*
     * A file that does not fit into one ByteArray would truncate its size
     * below and crash with a negative array size. Fail cleanly instead.
     */
    if (fileSize > Int.MAX_VALUE.toULong()) {
        fclose(file)
        perror("File is too large to read into memory: $fileSize bytes")
        return null
    }

    val buffer = ByteArray(fileSize.toInt())

    val bytesReadCount: ULong = fread(
        /* Destination for the raw file content */
        buffer.refTo(0),
        /* Single-byte items, so the returned count equals the byte count */
        1.toULong(),
        /* Number of items to read */
        fileSize,
        file
    )

    fclose(file)

    if (bytesReadCount != fileSize) {
        perror("Did not read file completely: $bytesReadCount != $fileSize")
        return null
    }

    return buffer
}
