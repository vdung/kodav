package vdung.kodav.retrofit

import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import vdung.kodav.MultiStatus
import java.lang.reflect.Type

class WebDavConverterFactory : Converter.Factory() {
    override fun responseBodyConverter(type: Type, annotations: Array<Annotation>, retrofit: Retrofit): Converter<ResponseBody, *>? {
        if (getRawType(type).kotlin != MultiStatus::class) {
            return super.responseBodyConverter(type, annotations, retrofit)
        }
        return WebDavResponseConverter()
    }
}