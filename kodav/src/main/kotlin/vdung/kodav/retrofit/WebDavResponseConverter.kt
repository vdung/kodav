package vdung.kodav.retrofit

import okhttp3.ResponseBody
import retrofit2.Converter
import vdung.kodav.MultiStatus
import vdung.kodav.WebDav
import vdung.kodav.Xml

class WebDavResponseConverter : Converter<ResponseBody, MultiStatus> {
    override fun convert(value: ResponseBody): MultiStatus {
        return WebDav.multiStatus(Xml.newPullParser(value.byteStream()))
    }
}