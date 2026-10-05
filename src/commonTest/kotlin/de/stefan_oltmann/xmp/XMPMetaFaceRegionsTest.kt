package de.stefan_oltmann.xmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the face region convenience methods of [XMPMeta], including the preservation of
 * regions of other types and of foreign region fields during a face rewrite.
 */
class XMPMetaFaceRegionsTest {

    /**
     * An empty region list reads back as an empty face region list.
     */
    @Test
    fun testGetFaceRegionsWithEmptyRegionList() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:mwg-rs="http://www.metadataworkinggroup.com/schemas/regions/">
                <mwg-rs:Regions rdf:parseType="Resource">
                  <mwg-rs:RegionList>
                    <rdf:Bag/>
                  </mwg-rs:RegionList>
                </mwg-rs:Regions>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(emptyList(), xmpMeta.getFaceRegions())
    }

    /**
     * The face regions are stored as mwg-rs regions and read back in order.
     */
    @Test
    fun testFaceRegionsRoundTrip() {

        val xmpMeta = XMPMetaFactory.create()

        assertEquals(emptyList(), xmpMeta.getFaceRegions())

        val regions = listOf(
            XmpFaceRegion("Face A", XMPRegionArea(0.1, 0.2, 0.3, 0.4)),
            XmpFaceRegion("Face B", XMPRegionArea(0.5, 0.6, 0.7, 0.8))
        )

        xmpMeta.setFaceRegions(regions, widthPx = 1500, heightPx = 1000)

        assertEquals(regions, xmpMeta.getFaceRegions())
    }

    /**
     * Area values below the plain-notation range must serialize with the JVM's
     * double spelling on every platform. Plain Double.toString writes "5.0E-4"
     * on the JVM but "0.0005" on JS and Wasm, so identical metadata produced
     * byte-divergent files depending on the platform that ran the write.
     */
    @Test
    fun testFaceAreaBelowPlainNotationRangeSerializesJvmStyle() {

        val xmpMeta = XMPMetaFactory.create()

        val regions = listOf(
            XmpFaceRegion("Face A", XMPRegionArea(5.0E-4, 0.2, 0.3, 0.4))
        )

        xmpMeta.setFaceRegions(regions, widthPx = 1500, heightPx = 1000)

        assertEquals(
            "5.0E-4",
            xmpMeta.getPropertyString(
                XMPConst.NS_MWG_RS,
                "Regions/mwg-rs:RegionList[1]/mwg-rs:Area/stArea:x"
            )
        )
    }

    /**
     * Two regions with the same name are both kept, because the region list is an array
     * and not keyed by name.
     */
    @Test
    fun testFaceRegionsRoundTripWithDuplicateNames() {

        val xmpMeta = XMPMetaFactory.create()

        val regions = listOf(
            XmpFaceRegion("Maria", XMPRegionArea(0.1, 0.2, 0.3, 0.4)),
            XmpFaceRegion("Maria", XMPRegionArea(0.5, 0.6, 0.7, 0.8))
        )

        xmpMeta.setFaceRegions(regions, widthPx = 1500, heightPx = 1000)

        assertEquals(regions, xmpMeta.getFaceRegions())
    }

    /**
     * A region without a name survives the round-trip instead of being dropped.
     */
    @Test
    fun testFaceRegionsKeepNamelessRegions() {

        val xmpMeta = XMPMetaFactory.create()

        val regions = listOf(XmpFaceRegion(null, XMPRegionArea(0.1, 0.2, 0.3, 0.4)))

        xmpMeta.setFaceRegions(regions, widthPx = 1500, heightPx = 1000)

        assertEquals(regions, xmpMeta.getFaceRegions())
    }

    /**
     * Setting an empty face region list deletes the regions.
     */
    @Test
    fun testSetEmptyFaceRegionsDeletesRegions() {

        val xmpMeta = XMPMetaFactory.create()

        xmpMeta.setFaceRegions(
            listOf(XmpFaceRegion("Face A", XMPRegionArea(0.1, 0.2, 0.3, 0.4))),
            1500,
            1000
        )

        xmpMeta.setFaceRegions(emptyList(), 1500, 1000)

        assertEquals(emptyList(), xmpMeta.getFaceRegions())
    }

    /**
     * Regions that are no faces are skipped when reading.
     */
    @Test
    fun testGetFaceRegionsSkipsNonFaceRegions() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:mwg-rs="http://www.metadataworkinggroup.com/schemas/regions/"
                  xmlns:stArea="http://ns.adobe.com/xmp/sType/Area#">
                <mwg-rs:Regions rdf:parseType="Resource">
                  <mwg-rs:RegionList>
                    <rdf:Bag>
                      <rdf:li>
                        <rdf:Description
                          mwg-rs:Name="Doggy"
                          mwg-rs:Type="Pet">
                        <mwg-rs:Area
                          stArea:h="0.05"
                          stArea:unit="normalized"
                          stArea:w="0.03"
                          stArea:x="0.2"
                          stArea:y="0.3"/>
                        </rdf:Description>
                      </rdf:li>
                      <rdf:li>
                        <rdf:Description
                          mwg-rs:Name="NoArea"
                          mwg-rs:Type="Face"/>
                      </rdf:li>
                    </rdf:Bag>
                  </mwg-rs:RegionList>
                </mwg-rs:Regions>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        assertEquals(emptyList(), xmpMeta.getFaceRegions())
    }

    /**
     * A face rewrite keeps regions of other types, like pets, instead of deleting them.
     */
    @Test
    fun testFaceRewritePreservesNonFaceRegions() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:mwg-rs="http://www.metadataworkinggroup.com/schemas/regions/"
                  xmlns:stArea="http://ns.adobe.com/xmp/sType/Area#">
                <mwg-rs:Regions rdf:parseType="Resource">
                  <mwg-rs:AppliedToDimensions>
                    <rdf:Description stArea:w="1500" stArea:h="1000" stArea:unit="pixel"/>
                  </mwg-rs:AppliedToDimensions>
                  <mwg-rs:RegionList>
                    <rdf:Bag>
                      <rdf:li>
                        <rdf:Description
                          mwg-rs:Name="Doggy"
                          mwg-rs:Type="Pet">
                          <mwg-rs:Area
                            stArea:h="0.05"
                            stArea:unit="normalized"
                            stArea:w="0.03"
                            stArea:x="0.2"
                            stArea:y="0.3"/>
                        </rdf:Description>
                      </rdf:li>
                      <rdf:li>
                        <rdf:Description
                          mwg-rs:Name="Anna"
                          mwg-rs:Type="Face">
                          <mwg-rs:Area
                            stArea:h="0.4"
                            stArea:unit="normalized"
                            stArea:w="0.3"
                            stArea:x="0.1"
                            stArea:y="0.2"/>
                        </rdf:Description>
                      </rdf:li>
                    </rdf:Bag>
                  </mwg-rs:RegionList>
                </mwg-rs:Regions>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        xmpMeta.setFaceRegions(
            listOf(XmpFaceRegion("Berta", XMPRegionArea(0.9, 0.8, 0.3, 0.4))),
            1500,
            1000
        )

        assertEquals(
            expected = listOf(XmpFaceRegion("Berta", XMPRegionArea(0.9, 0.8, 0.3, 0.4))),
            actual = xmpMeta.getFaceRegions()
        )

        val serialized = XMPMetaFactory.serializeToString(xmpMeta)

        assertTrue(serialized.contains("Doggy"))
    }

    /**
     * A face rewrite carries unknown fields of a recognized region, like the vendor
     * extensions other tools store in mwg-rs:Extensions.
     */
    @Test
    fun testFaceRewritePreservesForeignFieldsOfRecognizedFace() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:mwg-rs="http://www.metadataworkinggroup.com/schemas/regions/"
                  xmlns:stArea="http://ns.adobe.com/xmp/sType/Area#">
                <mwg-rs:Regions rdf:parseType="Resource">
                  <mwg-rs:AppliedToDimensions>
                    <rdf:Description stArea:w="1500" stArea:h="1000" stArea:unit="pixel"/>
                  </mwg-rs:AppliedToDimensions>
                  <mwg-rs:RegionList>
                    <rdf:Bag>
                      <rdf:li>
                        <rdf:Description
                          mwg-rs:Name="Anna"
                          mwg-rs:Type="Face">
                          <mwg-rs:Area
                            stArea:h="0.4"
                            stArea:unit="normalized"
                            stArea:w="0.3"
                            stArea:x="0.1"
                            stArea:y="0.2"/>
                          <mwg-rs:Extensions vendor:FaceId="42"
                              xmlns:vendor="https://example.org/xmp-vendor/1.0/"/>
                        </rdf:Description>
                      </rdf:li>
                    </rdf:Bag>
                  </mwg-rs:RegionList>
                </mwg-rs:Regions>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        xmpMeta.setFaceRegions(
            listOf(XmpFaceRegion("Anna", XMPRegionArea(0.9, 0.8, 0.3, 0.4))),
            1500,
            1000
        )

        val serialized = XMPMetaFactory.serializeToString(xmpMeta)

        assertTrue(serialized.contains("FaceId"))
    }

    /**
     * When a name is ambiguous, the foreign fields of the old regions are not carried
     * over, because the regions cannot be told apart.
     */
    @Test
    fun testFaceRewriteDropsForeignFieldsWhenNameAmbiguous() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:mwg-rs="http://www.metadataworkinggroup.com/schemas/regions/"
                  xmlns:stArea="http://ns.adobe.com/xmp/sType/Area#">
                <mwg-rs:Regions rdf:parseType="Resource">
                  <mwg-rs:AppliedToDimensions>
                    <rdf:Description stArea:w="1500" stArea:h="1000" stArea:unit="pixel"/>
                  </mwg-rs:AppliedToDimensions>
                  <mwg-rs:RegionList>
                    <rdf:Bag>
                      <rdf:li>
                        <rdf:Description
                          mwg-rs:Name="Maria"
                          mwg-rs:Type="Face">
                          <mwg-rs:Area
                            stArea:h="0.4"
                            stArea:unit="normalized"
                            stArea:w="0.3"
                            stArea:x="0.1"
                            stArea:y="0.2"/>
                          <mwg-rs:Extensions vendor:FaceId="42"
                              xmlns:vendor="https://example.org/xmp-vendor/1.0/"/>
                        </rdf:Description>
                      </rdf:li>
                      <rdf:li>
                        <rdf:Description
                          mwg-rs:Name="Maria"
                          mwg-rs:Type="Face">
                          <mwg-rs:Area
                            stArea:h="0.5"
                            stArea:unit="normalized"
                            stArea:w="0.3"
                            stArea:x="0.6"
                            stArea:y="0.7"/>
                        </rdf:Description>
                      </rdf:li>
                    </rdf:Bag>
                  </mwg-rs:RegionList>
                </mwg-rs:Regions>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        xmpMeta.setFaceRegions(
            listOf(
                XmpFaceRegion("Maria", XMPRegionArea(0.11, 0.21, 0.3, 0.4)),
                XmpFaceRegion("Maria", XMPRegionArea(0.51, 0.61, 0.7, 0.8))
            ),
            1500,
            1000
        )

        val serialized = XMPMetaFactory.serializeToString(xmpMeta)

        assertTrue(!serialized.contains("FaceId"))
    }

    /**
     * The foreign fields of a removed face are deleted together with the face.
     */
    @Test
    fun testFaceRewriteDropsForeignFieldsOfRemovedFace() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:mwg-rs="http://www.metadataworkinggroup.com/schemas/regions/"
                  xmlns:stArea="http://ns.adobe.com/xmp/sType/Area#">
                <mwg-rs:Regions rdf:parseType="Resource">
                  <mwg-rs:AppliedToDimensions>
                    <rdf:Description stArea:w="1500" stArea:h="1000" stArea:unit="pixel"/>
                  </mwg-rs:AppliedToDimensions>
                  <mwg-rs:RegionList>
                    <rdf:Bag>
                      <rdf:li>
                        <rdf:Description
                          mwg-rs:Name="Anna"
                          mwg-rs:Type="Face">
                          <mwg-rs:Area
                            stArea:h="0.4"
                            stArea:unit="normalized"
                            stArea:w="0.3"
                            stArea:x="0.1"
                            stArea:y="0.2"/>
                          <mwg-rs:Extensions vendor:FaceId="42"
                              xmlns:vendor="https://example.org/xmp-vendor/1.0/"/>
                        </rdf:Description>
                      </rdf:li>
                      <rdf:li>
                        <rdf:Description
                          mwg-rs:Name="Berta"
                          mwg-rs:Type="Face">
                          <mwg-rs:Area
                            stArea:h="0.5"
                            stArea:unit="normalized"
                            stArea:w="0.3"
                            stArea:x="0.6"
                            stArea:y="0.7"/>
                        </rdf:Description>
                      </rdf:li>
                    </rdf:Bag>
                  </mwg-rs:RegionList>
                </mwg-rs:Regions>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        xmpMeta.setFaceRegions(
            listOf(XmpFaceRegion("Berta", XMPRegionArea(0.6, 0.7, 0.3, 0.5))),
            1500,
            1000
        )

        val serialized = XMPMetaFactory.serializeToString(xmpMeta)

        assertTrue(serialized.contains("Berta"))

        assertTrue(!serialized.contains("FaceId"))
    }

    /**
     * Clearing the face list keeps regions of other types.
     */
    @Test
    fun testSetEmptyFaceRegionsKeepsNonFaceRegions() {

        /* language=XML */
        val testXmp = """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""
                  xmlns:mwg-rs="http://www.metadataworkinggroup.com/schemas/regions/"
                  xmlns:stArea="http://ns.adobe.com/xmp/sType/Area#">
                <mwg-rs:Regions rdf:parseType="Resource">
                  <mwg-rs:AppliedToDimensions>
                    <rdf:Description stArea:w="1500" stArea:h="1000" stArea:unit="pixel"/>
                  </mwg-rs:AppliedToDimensions>
                  <mwg-rs:RegionList>
                    <rdf:Bag>
                      <rdf:li>
                        <rdf:Description
                          mwg-rs:Name="Doggy"
                          mwg-rs:Type="Pet">
                          <mwg-rs:Area
                            stArea:h="0.05"
                            stArea:unit="normalized"
                            stArea:w="0.03"
                            stArea:x="0.2"
                            stArea:y="0.3"/>
                        </rdf:Description>
                      </rdf:li>
                      <rdf:li>
                        <rdf:Description
                          mwg-rs:Name="Anna"
                          mwg-rs:Type="Face">
                          <mwg-rs:Area
                            stArea:h="0.4"
                            stArea:unit="normalized"
                            stArea:w="0.3"
                            stArea:x="0.1"
                            stArea:y="0.2"/>
                        </rdf:Description>
                      </rdf:li>
                    </rdf:Bag>
                  </mwg-rs:RegionList>
                </mwg-rs:Regions>
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()

        val xmpMeta = XMPMetaFactory.parseFromString(testXmp)

        xmpMeta.setFaceRegions(emptyList(), 1500, 1000)

        assertEquals(emptyList(), xmpMeta.getFaceRegions())

        val serialized = XMPMetaFactory.serializeToString(xmpMeta)

        assertTrue(serialized.contains("Doggy"))
    }
}
