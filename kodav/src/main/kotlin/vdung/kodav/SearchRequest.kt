@file:Suppress("SpellCheckingInspection")

package vdung.kodav

val TAG_LITERAL = webDavTag("literal")
val TAG_SCOPE = webDavTag("scope")
val TAG_DEPTH = webDavTag("depth")
val TAG_SELECT = webDavTag("select")
val TAG_FROM = webDavTag("from")
val TAG_WHERE = webDavTag("where")
val TAG_BASICSEARCH = webDavTag("basicsearch")
val TAG_SEARCHREQUEST = webDavTag("searchrequest")

class Select : TagWriter(TAG_SELECT) {
    fun prop(init: Prop.Writer.() -> Unit) = addChild(Prop.Writer(), init)
}

class Scope : TagWriter(TAG_SCOPE) {
    companion object {
        @JvmStatic
        val DEPTH_INFINITY = -1
    }

    fun href(href: String) = addChild(TagWriter(TAG_HREF)) {
        addChild(TextWriter(href)) {}
    }

    fun depth(depth: Int) = addChild(TagWriter(TAG_DEPTH)) {
        addChild(TextWriter(if (depth == DEPTH_INFINITY) "infinity" else depth.toString())) {}
    }
}

class From : TagWriter(TAG_FROM) {
    fun scope(init: Scope.() -> Unit) = addChild(Scope(), init)
}

abstract class Op(tag: Xml.Tag) : TagWriter(tag) {
    abstract class Log(tag: Xml.Tag) : Op(tag) {
        fun and(init: Log.() -> Unit) = addChild(And(), init)
        fun or(init: Log.() -> Unit) = addChild(Or(), init)
        fun not(init: Log.() -> Unit) = addChild(Not(), init)
        fun eq(init: Comp.() -> Unit) = addChild(Eq(), init)
        fun lt(init: Comp.() -> Unit) = addChild(Lt(), init)
        fun gt(init: Comp.() -> Unit) = addChild(Gt(), init)
        fun lte(init: Comp.() -> Unit) = addChild(Lte(), init)
        fun gte(init: Comp.() -> Unit) = addChild(Gte(), init)
        fun like(init: Comp.() -> Unit) = addChild(Like(), init)
    }

    abstract class Comp(tag: Xml.Tag) : Op(tag) {
        fun prop(init: Prop.Writer.() -> Unit) = addChild(Prop.Writer(), init)

        fun literal(literal: Any) = addChild(TagWriter(TAG_LITERAL)) {
            addChild(TextWriter(literal.toString())) {}
        }
    }

    class And : Log(webDavTag("and"))
    class Or : Log(webDavTag("or"))
    class Not : Log(webDavTag("not"))

    class Eq : Comp(webDavTag("eq"))
    class Lt : Comp(webDavTag("lt"))
    class Gt : Comp(webDavTag("gt"))
    class Lte : Comp(webDavTag("lte"))
    class Gte : Comp(webDavTag("gte"))

    class Like : Comp(webDavTag("like"))
}

class Where : Op.Log(TAG_WHERE)

class BasicSearch : TagWriter(TAG_BASICSEARCH) {
    fun select(init: Select.() -> Unit) = addChild(Select(), init)
    fun from(init: From.() -> Unit) = addChild(From(), init)
    fun where(init: Where.() -> Unit) = addChild(Where(), init)
}

class SearchRequest : TagWriter(TAG_SEARCHREQUEST) {
    fun basicSearch(init: BasicSearch.() -> Unit) = addChild(BasicSearch(), init)
}
