package vdung.kodav

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * A parser that, combined with a [builder], will parse the XML and build an object at the same time.
 *
 * @sample Xml.parse
 * @param builder An object builder
 * @see Xml.parse
 */
class TagParser<T>(val builder: T) {

    private val factories = mutableMapOf<Xml.Tag, (XmlPullParser) -> Unit>()

    private fun register(tag: Xml.Tag, parser: (XmlPullParser) -> Unit) {
        factories[tag] = parser
    }

    operator fun Xml.Tag.invoke(parse: T.(XmlPullParser) -> Any) = register(this) { builder.parse(it) }
    operator fun String.invoke(parse: T.(XmlPullParser) -> Any) = Xml.Tag("", this)(parse)

    operator fun <U> Xml.Tag.invoke(parse: (XmlPullParser) -> U, build: T.(U) -> Any) =
            register(this) { builder.build(parse(it)) }

    operator fun <U> String.invoke(parse: (XmlPullParser) -> U, build: T.(U) -> Any) = Xml.Tag("", this)(parse, build)

    fun parse(parser: XmlPullParser) {
        val tag = Xml.Tag(parser.namespace, parser.name)
        val factory = factories[tag] ?: Xml::skip
        factory(parser)
    }
}

@DslMarker
annotation class WriterMarker

/**
 * Represent an XML writer. [write] should write a complete element.
 */
@WriterMarker
interface XmlWriter {
    @Throws(IOException::class)
    fun write(writer: XmlSerializer)
}

/**
 * A [XmlWriter] that write [value] as a text element.
 *
 * @param value The text to be written
 */
class TextWriter(val value: String?) : XmlWriter {
    override fun write(writer: XmlSerializer) {
        value?.let {
            writer.text(it)
        }
    }
}

/**
 * A [XmlWriter] that write an element with namespace and name from [tag].
 *
 * @sample SearchRequest.Writer
 * @param tag The namespace and name of the element
 */
open class TagWriter(val tag: Xml.Tag) : XmlWriter {

    private val children = mutableListOf<XmlWriter>()

    @Throws(IOException::class)
    override fun write(writer: XmlSerializer) {
        writer.startTag(tag.namespace, tag.name)
        for (child in children) {
            child.write(writer)
        }
        writer.endTag(tag.namespace, tag.name)
    }

    /**
     * Add a child element to be written
     *
     * @param child The child's writer
     * @param init [child] configuration
     */
    fun <T : XmlWriter> addChild(child: T, init: T.() -> Unit) = apply {
        child.init()
        children.add(child)
    }
}

/**
 * Helper functions for parsing and writing XML.
 */
object Xml {
    data class Tag(val namespace: String, val name: String)

    private val factory: XmlPullParserFactory

    init {
        try {
            factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
        } catch (e: XmlPullParserException) {
            throw AssertionError(e)
        }
    }

    /**
     * Create a new [XmlPullParser].
     *
     * @return A [XmlPullParser]
     */
    fun newPullParser() = factory.newPullParser()!!

    /**
     * Create a new [XmlPullParser] and set the input to [inputStream].
     *
     * @param inputStream The input of the parser
     * @return A [XmlPullParser]
     */
    fun newPullParser(inputStream: InputStream): XmlPullParser {
        val parser = newPullParser()
        parser.setInput(inputStream, null)
        return parser
    }

    /**
     * Create a new [XmlSerializer].
     *
     * @return A [XmlSerializer]
     */
    fun newSerializer() = factory.newSerializer()!!

    /**
     * Create a new [XmlSerializer] and set the output to [outputStream].
     *
     * @param outputStream The out of the parser
     * @return A [XmlSerializer]
     */
    fun newSerializer(outputStream: OutputStream): XmlSerializer {
        val serializer = newSerializer()
        serializer.setOutput(outputStream, null)
        return serializer
    }

    /**
     * Parse the content of the current element using [parse].
     *
     * [parser]'s current eventType must be [XmlPullParser.START_TAG].
     * Parsing will finish after encountering an [XmlPullParser.END_TAG].
     * Each time [parse] is called, it should parse only **one** child element.
     *
     * @param parser The XML parser
     * @param tag The current element's namespace and name
     * @param parse Child element parse function
     */
    @Throws(XmlPullParserException::class, IOException::class)
    inline fun parseTag(parser: XmlPullParser, tag: Xml.Tag, parse: (parser: XmlPullParser) -> Unit) {
        parser.require(XmlPullParser.START_TAG, tag.namespace, tag.name)
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            parse(parser)
        }
        parser.require(XmlPullParser.END_TAG, tag.namespace, tag.name)
    }

    /**
     * Parse the text of the current element.
     *
     * @param parser The XML parser
     * @return The text content
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun parseText(parser: XmlPullParser): String? {
        var text: String? = null

        if (parser.next() == XmlPullParser.TEXT) {
            text = parser.text
            parser.nextTag()
        }

        return text
    }

    /**
     * Skip the current element. The [parser]'s currentEvent must be [XmlPullParser.START_TAG].
     *
     * @param parser The XML parser
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }

        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    /**
     * Parse the current element and build an object using a [TagParser] combined with the [builder].
     * This is mainly used for implementing parsers for concrete types.
     *
     * <code>
     *
     *      data class Bar(val value: String?) {
     *          companion object {
     *              fun parse(parser: XmlPullParser) = Bar(Xml.parseText(parser))
     *          }
     *      }
     *
     *      data class Foo(var bar: Bar?) {
     *          companion object {
     *              fun parse(parser: XmlPullParser) = Xml.parse(parser, Xml.Tag("", "foo"), Foo()) {
     *                  "bar" { apply { bar = Bar.parse(it) } }
     *              }.build()
     *          }
     *      }
     *
     *      val parser = Xml.newPullParser(ByteArrayInputStream("""
     *          <foo>
     *              <bar>baz</bar>
     *          </foo>
     *      """.toByteArray()))
     *      parser.next()
     *
     *      val foo = Foo.parse(parser)
     *      assertEquals(foo.bar?.value, "baz")
     *
     *  </code>
     *
     *  @param parser The XML parser
     *  @param tag The current element's tag
     *  @param builder An object that will be passed to a [TagParser]
     *  @param init [TagParser] configuration
     *  @return The [builder]
     */
    @Throws(XmlPullParserException::class, IOException::class)
    inline fun <T> parse(parser: XmlPullParser, tag: Xml.Tag, builder: T, init: TagParser<T>.() -> Unit): T {
        val tagParser = TagParser(builder)
        tagParser.init()
        parseTag(parser, tag) {
            tagParser.parse(it)
        }

        return builder
    }
}
