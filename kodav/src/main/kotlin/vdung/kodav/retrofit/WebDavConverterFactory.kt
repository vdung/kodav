package vdung.kodav.retrofit

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import vdung.kodav.MultiStatus
import vdung.kodav.XmlWriter
import java.lang.reflect.Type

class WebDavConverterFactory : Converter.Factory() {
    override fun responseBodyConverter(type: Type, annotations: Array<Annotation>, retrofit: Retrofit): Converter<ResponseBody, *>? {
        return when (getRawType(type).kotlin) {
            MultiStatus::class -> WebDavResponseConverter()
            else -> super.responseBodyConverter(type, annotations, retrofit)
        }
    }

    override fun requestBodyConverter(type: Type, parameterAnnotations: Array<Annotation>, methodAnnotations: Array<Annotation>, retrofit: Retrofit): Converter<*, RequestBody>? {
        if (XmlWriter::class.java.isAssignableFrom(getRawType(type))) {
            return WebDavRequestConverter()
        }
        return super.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)
    }
}