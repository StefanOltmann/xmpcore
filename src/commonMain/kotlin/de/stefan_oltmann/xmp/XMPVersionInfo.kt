package de.stefan_oltmann.xmp

/**
 * Since v1.0.1 we report the library no longer as "Adobe XMP Core 5.1.3"
 * (which it is based on), but under its real name and version number.
 */
@Suppress("MagicNumber")
public object XMPVersionInfo {

    public const val MAJOR: Int = 1
    public const val MINOR: Int = 7
    public const val PATCH: Int = 1

    public const val VERSION_MESSAGE: String =
        "XMP Core for KMP $MAJOR.$MINOR.$PATCH"

    public const val DEBUG: Boolean = false

}
