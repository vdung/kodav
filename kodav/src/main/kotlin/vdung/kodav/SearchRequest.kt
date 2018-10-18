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
    fun href(href: String?) = addChild(TagWriter(TAG_HREF)) {
        addChild(TextWriter(href)) {}
    }

    fun depth(depth: Int?) = addChild(TagWriter(TAG_DEPTH)) {
        addChild(TextWriter(depth?.toString() ?: "infinity")) {}
    }
}

class From : TagWriter(TAG_FROM) {
    fun scope(init: Scope.() -> Unit) = addChild(Scope(), init)
}

abstract class Op(tag: Xml.Tag) : TagWriter(tag) {
    abstract class Log(tag: Xml.Tag) : Op(tag) {
        class And : Log(webDavTag("and"))
        class Or : Log(webDavTag("or"))
        class Not : Log(webDavTag("not"))

        fun and(init: Op.() -> Unit) = addChild(And(), init)
        fun or(init: Op.() -> Unit) = addChild(Or(), init)
        fun not(init: Op.() -> Unit) = addChild(Not(), init)
        fun eq(init: Comp.() -> Unit) = addChild(Comp.Eq(), init)
        fun lt(init: Comp.() -> Unit) = addChild(Comp.Lt(), init)
        fun gt(init: Comp.() -> Unit) = addChild(Comp.Gt(), init)
        fun lte(init: Comp.() -> Unit) = addChild(Comp.Lte(), init)
        fun gte(init: Comp.() -> Unit) = addChild(Comp.Gte(), init)
    }

    abstract class Comp(tag: Xml.Tag) : TagWriter(tag) {
        class Eq : Comp(webDavTag("eq"))
        class Lt : Comp(webDavTag("lt"))
        class Gt : Comp(webDavTag("gt"))
        class Lte : Comp(webDavTag("lte"))
        class Gte : Comp(webDavTag("gte"))

        fun prop(init: Prop.Writer.() -> Unit) = addChild(Prop.Writer(), init)

        fun literal(literal: String) = addChild(TagWriter(TAG_LITERAL)) {
            addChild(TextWriter(literal)) {}
        }
    }
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
