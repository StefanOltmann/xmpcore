package de.stefan_oltmann.xmp

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the data model normalization of [XMPMeta]: DC array forms, alt-text
 * repairs and the instance ID move.
 */
class XMPDataModelNormalizationTest {

    /**
     * A simple dc:subject property is normalized to a bag array.
     */
    @Test
    fun testSimpleDcPropertyNormalizedToArray() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:subject>fox</dc:subject>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(1, xmpMeta.countArrayItems(XMPConst.NS_DC, "subject"))
        assertEquals("fox", checkNotNull(xmpMeta.getArrayItem(XMPConst.NS_DC, "subject", 1)).getValue())
    }

    /**
     * A simple dc:title property is normalized to an alt-text array with an
     * x-default language qualifier.
     */
    @Test
    fun testSimpleDcTitleNormalizedToAltText() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:dc="http://purl.org/dc/elements/1.1/">
                <dc:title>Titel</dc:title>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "Titel",
            actual = checkNotNull(
                xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, XMPConst.X_DEFAULT)
            ).getValue()
        )
    }

    /**
     * An exif:UserComment alt array without language qualifiers is repaired
     * with the x-repair language.
     */
    @Test
    fun testExifUserCommentAltTextRepaired() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:exif="http://ns.adobe.com/exif/1.0/">
                <exif:UserComment>
                  <rdf:Alt>
                    <rdf:li>Ein Kommentar</rdf:li>
                  </rdf:Alt>
                </exif:UserComment>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "Ein Kommentar",
            actual = checkNotNull(
                xmpMeta.getLocalizedText(XMPConst.NS_EXIF, "UserComment", null, "x-repair")
            ).getValue()
        )
    }

    /**
     * The alt-text repair removes empty and composite items.
     */
    @Test
    fun testExifUserCommentRepairRemovesInvalidItems() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:exif="http://ns.adobe.com/exif/1.0/">
                <exif:UserComment>
                  <rdf:Alt>
                    <rdf:li></rdf:li>
                    <rdf:li>
                      <rdf:Bag/>
                    </rdf:li>
                    <rdf:li>Behalten</rdf:li>
                  </rdf:Alt>
                </exif:UserComment>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(1, xmpMeta.countArrayItems(XMPConst.NS_EXIF, "UserComment"))
        assertEquals(
            expected = "Behalten",
            actual = checkNotNull(
                xmpMeta.getLocalizedText(XMPConst.NS_EXIF, "UserComment", null, "x-repair")
            ).getValue()
        )
    }

    /**
     * An xmpRights:UsageTerms alt array without language qualifiers is repaired.
     */
    @Test
    fun testXmpRightsUsageTermsRepaired() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:xmpRights="http://ns.adobe.com/xap/1.0/rights/">
                <xmpRights:UsageTerms>
                  <rdf:Alt>
                    <rdf:li>Nutzungsbedingungen</rdf:li>
                  </rdf:Alt>
                </xmpRights:UsageTerms>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "Nutzungsbedingungen",
            actual = checkNotNull(
                xmpMeta.getLocalizedText(
                    XMPConst.NS_XMP_RIGHTS,
                    "UsageTerms",
                    null,
                    "x-repair"
                )
            ).getValue()
        )
    }

    /**
     * An instance ID in the rdf:about attribute is moved to xmpMM:InstanceID.
     */
    @Test
    fun testInstanceIdMovedFromAbout() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about="uuid:bac965c4-9d87-11d9-9a30-000d936b79c4"/>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals("", xmpMeta.getObjectName())
        assertEquals(
            expected = "uuid:bac965c4-9d87-11d9-9a30-000d936b79c4",
            actual = xmpMeta.getPropertyString(XMPConst.NS_XMP_MM, "InstanceID")
        )
    }

    /**
     * A plain UUID without the uuid: prefix is moved too.
     */
    @Test
    fun testPlainUuidMovedFromAbout() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about="bac965c4-9d87-11d9-9a30-000d936b79c4"/>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "uuid:bac965c4-9d87-11d9-9a30-000d936b79c4",
            actual = xmpMeta.getPropertyString(XMPConst.NS_XMP_MM, "InstanceID")
        )
    }

    /**
     * An existing xmpMM:InstanceID is clobbered by the moved instance ID.
     */
    @Test
    fun testExistingInstanceIdClobbered() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about="uuid:bac965c4-9d87-11d9-9a30-000d936b79c4"
                  xmlns:xmpMM="http://ns.adobe.com/xap/1.0/mm/"
                xmpMM:InstanceID="uuid:old"/>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(
            expected = "uuid:bac965c4-9d87-11d9-9a30-000d936b79c4",
            actual = xmpMeta.getPropertyString(XMPConst.NS_XMP_MM, "InstanceID")
        )
    }

    /**
     * A non-UUID rdf:about stays as object name.
     */
    @Test
    fun testNonUuidAboutStays() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about="http://example.org/"/>
            </rdf:RDF>
        """.trimIndent()

        assertEquals(
            expected = "http://example.org/",
            actual = XMPMetaFactory.parseFromString(testXmp).getObjectName()
        )
    }
}
