package de.stefan_oltmann.xmp

import de.stefan_oltmann.xmp.internal.XMPErrorConst
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Hostile files can nest XML elements arbitrarily deep. The recursive descent parsers must
 * reject such input with an [XMPException] instead of exhausting the stack with a
 * `StackOverflowError` that would escape the library's exception contract.
 */
class XMPMetaDeepNestingTest {

    /**
     * Builds an RDF document with the given number of nested rdf:parseType="Resource" structs.
     */
    private fun buildNestedRdf(depth: Int): String {

        val open = StringBuilder()

        val close = StringBuilder()

        for (index in 1..depth) {

            open.append("<ns:p$index rdf:parseType=\"Resource\">")

            close.insert(0, "</ns:p$index>")
        }

        /* language=XML */
        return """
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                     xmlns:ns="http://example.org/nesting/">
              <rdf:Description rdf:about="">
                $open<ns:leaf>x</ns:leaf>$close
              </rdf:Description>
            </rdf:RDF>
        """.trimIndent()
    }

    /**
     * Nesting beyond the parser limit is rejected as a normal parse error.
     */
    @Test
    fun testExcessiveRdfNestingIsRejected() {

        val input = buildNestedRdf(depth = 200)

        val ex = assertFailsWith<XMPException> {
            XMPMetaFactory.parseFromString(input)
        }

        assertEquals(XMPErrorConst.BADRDF, ex.errorCode)
    }

    /**
     * Nesting below the limit still parses, so the limit does not affect legitimate files.
     */
    @Test
    fun testModerateRdfNestingStillParses() {

        val input = buildNestedRdf(depth = 64)

        val xmpMeta = XMPMetaFactory.parseFromString(input)

        assertEquals("", xmpMeta.getObjectName())
    }

    /**
     * Generic XML wrappers around the RDF part are searched recursively; excessive wrapper
     * nesting is also rejected instead of overflowing the stack.
     */
    @Test
    fun testExcessiveXmlWrapperNestingIsRejected() {

        val depth = 2000

        val open = StringBuilder()

        val close = StringBuilder()

        for (index in 1..depth) {

            open.append("<w$index>")

            close.insert(0, "</w$index>")
        }

        /* language=XML */
        val input = """
            <?xpacket begin="" id="W5M0MpCehiHzreSzNTczkc9d"?>
            $open
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
              <rdf:Description rdf:about=""/>
            </rdf:RDF>
            $close
            <?xpacket end="w"?>
        """.trimIndent()

        val ex = assertFailsWith<XMPException> {
            XMPMetaFactory.parseFromString(input)
        }

        assertEquals(XMPErrorConst.BADXMP, ex.errorCode)
    }
}
