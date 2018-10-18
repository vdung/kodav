package vdung.kodav

import org.junit.Test

import org.junit.Assert.*
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream


class XmlTest {

    data class Bar(val value: String?) {
        companion object {
            fun parse(parser: XmlPullParser) = Bar(Xml.parseText(parser))
        }
    }

    data class Foo(var bar: Bar? = null) {
        companion object {
            fun parse(parser: XmlPullParser) = Xml.parse(parser, Xml.Tag("", "foo"), Foo()) {
                "bar" { apply { bar = Bar.parse(it) } }
            }
        }
    }

    @Test
    fun parser_parse() {
        val parser = Xml.newPullParser(ByteArrayInputStream("""
            <bar>baz</bar>
        """.trimIndent().toByteArray()))
        parser.next()

        val tagParser = TagParser(Foo())
        tagParser.apply {
            "bar" { apply { bar = Bar(Xml.parseText(it)) } }
        }.parse(parser)

        assertEquals("baz", tagParser.builder.bar?.value)
    }

    @Test
    fun xml_parseFooBar() {
        val parser = Xml.newPullParser(ByteArrayInputStream("""
            <foo>
                <bar>baz</bar>
            </foo>
        """.trimIndent().toByteArray()))
        parser.next()

        val foo = Foo.parse(parser)

        assertEquals("baz", foo.bar?.value)
    }
}

