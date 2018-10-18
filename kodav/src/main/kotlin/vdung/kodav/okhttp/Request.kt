package vdung.kodav.okhttp

import okhttp3.MediaType
import okhttp3.RequestBody
import vdung.kodav.SearchRequest
import vdung.kodav.WebDav
import vdung.kodav.Xml
import java.io.ByteArrayOutputStream

fun searchRequest(init: SearchRequest.() -> Unit): RequestBody {
    val output = ByteArrayOutputStream()
    WebDav.searchRequest(Xml.newSerializer(output), init)

    return RequestBody.create(MediaType.parse("text/xml"), output.toByteArray())
}
