package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.options.ParseOptions
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the default-on FIX_CONTROL_CHARS parse option ported from the Adobe original:
 * ASCII control characters that are invalid in XML are replaced with spaces instead of
 * being passed to the XML parser.
 */
class FixControlCharsTest {

    /**
     * A control character inside a value becomes a space, not a question mark or garbage.
     */
    @Suppress("MultilineRawStringIndentation")
    @Test
    fun testControlCharsBecomeSpaces() {

        val testXmp = buildString {

            append(
                """
                    <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                      <rdf:Description rdf:about=""
                          xmlns:dc="http://purl.org/dc/elements/1.1/"
                        dc:title="before
                    """.trimIndent()
            )

            /* An ASCII unit separator (0x1F), a frequent defect of broken writers. */
            append('\u001F')

            append(
                """
                    after"/>
                    </rdf:RDF>
                """.trimIndent()
            )
        }

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals("before after", xmpMeta.getTitle())
    }

    /**
     * The option is on by default, like the Adobe original.
     */
    @Test
    fun testFixControlCharsIsOnByDefault() {

        assertEquals(true, ParseOptions().getFixControlChars())
    }

    /**
     * A numeric character reference that resolves to a control character is repaired like
     * the FixASCIIControlsReader retry pass of the Adobe original: the reference becomes a
     * space, so the invalid character can never reach the data model. dc:source is used
     * because dc:title is normalized into an alt-text array during parsing.
     */
    @Suppress("MultilineRawStringIndentation")
    @Test
    fun testEntityEncodedControlCharIsRepaired() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:source>a&#x1;b&#2;c</dc:source>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals("a b c", xmpMeta.getPropertyString(XMPConst.NS_DC, "source"))
    }

    /**
     * Character references that resolve to legal characters are kept: text references
     * survive unchanged and the tab reference stays the preserved whitespace character.
     */
    @Suppress("MultilineRawStringIndentation")
    @Test
    fun testLegalCharacterReferencesAreUntouched() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:source>A&#65;B&#x9;C</dc:source>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals("AAB\tC", xmpMeta.getPropertyString(XMPConst.NS_DC, "source"))
    }
}
