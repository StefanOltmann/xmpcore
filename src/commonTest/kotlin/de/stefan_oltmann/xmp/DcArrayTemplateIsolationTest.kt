package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.options.PropertyOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/*
 * Regression test for the DC array form templates in XMPNormalizer: normalization
 * handed the shared template instance to the created array node, so a client could
 * mutate the options returned by getProperty() and thereby corrupt the process-global
 * templates. Every subsequent parse would then normalize dc:title and friends wrongly.
 */
class DcArrayTemplateIsolationTest {

    /* language=XML */
    private val simpleTitleXmp =
        """
            <x:xmpmeta xmlns:x="adobe:ns:meta/">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about=""
                    xmlns:dc="http://purl.org/dc/elements/1.1/">
                  <dc:title>Simple Title</dc:title>
                </rdf:Description>
              </rdf:RDF>
            </x:xmpmeta>
        """.trimIndent()

    @Test
    fun testMutatingReturnedOptionsDoesNotCorruptNormalization() {

        /*
         * A simple dc:title is normalized to an alt-text array whose options came
         * from the shared template.
         */
        val first = XMPMetaFactory.parseFromString(simpleTitleXmp)

        val firstOptions = assertNotNull(first.getProperty(XMPConst.NS_DC, "title")).getOptions()

        assertTrue(firstOptions.isArrayAltText())

        /* Act: mutate the live options object exposed to the client. */
        firstOptions.setArrayAltText(false)

        /* Assert: a fresh parse must still normalize to an alt-text array. */
        val second = XMPMetaFactory.parseFromString(simpleTitleXmp)

        val secondOptions = assertNotNull(second.getProperty(XMPConst.NS_DC, "title")).getOptions()

        assertTrue(secondOptions.isArrayAltText())
        assertFalse(secondOptions.isSimple())
    }

    @Test
    fun testTemplateCopyCarriesCompleteArrayForm() {

        val xmp = XMPMetaFactory.parseFromString(simpleTitleXmp)

        val property = assertNotNull(xmp.getProperty(XMPConst.NS_DC, "title"))

        /* The copy must contain the full AltText promotion chain, not just single bits. */
        val expected = PropertyOptions()
            .setArray(true)
            .setArrayOrdered(true)
            .setArrayAlternate(true)
            .setArrayAltText(true)

        assertEquals(
            expected = expected.getOptions(),
            actual = property.getOptions().getOptions()
        )
    }
}
