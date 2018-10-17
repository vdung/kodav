@file:Suppress("SpellCheckingInspection")

package vdung.kodav

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

val TAG_PROP = webDavTag("prop")

val PROP_DISPLAYNAME = webDavTag("displayname")
val PROP_GETCONTENTTYPE = webDavTag("getcontenttype")
val PROP_GETCONTENTLENGTH = webDavTag("getcontentlength")

typealias PropParser = (XmlPullParser) -> Prop

interface Prop {
    interface Parser<T : Prop> {
        val tag: Xml.Tag
        fun parse(parser: XmlPullParser): T
    }

    class Writer : TagWriter(TAG_PROP) {
        operator fun String.unaryMinus() = addChild(TagWriter(Xml.Tag("", this))) {}
        operator fun Xml.Tag.unaryMinus() = addChild(TagWriter(this)) {}
    }

    companion object {
        private val factories = mutableMapOf<Xml.Tag, PropParser>()

        init {
            register(DisplayName.Companion)
            register(GetContentType.Companion)
            register(GetContentLength.Companion)
        }

        fun register(tag: Xml.Tag, parser: PropParser) {
            factories[tag] = parser
        }

        fun register(parser: Parser<*>) {
            register(parser.tag, parser::parse)
        }

        fun register(vararg parsers: Parser<*>) {
            for (parser in parsers) {
                register(parser)
            }
        }

        @Throws(XmlPullParserException::class, IOException::class)
        fun parse(parser: XmlPullParser) = Xml.parse(parser, TAG_PROP, linkedMapOf<Xml.Tag, Prop>()) {
            for ((tag, factory) in factories) {
                tag {
                    set(tag, factory(it))
                }
            }
        }
    }
}

data class DisplayName(val displayName: String?) : Prop {
    companion object : Prop.Parser<DisplayName> {
        override val tag: Xml.Tag get() = PROP_DISPLAYNAME

        override fun parse(parser: XmlPullParser) =
                DisplayName(Xml.parseText(parser))
    }
}

data class GetContentLength(val contentLength: Long) : Prop {
    companion object : Prop.Parser<GetContentLength> {
        override val tag: Xml.Tag get() = PROP_GETCONTENTLENGTH

        override fun parse(parser: XmlPullParser) =
                GetContentLength(Xml.parseText(parser)?.toLong() ?: -1)
    }
}

data class GetContentType(val contentType: String?) : Prop {
    companion object : Prop.Parser<GetContentType> {
        override val tag: Xml.Tag get() = PROP_GETCONTENTTYPE

        override fun parse(parser: XmlPullParser) =
                GetContentType(Xml.parseText(parser))
    }
}
