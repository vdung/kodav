package vdung.kodav.retrofit

import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Converter
import vdung.kodav.NS_DAV
import vdung.kodav.Xml
import vdung.kodav.XmlWriter
import java.io.ByteArrayOutputStream

class WebDavRequestConverter : Converter<XmlWriter, RequestBody> {
    override fun convert(value: XmlWriter): RequestBody {
        val output = ByteArrayOutputStream()

        val serializer = Xml.newSerializer(output)
        serializer.setPrefix("d", NS_DAV)
        serializer.startDocument("UTF-8", false)
        value.write(serializer)
        serializer.endDocument()

        return RequestBody.create(MediaType.parse("text/xml"), output.toByteArray())
    }
}