package pt.opensoft.kotlinx.serialization.xml.internal

import pt.opensoft.kotlinx.serialization.xml.Xml

private const val DEFAULT_SB_CAPACITY = 128

internal fun Composer(xml: Xml): Composer {
    val sb = StringBuilder(DEFAULT_SB_CAPACITY)
    return if (xml.configuration.prettyPrint)
        ComposerWithPrettyPrint(sb, xml.configuration.prettyPrintIndent)
    else Composer(sb)
}

internal fun Composer(from: Composer): Composer {
    val sb = StringBuilder(DEFAULT_SB_CAPACITY)
    return if (from is ComposerWithPrettyPrint) ComposerWithPrettyPrint(sb, from) else Composer(sb)
}

internal open class Composer(internal val sb: StringBuilder) {
    open fun indent(): Composer = this

    open fun unIndent(): Composer = this

    open fun newElement(): Composer = this

    open fun newAttribute(): Composer = also { sb.append(" ") }

    open fun appendLine() = this

    fun append(composer: Composer): Composer = also { sb.append(composer.sb) }

    fun append(value: String): Composer = also { sb.append(value) }

    // TODO: Escaping
    fun appendQuoted(value: String): Composer = also { sb.append("\"$value\"") }

    override fun toString() = sb.toString()
}

internal class ComposerWithPrettyPrint(
    sb: StringBuilder,
    private val indent: String,
    private var level: Int = 0
) : Composer(sb) {
    internal constructor(
        sb: StringBuilder,
        composer: ComposerWithPrettyPrint
    ) : this(sb, composer.indent, composer.level)

    override fun indent(): Composer = also { level++ }

    override fun unIndent(): Composer = also { level-- }

    override fun newElement(): Composer = also { appendLine() }

    override fun newAttribute(): Composer = also { appendLine().append(indent) }

    override fun appendLine(): Composer = also {
        sb.appendLine()
        sb.append(indent.repeat(level))
    }
}
