package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.XmpGps.Companion.DDM_FRACTION_DIGITS
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * A GPS position as decimal degrees, plus the parser and renderer for the DDM form
 * (degrees, decimal minutes) the XMP specification requires for GPS values.
 *
 * In the DDM form a coordinate looks like "53,13.1635N": the integer degrees, a comma, the
 * minutes with up to four fraction digits, and the hemisphere letter. Positive latitudes are
 * north and positive longitudes are east; [parse] accepts the DDM form and the DMS form with
 * seconds, in upper and lower case, and returns null for corrupted values, like minutes of
 * 60 or more or coordinates outside the valid sphere range.
 */
public data class XmpGps(
    /**
     * The latitude in decimal degrees, -90..90, positive north.
     */
    public val latitude: Double,

    /**
     * The longitude in decimal degrees, -180..180, positive east.
     */
    public val longitude: Double
) {

    init {
        require(latitude in -MAX_LATITUDE..MAX_LATITUDE) { "Latitude out of range: $latitude" }

        require(longitude in -MAX_LONGITUDE..MAX_LONGITUDE) { "Longitude out of range: $longitude" }
    }

    /**
     * Renders the latitude as a DDM value for the `exif:GPSLatitude` property, like
     * "53,13.1635N". The minutes carry four fraction digits, and a carry of a full minute
     * rounds over into the degrees, so the rendered value can never decode back outside the
     * valid sphere range.
     */
    public fun toLatitudeDdm(): String = toDdm(latitude, MAX_LATITUDE, "N", "S")

    /**
     * Renders the longitude as a DDM value for the `exif:GPSLongitude` property, like
     * "8,14.3797E".
     */
    public fun toLongitudeDdm(): String = toDdm(longitude, MAX_LONGITUDE, "E", "W")

    @Suppress("MagicNumber")
    public companion object {

        private const val MAX_LATITUDE: Double = 90.0

        private const val MAX_LONGITUDE: Double = 180.0

        private const val MINUTES_PER_DEGREE: Double = 60.0

        private const val SECONDS_PER_MINUTE: Double = 60.0

        private const val SECONDS_PER_DEGREE: Double = 3600.0

        private const val DDM_FRACTION_DIGITS: Int = 4

        private const val DDM_FRACTION_FACTOR: Int = 10_000

        /**
         * The minute fraction digits of a value rounded to [DDM_FRACTION_DIGITS] can reach
         * one full minute of 60.0000, which carries over into the degrees.
         */
        private const val MINUTE_CARRY_THRESHOLD: Int = 60 * DDM_FRACTION_FACTOR

        /**
         * Parses the `exif:GPSLatitude` and `exif:GPSLongitude` property values, which may be
         * in DDM form like "53,13.1635N" or DMS form with seconds like "53,13,8N". Direction
         * letters are accepted in upper and lower case. Coordinates outside the valid sphere
         * range are rejected, because corrupt XMP must not flow into the position.
         *
         * @param latitudeValue The raw latitude property value.
         * @param longitudeValue The raw longitude property value.
         * @return Returns the parsed position, or null if a value is null, unparseable or out
         * of range.
         */
        @kotlin.jvm.JvmStatic
        public fun parse(latitudeValue: String?, longitudeValue: String?): XmpGps? {

            val latitude = parseValue(latitudeValue) ?: return null

            val longitude = parseValue(longitudeValue) ?: return null

            if (latitude !in -MAX_LATITUDE..MAX_LATITUDE ||
                longitude !in -MAX_LONGITUDE..MAX_LONGITUDE
            )
                return null

            return XmpGps(latitude, longitude)
        }

        /**
         * Converts a DMS (degrees, minutes, seconds) or DDM (degrees, decimal minutes) value
         * into decimal degrees. Designed to be robust and to never throw.
         */
        private fun parseValue(dms: String?): Double? {

            /* Blank values are illegal. */
            if (dms.isNullOrBlank())
                return null

            val normalized = dms.uppercase()

            val directionLetter = normalized.last()

            /* A well-formed value ends with a direction letter. */
            if (directionLetter !in "NSEW")
                return null

            val parts = normalized.split(",", "N", "S", "E", "W")

            /* Degrees and minutes are required, only the seconds are optional. */
            if (parts.size < 2)
                return null

            val degrees = parts[0].toDoubleOrNull() ?: return null

            val minutes = parts[1].toDoubleOrNull() ?: return null

            val seconds = if (parts.size >= 3) parts[2].toDoubleOrNull() ?: 0.0 else 0.0

            /*
             * Minutes and seconds of 60 or more are implausible and usually a sign of corrupt
             * data, so they are rejected instead of silently producing an out-of-range
             * coordinate.
             */
            if (minutes >= MINUTES_PER_DEGREE || seconds >= SECONDS_PER_MINUTE)
                return null

            val direction = if (directionLetter == 'S' || directionLetter == 'W') -1 else 1

            return direction * (degrees + minutes / MINUTES_PER_DEGREE + seconds / SECONDS_PER_DEGREE)
        }

        private fun toDdm(
            value: Double,
            maxDegrees: Double,
            positiveDirection: String,
            negativeDirection: String
        ): String {

            val direction = if (value >= 0) positiveDirection else negativeDirection

            /*
             * Clamp before splitting, so an out-of-range input cannot produce an out-of-sphere
             * DDM output: "90,30.0N" would decode back to 90.5, which is outside the valid
             * range.
             */
            val absoluteValue = abs(value).coerceIn(0.0, maxDegrees)

            var degrees = absoluteValue.toInt()

            val minutes = (absoluteValue - degrees) * MINUTES_PER_DEGREE

            /*
             * Rounding happens on the integer count of ten-thousandths of a minute, so the
             * rendering below works on exact tenths and stays identical on every platform.
             */
            val minuteTenThousandths = (minutes * DDM_FRACTION_FACTOR).roundToLong().toInt()

            /*
             * The minutes can round up to one full minute of 60.0000, which is not a valid DDM
             * value. Carry the full minute over to the degrees instead.
             */
            val carriedIntoDegrees = minuteTenThousandths >= MINUTE_CARRY_THRESHOLD

            if (carriedIntoDegrees)
                degrees++

            val renderedMinuteTenThousandths = if (carriedIntoDegrees) 0 else minuteTenThousandths

            return "$degrees,${renderMinutes(renderedMinuteTenThousandths)}$direction"
        }

        /**
         * Renders the minute value given as its count of ten-thousandths with the JVM's double
         * rendering rules: at least one fraction digit, plain notation from 0.001, exponent
         * notation below. Rendering from the integer avoids the platform differences of
         * [Double.toString] - Kotlin/JS and Kotlin/Wasm would produce other bytes - because
         * the value is written into files.
         */
        @Suppress("MagicNumber")
        private fun renderMinutes(minuteTenThousandths: Int): String = when {

            minuteTenThousandths == 0 -> "0.0"

            /* Values below 0.001 render in exponent notation like the JVM does. */
            minuteTenThousandths < 10 -> "$minuteTenThousandths.0E-4"

            else -> {

                val wholeMinutes = minuteTenThousandths / 10_000

                val fractionDigits = (minuteTenThousandths % 10_000)
                    .toString()
                    .padStart(4, '0')
                    .trimEnd('0')

                val fractionText = fractionDigits.ifEmpty { "0" }

                "$wholeMinutes.$fractionText"
            }
        }
    }
}
