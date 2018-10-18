package vdung.kodav.retrofit

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import vdung.kodav.MultiStatus
import vdung.kodav.Response
import vdung.kodav.SearchRequest
import java.io.ByteArrayOutputStream

class WebDavConverterFactoryTest {

    interface Service {
        @POST("search")
        fun search(@Body searchRequest: SearchRequest): Call<MultiStatus>
    }

    @Test
    fun factory_createConverters() {
        val server = MockWebServer()
        server.start()

        val retrofit = Retrofit.Builder()
                .addConverterFactory(WebDavConverterFactory())
                .baseUrl(server.url("/"))
                .build()

        val service = retrofit.create(Service::class.java)

        server.enqueue(MockResponse().setBody("""
            <d:multistatus xmlns:d="DAV:">
                <d:response>
                    <d:href>/remote.php/dav/files/USERNAME/</d:href>
                </d:response>
            </d:multistatus>
        """.trimIndent()))

        val response = service.search(SearchRequest().apply {
            basicSearch { }
        }).execute()

        assertEquals(
                MultiStatus(listOf(Response("/remote.php/dav/files/USERNAME/", emptyList()))),
                response.body()
        )

        val request = server.takeRequest()
        val requestBody = ByteArrayOutputStream()
        request.body.copyTo(requestBody)
        assertEquals(
                "<?xml version='1.0' encoding='UTF-8' standalone='no' ?><d:searchrequest xmlns:d=\"DAV:\"><d:basicsearch /></d:searchrequest>",
                requestBody.toString().replace(Regex("\\n\\s*"), "")
        )
    }
}