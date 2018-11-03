@file:Suppress("SpellCheckingInspection")

package vdung.kodav

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.*
import kotlin.collections.LinkedHashMap

val TAG_PROP = webDavTag("prop")

val PROP_DISPLAYNAME = webDavTag("displayname")
val PROP_GETCONTENTTYPE = webDavTag("getcontenttype")
val PROP_GETCONTENTLENGTH = webDavTag("getcontentlength")
val PROP_GETETAG = webDavTag("getetag")
val PROP_GETLASTMODIFIED = webDavTag("getlastmodified")

interface Prop<T> {
    val value: T?

    interface Parser<T : Prop<*>> {
        val tag: Xml.Tag
        fun parse(parser: XmlPullParser): T
    }

    class Writer : TagWriter(TAG_PROP) {
        operator fun String.unaryMinus() = addChild(TagWriter(Xml.Tag("", this))) {}
        operator fun Xml.Tag.unaryMinus() = addChild(TagWriter(this)) {}
    }

    companion object {
        private fun TagParser.Builder<LinkedHashMap<Xml.Tag, Prop<*>>>.register(vararg propParsers: Prop.Parser<*>) = apply {
            for (propParser in propParsers) {
                propParser.tag {
                    set(propParser.tag, propParser.parse(it))
                }
            }
        }

        private val parserBuilder = TagParser.builder<LinkedHashMap<Xml.Tag, Prop<*>>>(TAG_PROP) {
            register(
                    DisplayName,
                    GetContentType,
                    GetContentLength,
                    GetETag,
                    GetLastModified
            )
        }

        fun register(propParser: Parser<*>) {
            parserBuilder.register(propParser)
        }

        @Throws(XmlPullParserException::class, IOException::class)
        fun parse(parser: XmlPullParser) = parserBuilder.create().parse(linkedMapOf(), parser)
    }
}

data class DisplayName(override val value: String?) : Prop<String> {
    companion object : Prop.Parser<DisplayName> {
        override val tag: Xml.Tag get() = PROP_DISPLAYNAME

        override fun parse(parser: XmlPullParser) =
                DisplayName(Xml.parseText(parser))
    }
}

data class GetContentLength(override val value: Long?) : Prop<Long> {
    companion object : Prop.Parser<GetContentLength> {
        override val tag: Xml.Tag get() = PROP_GETCONTENTLENGTH

        override fun parse(parser: XmlPullParser) =
                GetContentLength(Xml.parseText(parser)?.toLong())
    }
}

data class GetContentType(override val value: String?) : Prop<String> {
    companion object : Prop.Parser<GetContentType> {
        override val tag: Xml.Tag get() = PROP_GETCONTENTTYPE

        override fun parse(parser: XmlPullParser) =
                GetContentType(Xml.parseText(parser))
    }
}

data class GetETag(override val value: String?) : Prop<String> {
    companion object : Prop.Parser<GetETag> {
        override val tag: Xml.Tag get() = PROP_GETETAG

        override fun parse(parser: XmlPullParser) =
                GetETag(Xml.parseText(parser))
    }
}

data class GetLastModified(override val value: Date?) : Prop<Date> {
    companion object : Prop.Parser<GetLastModified> {
        override val tag: Xml.Tag get() = PROP_GETLASTMODIFIED

        override fun parse(parser: XmlPullParser) =
                GetLastModified(Xml.parseText(parser)?.let { HttpDate.parse(it) })
    }
}
