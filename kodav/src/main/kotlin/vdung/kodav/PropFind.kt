package vdung.kodav

val TAG_PROPFIND = webDavTag("propfind")

class PropFind : TagWriter(TAG_PROPFIND) {
    fun prop(init: Prop.Writer.() -> Unit) = addChild(Prop.Writer(), init)
}
