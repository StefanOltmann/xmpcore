package de.stefan_oltmann.xmp

import kotlin.math.abs

/**
 * A parsed XMP date or date-time value and the parser for the ISO 8601 subset that the XMP
 * specification allows.
 *
 * XMP stores dates as ISO 8601 strings with optional parts: a value may stop after the year,
 * the month or the day, and the time of day and a UTC offset are optional as well. Following
 * Adobe's XMPDateTime, absent calendar parts are reported as `0` ([month] and [day] are
 * 1-based when present) and [utcOffsetMinutes] is null when the value carries no timezone.
 * The timezone policy - like interpreting an offset-less local time in the viewer's zone -
 * stays with the caller, because that is an application decision, not a metadata decision.
 */
public data class XmpDate(

    /**
     * The year.
     */
    public val year: Int,

    /**
     * The month, 1..12, or 0 when the value has no month.
     */
    public val month: Int,

    /**
     * The day of month, 1..31, or 0 when the value has no day.
     */
    public val day: Int,

    /**
     * The hour, 0..23, or 0 when the value has no time.
     */
    public val hour: Int,

    /**
     * The minute, 0..59, or 0 when the value has no time.
     */
    public val minute: Int,

    /**
     * The second, 0..59, or 0 when the value has no seconds.
     */
    public val second: Int,

    /**
     * The fractional second in nanoseconds, 0..999999999.
     */
    public val nanosecond: Int,

    /**
     * The UTC offset in minutes, for example 300 for "+05:00", -300 for "-05:00" and 0 for
     * "Z", or null when the value carries no timezone.
     */
    public val utcOffsetMinutes: Int?
) {

    /**
     * Renders the value back into the ISO 8601 form XMP uses, with absent calendar parts
     * omitted. The time is rendered when any time part is set or when an offset is present,
     * because an offset without a time would be meaningless.
     */
    @Suppress("MagicNumber")
    override fun toString(): String {

        val builder = StringBuilder(32)

        builder.append(year.toString().padStart(4, '0'))

        if (month > 0)
            builder.append('-').append(month.toString().padStart(2, '0'))

        if (day > 0)
            builder.append('-').append(day.toString().padStart(2, '0'))

        val hasTime = listOf(hour, minute, second, nanosecond).any { it != 0 } ||
            utcOffsetMinutes != null

        if (hasTime) {

            builder.append('T')
            builder.append(hour.toString().padStart(2, '0'))
            builder.append(':').append(minute.toString().padStart(2, '0'))
            builder.append(':').append(second.toString().padStart(2, '0'))

            if (nanosecond > 0)
                builder.append('.').append(nanosecond.toString().padStart(9, '0').trimEnd('0'))

            appendUtcOffset(builder, utcOffsetMinutes)
        }

        return builder.toString()
    }

    @Suppress("MagicNumber")
    public companion object {

        private val TIME_SEPARATORS = charArrayOf('T', 't', ' ')

        /**
         * Parses an XMP date value like "2023", "2023-05", "2023-05-12", "2023-05-12T18:04"
         * or "2023-05-12T18:04:07.5-05:00". The time separator may be "T", "t" or a space,
         * and the UTC offset may omit the colon, because broken tools write those variants.
         *
         * @param value The raw XMP property value.
         * @return Returns the parsed date, or null if the value is null or not a well-formed
         * XMP date.
         */
        @kotlin.jvm.JvmStatic
        public fun parse(value: String?): XmpDate? {

            if (value == null)
                return null

            val scanner = Scanner(value.trim())

            val year = scanner.digits(4) ?: return null

            var month = 0
            var day = 0

            if (scanner.consume('-')) {

                month = scanner.digits(2) ?: return null

                /* A written separator promises a value, so "00" is garbage, not absence. */
                if (month == 0)
                    return null

                if (scanner.consume('-')) {

                    day = scanner.digits(2) ?: return null

                    if (day == 0)
                        return null
                }
            }

            var hour = 0
            var minute = 0
            var second = 0
            var nanosecond = 0

            val timeSeparator = scanner.peekOrNull()

            if (timeSeparator != null && timeSeparator in TIME_SEPARATORS) {

                scanner.advance()

                val time = parseTime(scanner) ?: return null

                hour = time[0]
                minute = time[1]
                second = time[2]
                nanosecond = time[3]
            }

            /*
             * Something follows the calendar or time part, so it must be a timezone; a
             * character that does not start one, or a malformed offset like "+05:", fails
             * the parse. An offset may be followed by nothing else.
             */
            var utcOffsetMinutes: Int? = null

            if (!scanner.done) {

                utcOffsetMinutes = parseUtcOffset(scanner) ?: return null

                if (!scanner.done)
                    return null
            }

            if (!isValid(year, month, day, hour, minute, second, nanosecond, utcOffsetMinutes))
                return null

            return XmpDate(year, month, day, hour, minute, second, nanosecond, utcOffsetMinutes)
        }

        /**
         * Parses the time of day behind the separator: "hh:mm" plus optional ":ss" and an
         * optional fraction of a second.
         */
        private fun parseTime(scanner: Scanner): IntArray? {

            val hour = scanner.digits(2) ?: return null

            if (!scanner.consume(':'))
                return null

            val minute = scanner.digits(2) ?: return null

            var second = 0
            var nanosecond = 0

            if (scanner.consume(':')) {

                second = scanner.digits(2) ?: return null

                if (scanner.consume('.') || scanner.consume(','))
                    nanosecond = scanner.fraction() ?: return null
            }

            return intArrayOf(hour, minute, second, nanosecond)
        }

        /**
         * Reads the UTC offset behind the calendar or time part: "Z" or "z" for UTC, a signed
         * offset in "hh", "hh:mm" or "hhmm" form. Returns null when the next character does
         * not start a timezone; whether trailing content is allowed is decided by the caller.
         */
        private fun parseUtcOffset(scanner: Scanner): Int? {

            val current = scanner.peekOrNull() ?: return null

            if (current !in charArrayOf('Z', 'z', '+', '-'))
                return null

            scanner.advance()

            if (current == 'Z' || current == 'z')
                return 0

            val sign = if (current == '+') 1 else -1

            val hours = scanner.digits(2) ?: return null

            val hadColon = scanner.consume(':')

            val minutes = scanner.digits(2)

            if (hadColon && minutes == null)
                return null

            return sign * (hours * 60 + (minutes ?: 0))
        }

        /**
         * Appends the UTC offset in "Z" or "+hh:mm" form.
         */
        private fun appendUtcOffset(builder: StringBuilder, utcOffsetMinutes: Int?) {

            val offset = utcOffsetMinutes ?: return

            if (offset == 0) {

                builder.append('Z')

                return
            }

            val absoluteOffset = abs(offset)

            builder.append(if (offset < 0) '-' else '+')
            builder.append((absoluteOffset / 60).toString().padStart(2, '0'))
            builder.append(':')
            builder.append((absoluteOffset % 60).toString().padStart(2, '0'))
        }

        /**
         * Checks the calendar and clock ranges, including the day against the length of the
         * month with leap year handling.
         */
        private fun isValid(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            second: Int,
            nanosecond: Int,
            utcOffsetMinutes: Int?
        ): Boolean {

            if (month !in 0..12)
                return false

            if (hour !in 0..23 || minute !in 0..59 || second !in 0..59)
                return false

            if (nanosecond !in 0..999_999_999)
                return false

            if (day > 0 && (month == 0 || day > daysInMonth(year, month)))
                return false

            val offset = utcOffsetMinutes ?: return true

            val absoluteOffset = abs(offset)

            return absoluteOffset / 60 <= 23 && absoluteOffset % 60 <= 59
        }

        private fun daysInMonth(year: Int, month: Int): Int = when (month) {

            1, 3, 5, 7, 8, 10, 12 -> 31

            4, 6, 9, 11 -> 30

            else -> if (isLeapYear(year)) 29 else 28
        }

        private fun isLeapYear(year: Int): Boolean =
            year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

        /**
         * Cursor over the value being parsed.
         */
        @Suppress("MagicNumber")
        private class Scanner(private val text: String) {

            private var position = 0

            val done: Boolean
                get() = position >= text.length

            fun consume(expected: Char): Boolean {

                if (position < text.length && text[position] == expected) {

                    position++

                    return true
                }

                return false
            }

            fun peekOrNull(): Char? =
                text.getOrNull(position)

            fun advance() {
                position++
            }

            /**
             * Reads exactly [count] digits, or null if fewer digits are present.
             */
            fun digits(count: Int): Int? {

                if (position + count > text.length)
                    return null

                var result = 0

                for (digitOffset in 0 until count) {

                    val digit = text[position + digitOffset]

                    if (digit !in '0'..'9')
                        return null

                    result = result * 10 + (digit - '0')
                }

                position += count

                return result
            }

            /**
             * Reads a fraction of a second and renders it in nanoseconds, padded or truncated
             * to the nine digits nanoseconds have.
             */
            fun fraction(): Int? {

                val start = position

                while (position < text.length && text[position] in '0'..'9')
                    position++

                val digitCount = position - start

                if (digitCount == 0)
                    return null

                val usedDigits = minOf(digitCount, 9)

                var nanoseconds = 0

                for (digitOffset in 0 until usedDigits)
                    nanoseconds = nanoseconds * 10 + (text[start + digitOffset] - '0')

                repeat(9 - usedDigits) { nanoseconds *= 10 }

                return nanoseconds
            }
        }
    }
}
