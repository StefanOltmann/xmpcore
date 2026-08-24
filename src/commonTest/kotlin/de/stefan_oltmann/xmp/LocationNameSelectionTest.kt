package de.stefan_oltmann.xmp

import kotlin.test.Test
import kotlin.test.assertEquals

/*
 * Regression test for getLocation(): the iterator loop over Iptc4xmpExt:LocationName
 * kept reassigning the name for every item, so the last non-empty localization won
 * instead of the first (x-default) one. getTitle() and getDescription() return the
 * first item, so the selection must be consistent across the convenience getters.
 */
class LocationNameSelectionTest {

    /* language=XML */
    private val multiLanguageLocation =
        """
            <x:xmpmeta xmlns:x="adobe:ns:meta/">
              <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <rdf:Description rdf:about=""
                    xmlns:Iptc4xmpExt="http://iptc.org/std/Iptc4xmpExt/2008-02-29/">
                  <Iptc4xmpExt:LocationShown>
                    <rdf:Bag>
                      <rdf:li>
                        <rdf:Description Iptc4xmpExt:City="New York">
                          <Iptc4xmpExt:LocationName>
                            <rdf:Alt>
                              <rdf:li xml:lang="x-default">Times Square</rdf:li>
                              <rdf:li xml:lang="de-DE">Times Square DE</rdf:li>
                            </rdf:Alt>
                          </Iptc4xmpExt:LocationName>
                        </rdf:Description>
                      </rdf:li>
                    </rdf:Bag>
                  </Iptc4xmpExt:LocationShown>
                </rdf:Description>
              </rdf:RDF>
            </x:xmpmeta>
        """.trimIndent()

    @Test
    fun testGetLocationNameSelection() {

        val xmp = XMPMetaFactory.parseFromString(multiLanguageLocation)

        val location = xmp.getLocation()

        assertEquals("Times Square", location?.name)
    }
}
