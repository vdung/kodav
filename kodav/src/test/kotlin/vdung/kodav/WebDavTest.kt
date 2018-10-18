package vdung.kodav

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class WebDavTest {

    @Test
    fun multiStatus_parse() {
        val parser = Xml.newPullParser(ByteArrayInputStream("""
            <d:multistatus xmlns:d="DAV:">
                <d:response>
                    <d:href>/remote.php/dav/files/USERNAME/</d:href>
                    <d:propstat>
                        <d:prop>
                            <d:getlastmodified>Tue, 13 Oct 2015 17:07:45 GMT</d:getlastmodified>
                            <d:resourcetype><d:collection/></d:resourcetype>
                            <d:quota-used-bytes>163</d:quota-used-bytes>
                            <d:quota-available-bytes>11802275840</d:quota-available-bytes>
                            <d:getetag>"561d3a6139d05"</d:getetag>
                        </d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                </d:response>
                <d:response>
                    <d:href>/remote.php/dav/files/USERNAME/welcome.txt</d:href>
                    <d:propstat>
                        <d:prop>
                            <d:getlastmodified>Tue, 13 Oct 2015 17:07:35 GMT</d:getlastmodified>
                            <d:getcontentlength>163</d:getcontentlength>
                            <d:resourcetype/>
                            <d:getetag>"47465fae667b2d0fee154f5e17d1f0f1"</d:getetag>
                            <d:getcontenttype>text/plain</d:getcontenttype>
                        </d:prop>
                        <d:status>HTTP/1.1 200 OK</d:status>
                    </d:propstat>
                </d:response>
            </d:multistatus>
        """.toByteArray()))
        val multiStatus = WebDav.multiStatus(parser)
        assertEquals(2, multiStatus.responses.size)
        multiStatus.apply {
            responses[0].apply {
                assertEquals("/remote.php/dav/files/USERNAME/", href)
                assertEquals(1, propStats.size)
                assertEquals("HTTP/1.1 200 OK", propStats[0].status)
            }
            responses[1].apply {
                assertEquals("/remote.php/dav/files/USERNAME/welcome.txt", href)
                assertEquals(1, propStats.size)
                assertEquals(GetContentType("text/plain"), propStats[0].prop(GetContentType.tag))
                assertEquals(163, propStats[0].propValue(GetContentLength.tag)!!)
                assertEquals(HttpDate.parse("Tue, 13 Oct 2015 17:07:35 GMT"), propStats[0].propValue(GetLastModified.tag))
                assertEquals("HTTP/1.1 200 OK", propStats[0].status)
            }
        }
    }

    @Test
    fun searchRequest_write() {
        val output = ByteArrayOutputStream()
        val serializer = Xml.newSerializer(output)
        WebDav.searchRequest(serializer) {
            basicSearch {
                select {
                    prop {
                        -PROP_DISPLAYNAME
                    }
                }
                from {
                    scope {
                        href("/files/USER")
                        depth(null)
                    }
                }
                where {
                    eq {
                        prop {
                            -PROP_GETCONTENTTYPE
                        }
                        literal("image/png")
                    }
                }
            }
        }

        assertEquals("""
            <?xml version='1.0' encoding='UTF-8' standalone='no' ?>
            <d:searchrequest xmlns:d="DAV:">
                <d:basicsearch>
                    <d:select>
                        <d:prop>
                            <d:displayname />
                        </d:prop>
                    </d:select>
                    <d:from>
                        <d:scope>
                            <d:href>/files/USER</d:href>
                            <d:depth>infinity</d:depth>
                        </d:scope>
                    </d:from>
                    <d:where>
                        <d:eq>
                            <d:prop>
                                <d:getcontenttype />
                            </d:prop>
                            <d:literal>image/png</d:literal>
                        </d:eq>
                    </d:where>
                </d:basicsearch>
            </d:searchrequest>
        """.replace(Regex("\\n\\s*"), ""), output.toString())
    }
}
