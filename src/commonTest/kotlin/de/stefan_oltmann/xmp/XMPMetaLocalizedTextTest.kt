package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import de.stefan_oltmann.xmp.options.PropertyOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the interaction of the title/description convenience methods with the
 * localized text services.
 */
class XMPMetaLocalizedTextTest {

    /**
     * The title array created by [XMPMeta.setTitle] is readable via [XMPMeta.getLocalizedText].
     */
    @Test
    fun testGetLocalizedTextAfterSetTitle() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setTitle("Titel")

        val property = xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, XMPConst.X_DEFAULT)

        assertEquals("Titel", checkNotNull(property).getValue())
    }

    /**
     * The title array created by [XMPMeta.setTitle] accepts additional languages via
     * [XMPMeta.setLocalizedText].
     */
    @Test
    fun testSetLocalizedTextAfterSetTitle() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setTitle("Titel")
        xmpMeta.setLocalizedText(XMPConst.NS_DC, "title", null, "de", "Deutscher Titel")

        assertEquals(
            expected = "Deutscher Titel",
            actual = checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "de")).getValue()
        )
    }

    /**
     * The description array created by [XMPMeta.setDescription] is readable via
     * [XMPMeta.getLocalizedText].
     */
    @Test
    fun testGetLocalizedTextAfterSetDescription() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setDescription("Beschreibung")

        val property = xmpMeta.getLocalizedText(XMPConst.NS_DC, "description", null, XMPConst.X_DEFAULT)

        assertEquals("Beschreibung", checkNotNull(property).getValue())
    }

    /**
     * The description array created by [XMPMeta.setDescription] accepts additional languages via
     * [XMPMeta.setLocalizedText].
     */
    @Test
    fun testSetLocalizedTextAfterSetDescription() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setDescription("Beschreibung")
        xmpMeta.setLocalizedText(XMPConst.NS_DC, "description", null, "de", "Deutsche Beschreibung")

        assertEquals(
            expected = "Deutsche Beschreibung",
            actual = checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "description", null, "de")).getValue()
        )
    }

    /**
     * Setting localized text on an empty array creates an x-default item
     * followed by the specific language item with the same value.
     */
    @Test
    fun testSetLocalizedTextOnEmptyArrayCreatesDefaultAndSpecific() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setLocalizedText(XMPConst.NS_DC, "title", null, "de", "Der Titel")

        assertEquals(2, xmpMeta.countArrayItems(XMPConst.NS_DC, "title"))
        assertEquals(
            expected = "Der Titel",
            actual = checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "x-default")).getValue()
        )
        assertEquals(
            expected = "Der Titel",
            actual = checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "de")).getValue()
        )
    }

    /**
     * A generic language matches a specific item via partial match.
     */
    @Test
    fun testGetLocalizedTextGenericLanguagePartialMatch() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>
                  <rdf:Alt>
                    <rdf:li xml:lang="de-DE">Deutschland</rdf:li>
                  </rdf:Alt>
                </dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "Deutschland",
            actual = checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", "de", "de-CH")).getValue()
        )
    }

    /**
     * Setting a specific item updates the x-default item when both had the
     * same value.
     */
    @Test
    fun testSetLocalizedTextUpdatesXDefaultOnMatchingValue() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">Same</rdf:li>
                    <rdf:li xml:lang="de">Same</rdf:li>
                  </rdf:Alt>
                </dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        xmpMeta.setLocalizedText(XMPConst.NS_DC, "title", null, "de", "Neu")

        assertEquals("Neu", checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "de")).getValue())
        assertEquals(
            "Neu",
            checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "x-default")).getValue()
        )
    }

    /**
     * Setting a specific item keeps the x-default item when the values differ.
     */
    @Test
    fun testSetLocalizedTextKeepsDifferentXDefault() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">Default</rdf:li>
                    <rdf:li xml:lang="de">Deutsch</rdf:li>
                  </rdf:Alt>
                </dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        xmpMeta.setLocalizedText(XMPConst.NS_DC, "title", null, "de", "Neu")

        assertEquals("Neu", checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "de")).getValue())
        assertEquals(
            "Default",
            checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "x-default")).getValue()
        )
    }

    /**
     * Setting the x-default language updates all items with the same value.
     */
    @Test
    fun testSetLocalizedTextSpecificXDefaultUpdatesAllMatchingItems() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">Same</rdf:li>
                    <rdf:li xml:lang="de">Same</rdf:li>
                    <rdf:li xml:lang="fr">Same</rdf:li>
                  </rdf:Alt>
                </dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        xmpMeta.setLocalizedText(XMPConst.NS_DC, "title", null, XMPConst.X_DEFAULT, "Neu")

        assertEquals("Neu", checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "de")).getValue())
        assertEquals("Neu", checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "fr")).getValue())
        assertEquals(
            "Neu",
            checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "x-default")).getValue()
        )
    }

    /**
     * Setting a specific language on an x-default only array updates the
     * x-default item and appends the specific item.
     */
    @Test
    fun testSetLocalizedTextOnXDefaultOnlyArray() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">Alt</rdf:li>
                  </rdf:Alt>
                </dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        xmpMeta.setLocalizedText(XMPConst.NS_DC, "title", null, "de", "Neu")

        assertEquals(2, xmpMeta.countArrayItems(XMPConst.NS_DC, "title"))
        assertEquals("Neu", checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "de")).getValue())
        assertEquals(
            "Neu",
            checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "x-default")).getValue()
        )
    }

    /**
     * Multiple generic matches append a new specific language item.
     */
    @Test
    fun testSetLocalizedTextWithMultipleGenericMatchesAppends() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>
                  <rdf:Alt>
                    <rdf:li xml:lang="de-DE">Deutschland</rdf:li>
                    <rdf:li xml:lang="de-CH">Schweiz</rdf:li>
                  </rdf:Alt>
                </dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        xmpMeta.setLocalizedText(XMPConst.NS_DC, "title", "de", "de-AT", "Oesterreich")

        assertEquals(3, xmpMeta.countArrayItems(XMPConst.NS_DC, "title"))
        assertEquals(
            "Deutschland",
            checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "de-DE")).getValue()
        )
        assertEquals(
            "Oesterreich",
            checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "de-AT")).getValue()
        )
    }

    /**
     * Without any match a new specific language item is appended.
     */
    @Test
    fun testSetLocalizedTextWithoutMatchAppends() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>
                  <rdf:Alt>
                    <rdf:li xml:lang="en">England</rdf:li>
                    <rdf:li xml:lang="fr">Frankreich</rdf:li>
                  </rdf:Alt>
                </dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        xmpMeta.setLocalizedText(XMPConst.NS_DC, "title", null, "de", "Deutschland")

        assertEquals(3, xmpMeta.countArrayItems(XMPConst.NS_DC, "title"))
        assertEquals(
            "Deutschland",
            checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "de")).getValue()
        )
    }

    /**
     * Setting localized text on an array that is no alt-text array is rejected.
     */
    @Test
    fun testSetLocalizedTextOnNonAltTextArrayThrows() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.appendArrayItem(
            XMPConst.NS_DC,
            "subject",
            PropertyOptions().setArrayOrdered(true),
            "fox"
        )

        val ex = assertFailsWith<XMPException> {
            xmpMeta.setLocalizedText(XMPConst.NS_DC, "subject", null, "de", "Fuchs")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * Getting localized text on a missing property returns null.
     */
    @Test
    fun testGetLocalizedTextOnMissingPropertyReturnsNull() {

        val xmpMeta = XMPMetaFactory.create()

        assertNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "de"))
    }

    /**
     * An empty specific language is rejected.
     */
    @Test
    fun testEmptySpecificLanguageThrows() {

        val xmpMeta = XMPMetaFactory.create()

        assertFailsWith<XMPException> {
            xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }

        assertFailsWith<XMPException> {
            xmpMeta.setLocalizedText(XMPConst.NS_DC, "title", null, "", "value")
        }.let { assertEquals(XMPErrorConst.BADPARAM, it.errorCode) }
    }

    /**
     * Language tags are normalized when stored.
     */
    @Test
    fun testSetLocalizedTextNormalizesLanguageTag() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setLocalizedText(XMPConst.NS_DC, "title", null, "DE-de", "Titel")

        assertEquals(
            expected = "Titel",
            actual = checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "de-DE")).getValue()
        )
        assertEquals(
            expected = "Titel",
            actual = checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "DE-de")).getValue()
        )
    }

    /**
     * An item whose first qualifier is no language qualifier is rejected.
     */
    @Test
    fun testSetLocalizedTextOnItemWithoutLangQualifierThrows() {

        val qualifierNamespace = "http://example.org/xmpcore-locale/"

        XMPSchemaRegistry.registerNamespace(qualifierNamespace, "locale")

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setTitle("Titel")

        /* Remove the language qualifier, then add a different first qualifier. */
        xmpMeta.deleteQualifier(XMPConst.NS_DC, "title[1]", XMPConst.NS_XML, "lang")
        xmpMeta.setQualifier(XMPConst.NS_DC, "title[1]", qualifierNamespace, "custom", "v")

        val ex = assertFailsWith<XMPException> {
            xmpMeta.setLocalizedText(XMPConst.NS_DC, "title", null, "de", "Titel")
        }

        assertEquals(XMPErrorConst.BADXPATH, ex.errorCode)
    }

    /**
     * Parsing an alt array with x-default not first moves it to the front and
     * copies its value to the displaced item.
     */
    @Test
    fun testXDefaultItemMovedToFrontOnParse() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>
                  <rdf:Alt>
                    <rdf:li xml:lang="de">Deutsch</rdf:li>
                    <rdf:li xml:lang="x-default">Default</rdf:li>
                  </rdf:Alt>
                </dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        val iterator = xmpMeta.iterator(XMPConst.NS_DC, "title", null)

        val values = mutableListOf<String>()

        while (iterator.hasNext())
            values.add(iterator.next().getValue())

        assertEquals(listOf("", "Default", "x-default", "Default", "de"), values)
        assertTrue(xmpMeta.getTitle() == "Default")
    }

    /**
     * The property returned by [XMPMeta.getLocalizedText] exposes value,
     * options, language and string form.
     */
    @Test
    fun testLocalizedTextPropertyDetails() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setTitle("Titel")

        val property = checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, XMPConst.X_DEFAULT))

        assertEquals("Titel", property.getValue())
        assertEquals("x-default", property.getLanguage())
        assertTrue(property.getOptions().hasLanguage())
        assertEquals("Titel", property.toString())
    }

    /**
     * [XMPMeta.getLocalizedText] on an empty alt-text array returns null.
     */
    @Test
    fun testGetLocalizedTextOnEmptyAltArrayReturnsNull() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>
                  <rdf:Alt/>
                </dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "de"))
    }

    /**
     * An alternate array without children is promoted to an alt-text array.
     */
    @Test
    fun testSetLocalizedTextOnAlternateArrayWithoutChildren() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setProperty(
            XMPConst.NS_DC,
            "title",
            null,
            PropertyOptions().setArrayAlternate(true)
        )

        xmpMeta.setLocalizedText(XMPConst.NS_DC, "title", null, "de", "Titel")

        assertEquals(
            expected = "Titel",
            actual = checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "de")).getValue()
        )
    }

    /**
     * A generic language match updates the matched item and the x-default item
     * when both had the same value.
     */
    @Test
    fun testSetLocalizedTextGenericMatchUpdatesXDefault() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>
                  <rdf:Alt>
                    <rdf:li xml:lang="x-default">Same</rdf:li>
                    <rdf:li xml:lang="de-DE">Same</rdf:li>
                  </rdf:Alt>
                </dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        xmpMeta.setLocalizedText(XMPConst.NS_DC, "title", "de", "de-CH", "Neu")

        assertEquals("Neu", checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "de-DE")).getValue())
        assertEquals(
            "Neu",
            checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "x-default")).getValue()
        )
    }

    /**
     * Setting an item in a single-item array without x-default appends the
     * x-default item.
     */
    @Test
    fun testSetLocalizedTextAppendsXDefaultForSingleItemArray() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>
                  <rdf:Alt>
                    <rdf:li xml:lang="de">Alt</rdf:li>
                  </rdf:Alt>
                </dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        xmpMeta.setLocalizedText(XMPConst.NS_DC, "title", null, "de", "Neu")

        assertEquals(2, xmpMeta.countArrayItems(XMPConst.NS_DC, "title"))
        assertEquals("Neu", checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "de")).getValue())
        assertEquals(
            "Neu",
            checkNotNull(xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "x-default")).getValue()
        )
    }
}
