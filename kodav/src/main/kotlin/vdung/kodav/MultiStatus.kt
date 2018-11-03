@file:Suppress("SpellCheckingInspection")

package vdung.kodav

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException


val TAG_PROPSTAT = webDavTag("propstat")
val TAG_STATUS = webDavTag("status")
val TAG_HREF = webDavTag("href")
val TAG_RESPONSE = webDavTag("response")
val TAG_MULTISTATUS = webDavTag("multistatus")

data class PropStat(val props: Map<Xml.Tag, Prop<*>>, val status: String?) {

    inline fun <reified U : Prop<*>> prop(tag: Xml.Tag) = props[tag] as? U

    inline fun <reified T, reified U : Prop<T>> propValue(tag: Xml.Tag) = prop<U>(tag)?.value

    companion object {
        @JvmStatic
        private val PARSER = TagParser.create<Builder>(TAG_PROPSTAT) {
            TAG_STATUS { setStatus(Xml.parseText(it)) }
            TAG_PROP { setProps(Prop.parse(it)) }
        }

        @Throws(XmlPullParserException::class, IOException::class)
        fun parse(parser: XmlPullParser) = PARSER.parse(Builder(), parser).build()
    }

    class Builder {
        private var props = linkedMapOf<Xml.Tag, Prop<*>>()
        private var status: String? = null

        fun setStatus(status: String?) = apply { this.status = status }
        fun setProps(props: Map<Xml.Tag, Prop<*>>) = apply { this.props = LinkedHashMap(props) }

        fun build() = PropStat(props, status)
    }
}

data class Response(val href: String?, val propStats: List<PropStat>) {

    companion object {
        @JvmStatic
        private val PARSER = TagParser.create<Builder>(TAG_RESPONSE) {
            TAG_HREF { setHref(vdung.kodav.Xml.parseText(it)) }
            TAG_PROPSTAT { addPropStat(vdung.kodav.PropStat.parse(it)) }
        }

        @Throws(XmlPullParserException::class, IOException::class)
        fun parse(parser: XmlPullParser) = PARSER.parse(Builder(), parser).build()
    }

    class Builder {
        private var href: String? = null
        private var propStats = mutableListOf<PropStat>()

        fun setHref(href: String?) = apply { this.href = href }
        fun addPropStat(propStat: PropStat) = apply { this.propStats.add(propStat) }

        fun build() = Response(href, propStats)
    }
}

data class MultiStatus(val responses: List<Response>) {
    companion object {
        @JvmStatic
        private val PARSER = TagParser.create<Builder>(TAG_MULTISTATUS) {
            TAG_RESPONSE { addResponse(Response.parse(it)) }
        }

        @Throws(XmlPullParserException::class, IOException::class)
        fun parse(parser: XmlPullParser) = PARSER.parse(Builder(), parser).build()
    }

    class Builder {
        private var responses = mutableListOf<Response>()

        fun addResponse(response: Response) = apply { responses.add(response) }

        fun build() = MultiStatus(responses)
    }
}
