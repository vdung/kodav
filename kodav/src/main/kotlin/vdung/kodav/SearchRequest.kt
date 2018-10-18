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

data class Select(val props: List<Xml.Tag>) {
    class Writer : TagWriter(TAG_SELECT) {
        fun prop(init: Prop.Writer.() -> Unit) = addChild(Prop.Writer(), init)
    }
}

data class Scope(val href: String, val depth: Int?) {
    class Writer : TagWriter(TAG_SCOPE) {
        fun href(href: String?) = addChild(TagWriter(TAG_HREF)) {
            addChild(TextWriter(href)) {}
        }

        fun depth(depth: Int?) = addChild(TagWriter(TAG_DEPTH)) {
            addChild(TextWriter(depth?.toString() ?: "infinity")) {}
        }
    }
}

data class From(val scopes: List<Scope>) {
    class Writer : TagWriter(TAG_FROM) {
        fun scope(init: Scope.Writer.() -> Unit) = addChild(Scope.Writer(), init)
    }
}

sealed class Op {
    open class Writer(tag: Xml.Tag) : TagWriter(tag) {
        fun and(init: Op.Writer.() -> Unit) = addChild(Writer(LogOp.Name.AND.tag), init)
        fun or(init: Op.Writer.() -> Unit) = addChild(Writer(LogOp.Name.OR.tag), init)
        fun not(init: Op.Writer.() -> Unit) = addChild(Writer(LogOp.Name.NOT.tag), init)
        fun eq(init: PropLiteral.Writer.() -> Unit) = addChild(PropLiteral.Writer(CompOp.Name.EQ.tag), init)
        fun lt(init: PropLiteral.Writer.() -> Unit) = addChild(PropLiteral.Writer(CompOp.Name.LT.tag), init)
        fun gt(init: PropLiteral.Writer.() -> Unit) = addChild(PropLiteral.Writer(CompOp.Name.GT.tag), init)
        fun lte(init: PropLiteral.Writer.() -> Unit) = addChild(PropLiteral.Writer(CompOp.Name.LTE.tag), init)
        fun gte(init: PropLiteral.Writer.() -> Unit) = addChild(PropLiteral.Writer(CompOp.Name.GTE.tag), init)
    }
}


data class PropLiteral(val prop: Xml.Tag, val literal: String) {
    open class Writer(tag: Xml.Tag) : TagWriter(tag) {
        fun prop(init: Prop.Writer.() -> Unit) = addChild(Prop.Writer(), init)
        fun literal(literal: String) = addChild(TagWriter(TAG_LITERAL)) {
            addChild(TextWriter(literal)) {}
        }
    }
}

data class LogOp(val name: Name, val ops: List<Op>) {
    enum class Name {
        AND, OR, NOT;

        val tag get() = Xml.Tag(NS_DAV, name.toLowerCase())
    }
}


data class CompOp(val name: Name, val propLiteral: PropLiteral) : Op() {
    enum class Name {
        EQ, LT, GT, LTE, GTE;

        val tag get() = Xml.Tag(NS_DAV, name.toLowerCase())
    }
}

data class Where(val op: Op) {
    class Writer : Op.Writer(TAG_WHERE)
}

data class BasicSearch(val select: Select, val from: From, val where: Where) {
    class Writer : TagWriter(TAG_BASICSEARCH) {
        fun select(init: Select.Writer.() -> Unit) = addChild(Select.Writer(), init)
        fun from(init: From.Writer.() -> Unit) = addChild(From.Writer(), init)
        fun where(init: Where.Writer.() -> Unit) = addChild(Where.Writer(), init)
    }
}

data class SearchRequest(val basicSearch: BasicSearch) {
    class Writer : TagWriter(TAG_SEARCHREQUEST) {
        fun basicSearch(init: BasicSearch.Writer.() -> Unit) = addChild(BasicSearch.Writer(), init)
    }
}
