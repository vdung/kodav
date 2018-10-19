package vdung.kodav

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * An XML element parser.
 *
 * @param tag The element's namespace and name
 * @param factories The element's children parser
 * @sample [MultiStatus]
 * @see [Xml.parse]
 */
class TagParser<T>(private val tag: Xml.Tag, private val factories: Map<Xml.Tag, (XmlPullParser, T) -> Unit>) {

    /**
     * Parse the element. The [builder] will be passed to the children's factories along with the [parser].
     *
     * @param builder An object builder
     * @param parser The XML parser
     * @return The [builder]
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(builder: T, parser: XmlPullParser): T {
        Xml.parseTag(parser, tag) {
            val childTag = Xml.Tag(it.namespace, it.name)
            val factory = factories[childTag] ?: { p, _ -> Xml.skip(p) }
            factory(parser, builder)
        }

        return builder
    }

    companion object {
        /**
         * Create a [TagParser.Builder].
         *
         * @param tag [TagParser.tag]
         * @param init [TagParser.Builder] configuration
         * @return [TagParser.Builder]
         */
        inline fun <T> builder(tag: Xml.Tag, init: Builder<T>.() -> Unit) = Builder<T>(tag).apply(init)

        /**
         * Create a [TagParser].
         *
         * @param tag [TagParser.tag]
         * @param init [TagParser.Builder] configuration
         * @return [TagParser]
         */
        inline fun <T> create(tag: Xml.Tag, init: Builder<T>.() -> Unit) = builder(tag, init).create()
    }

    /**
     * [TagParser] builder.
     *
     * @param tag [TagParser.tag]
     */
    class Builder<T>(private val tag: Xml.Tag) {
        private val factories = mutableMapOf<Xml.Tag, (XmlPullParser, T) -> Unit>()

        /**
         * Register a child element's parser.
         *
         * @param tag The child element's tag
         * @param parser The child element's parser function
         */
        fun register(tag: Xml.Tag, parser: (XmlPullParser, T) -> Unit) {
            factories[tag] = parser
        }

        /**
         * @see [register]
         */
        operator fun Xml.Tag.invoke(parse: T.(XmlPullParser) -> Any) = register(this) { parser, builder ->
            builder.parse(parser)
        }

        /**
         * @see [register]
         */
        operator fun String.invoke(parse: T.(XmlPullParser) -> Any) = Xml.Tag("", this)(parse)

        /**
         * @see [register]
         */
        operator fun <U> Xml.Tag.invoke(parse: (XmlPullParser) -> U, build: T.(U) -> Any) = register(this) { parser, builder ->
            builder.build(parse(parser))
        }

        /**
         * @see [register]
         */
        operator fun <U> String.invoke(parse: (XmlPullParser) -> U, build: T.(U) -> Any) = Xml.Tag("", this)(parse, build)

        /**
         * Create a [TagParser]
         *
         * @return [TagParser]
         */
        fun create() = TagParser(tag, factories)
    }
}

@DslMarker
annotation class WriterMarker

/**
 * Represent an XML writer.
 */
@WriterMarker
interface XmlWriter {

    /**
     * Write an element to [writer].
     *
     * @param writer The XML serializer
     */
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
 * @sample Op
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
    fun <T : XmlWriter> addChild(child: T, init: T.() -> Unit) = apply { children.add(child.apply(init)) }
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
     *
     *  @param parser The XML parser
     *  @param tag The current element's tag
     *  @param builder The object that will be passed to [TagParser.parse]
     *  @param init [TagParser.Builder] configuration
     *  @return The [builder]
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun <T> parse(parser: XmlPullParser, tag: Xml.Tag, builder: T, init: TagParser.Builder<T>.() -> Unit): T {
        return parse(parser, builder, TagParser.create(tag, init))
    }

    /**
     * Parse the current element using an initialized [TagParser].
     *
     * @param parser The XML parser
     * @param builder The object that will be passed to [TagParser.parse]
     * @param tagParser The tag parser
     * @return The [builder]
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun <T> parse(parser: XmlPullParser, builder: T, tagParser: TagParser<T>) = tagParser.parse(builder, parser)
}
