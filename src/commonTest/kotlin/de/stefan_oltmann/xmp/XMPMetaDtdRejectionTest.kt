package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/*
 * Untrusted XMP often comes from arbitrary files. The parser rejects any document containing
 * a DOCTYPE declaration before any XML parsing happens, so internal or external entity
 * definitions can never be expanded (billion laughs) or resolved (XXE).
 */
class XMPMetaDtdRejectionTest {

    @Test
    fun testDoctypeWithInternalEntitiesIsRejected() {

        /* language=XML */
        val input = """
            <?xpacket begin="" id="W5M0MpCehiHzreSzNTczkc9d"?>
            <!DOCTYPE rdf:RDF [
              <!ENTITY a "AAAA">
              <!ENTITY b "&a;&a;&a;&a;&a;&a;&a;&a;&a;&a;">
            ]>
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about="" xmlns:test="http://example.org/">
                <test:Prop>&b;</test:Prop>
              </rdf:Description>
            </rdf:RDF>
            <?xpacket end="w"?>
        """.trimIndent()

        val ex = assertFailsWith<XMPException> {
            XMPMetaFactory.parseFromString(input)
        }

        assertEquals(XMPErrorConst.BADSTREAM, ex.errorCode)
    }

    /**
     * A DOCTYPE must be rejected even when the remaining document would parse on its own,
     * so the corrupted-file recovery cannot salvage a DTD-carrying packet by slicing around
     * the declaration.
     */
    @Test
    fun testDoctypeIsRejectedEvenWhenRemainderIsValid() {

        /* language=XML */
        val input = """
            <?xpacket begin="" id="W5M0MpCehiHzreSzNTczkc9d"?>
            <!DOCTYPE rdf:RDF>
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about="" xmlns:test="http://example.org/">
                <test:Prop>value</test:Prop>
              </rdf:Description>
            </rdf:RDF>
            <?xpacket end="w"?>
        """.trimIndent()

        val ex = assertFailsWith<XMPException> {
            XMPMetaFactory.parseFromString(input)
        }

        assertEquals(XMPErrorConst.BADSTREAM, ex.errorCode)
    }
}
