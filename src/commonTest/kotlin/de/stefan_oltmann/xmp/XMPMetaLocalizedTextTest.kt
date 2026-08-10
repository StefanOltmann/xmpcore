package de.stefan_oltmann.xmp

import kotlin.test.Test
import kotlin.test.assertEquals

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

        assertEquals("Titel", property!!.getValue())
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
            actual = xmpMeta.getLocalizedText(XMPConst.NS_DC, "title", null, "de")!!.getValue()
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

        assertEquals("Beschreibung", property!!.getValue())
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
            actual = xmpMeta.getLocalizedText(XMPConst.NS_DC, "description", null, "de")!!.getValue()
        )
    }
}
