package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XmpPacketDecoder
import de.stefan_oltmann.xmp.options.SerializeOptions
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import platform.posix.fprintf
import platform.posix.stderr
import kotlin.system.exitProcess

private val xmpSerializeOptionsCompact =
    SerializeOptions()
        .setOmitXmpMetaElement(false)
        .setOmitPacketWrapper(false)
        .setUseCompactFormat(true)
        .setUseCanonicalFormat(false)
        .setSort(true)

/**
 * Writes a line to stderr, keeping stdout reserved for the serialized XMP packet.
 */
@OptIn(ExperimentalForeignApi::class)
private fun printErrorLine(message: String): Unit = memScoped {
    fprintf(stderr, "%s\n", message.cstr.ptr)
}

/**
 * Prints a clean error message to stderr and terminates with a non-zero exit code, so scripts
 * and CI pipelines can detect the failure instead of seeing an unhandled exception trace.
 */
private fun fail(message: String): Nothing {

    printErrorLine("ERROR: $message")

    exitProcess(1)
}

/**
 * Reads the XMP sidecar given as the only argument and prints its reserialized compact form
 * to stdout, so scripts can normalize sidecars through this executable.
 */
public fun main(args: Array<String>) {

    if (args.size != 1) {
        printErrorLine("USAGE: Must be called with one argument: the path to an XMP sidecar file.")
        exitProcess(1)
    }

    val filePath = args.first()

    val bytes = readFileAsByteArray(filePath)

    /* readFileAsByteArray has already printed the reason. */
    if (bytes == null)
        exitProcess(1)

    /*
     * Sidecars may be UTF-8 or UTF-16. Anything else fails with a clear
     * message instead of being silently mangled.
     */
    val xmp = try {
        XmpPacketDecoder.decode(bytes)
    } catch (ex: IllegalArgumentException) {
        fail("Not a valid UTF-8 or UTF-16 encoded XMP packet: $filePath (${ex.message})")
    }

    val newXmp = try {

        val xmpMeta = XMPMetaFactory.parseFromString(xmp)

        XMPMetaFactory.serializeToString(xmpMeta, xmpSerializeOptionsCompact)

    } catch (ex: XMPException) {
        fail("Not a valid XMP packet: $filePath (${ex.message})")
    }

    println(newXmp)
}
