package vdung.kodav

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlSerializer
import java.io.IOException

const val NS_DAV = "DAV:"
fun webDavTag(name: String) = Xml.Tag(NS_DAV, name)

object WebDav {
    /**
     * Parse a [MultiStatus] response.
     *
     * @param parser An initialized [XmlPullParser]
     * @return A [MultiStatus] object
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun multiStatus(parser: XmlPullParser): MultiStatus {
        parser.nextTag()
        return MultiStatus.parse(parser)
    }

    /**
     * Serialize a [SearchRequest] to the [serializer].
     *
     * @param serializer An initialized [XmlSerializer]
     * @param init [SearchRequest.Writer] configuration
     * @return The [serializer]
     */
    @Throws(IOException::class)
    fun searchRequest(serializer: XmlSerializer, init: SearchRequest.Writer.() -> Unit): XmlSerializer {
        val writer = SearchRequest.Writer()
        writer.init()

        serializer.setPrefix("d", NS_DAV)
        serializer.startDocument("UTF-8", false)
        writer.write(serializer)
        serializer.endDocument()

        return serializer
    }
}