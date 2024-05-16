package pt.opensoft.kotlinx.serialization.xml.internal

internal class XmlLexer(private val source: String) {
    var position = 0

    private var lastToken: Token = Token.None

    fun copy(): XmlLexer {
        val other = XmlLexer(source)
        other.position = position
        other.lastToken = lastToken
        return other
    }

    fun next(): Char? = if (position < source.length) source[position++] else null

    fun peek(): Char? = if (position < source.length) source[position] else null

    fun skipToChar(char: Char) {
        var c = next()
        while (c != null) {
            when (c) {
                char -> return
                else -> c = next()
            }
        }
    }

    fun skipWhitespace() {
        var c = peek()
        while (c != null) {
            when (c) {
                ' ',
                '\n',
                '\t',
                '\r' -> {
                    next()
                    c = peek()
                }
                else -> return
            }
        }
    }

    fun requireChar(char: Char) {
        skipWhitespace()
        require(peek() != null) { "Unexpected end of file" }
        require(peek() == char) { "Unexpected token ${peek()}, expecting $char" }
    }

    data class QName(val local: String, val prefix: String? = null)

    private fun readElementName(): QName {
        skipWhitespace()
        var start = position
        var prefix: String? = null
        while (true) {
            when (next()) {
                null -> throw IllegalArgumentException("Unexpected end of file")
                ':' -> {
                    prefix = source.substring(start, position - 1)
                    start = position
                }
                '\r',
                '\n',
                '\t',
                ' ',
                '>',
                '/' -> break
            }
        }
        val local = source.substring(start, --position)
        return QName(local, prefix)
    }

    private fun readAttributeName(): QName {
        skipWhitespace()
        var start = position
        var namespace: String? = null
        while (true) {
            when (peek()) {
                null -> throw IllegalArgumentException("Unexpected end of file")
                ':' -> {
                    namespace = source.substring(start, position++)
                    start = position
                }
                '\r',
                '\t',
                '\n',
                ' ',
                '=' -> break
                else -> position++
            }
        }
        val name = source.substring(start, position)
        return QName(name, namespace)
    }

    private fun readAttributeValue(): String {
        skipWhitespace()
        var quote = next()
        while (quote != null && quote != '\'' && quote != '"') {
            quote = next()
        }
        requireNotNull(quote) { "Unexpected end of file" }

        val s = position
        var c = next()
        while (true) {
            when (c) {
                null -> throw IllegalArgumentException("Unexpected end of file")
                '<',
                '&' -> throw IllegalArgumentException("Invalid character in attribute name: $c")
                quote -> break
                else -> c = next()
            }
        }
        return source.substring(s, position - 1)
    }

    fun readText(): String {
        skipWhitespace()
        val text = StringBuilder()
        while (true) {
            when (val c = next()) {
                null -> throw IllegalArgumentException("Unexpected end of file")
                '<' -> {
                    if (peek() == '!' && source.substring(position, position + 8) == "![CDATA[") {
                        position += 8
                        val end = source.indexOf("]]>", position)
                        text.append(source.substring(position, end))
                        position = end + 3
                    } else {
                        position--
                        break
                    }
                }
                else -> text.append(c)
            }
        }
        return text.toString().trim()
    }

    fun readNextToken(): Token {
        when (lastToken) {
            is Token.DocumentEnd -> return lastToken
            is Token.None,
            Token.ElementStartEnd,
            is Token.ElementEnd,
            is Token.Text -> {
                skipWhitespace()
                while (true) {
                    skipWhitespace()
                    when (peek()) {
                        null -> return Token.DocumentEnd.also { lastToken = it }
                        '<' -> {
                            next() // consume the bracket
                            when (peek()) {
                                '!',
                                '?' -> {
                                    skipToChar('>')
                                }
                                '/' -> {
                                    next() // consume the slash
                                    val elementName = readElementName()
                                    skipWhitespace()
                                    next() // Consume the closing bracket
                                    return Token.ElementEnd(elementName.local, elementName.prefix)
                                        .also { lastToken = it }
                                }
                                else -> {
                                    val elementName = readElementName()
                                    return Token.ElementStart(elementName.local, elementName.prefix)
                                        .also { lastToken = it }
                                }
                            }
                        }
                        '/' -> {
                            skipToChar('>')
                            return Token.ElementEnd()
                        }
                        else -> {
                            return Token.Text(readText()).also { lastToken = it }
                        }
                    }
                }
            }
            is Token.ElementStart,
            is Token.AttributeValue,
            is Token.AttributeEnd -> {
                while (true) {
                    skipWhitespace()
                    return when (peek()) {
                        '/' -> {
                            skipToChar('>')
                            Token.ElementEnd()
                        }
                        '>' -> {
                            next() // consume the bracket
                            Token.ElementStartEnd
                        }
                        else -> {
                            val qname = readAttributeName()
                            Token.AttributeName(qname.local, qname.prefix)
                        }
                    }.also { lastToken = it }
                }
            }
            is Token.AttributeName -> {
                skipWhitespace()
                return if (peek() == '=') {
                    position++
                    Token.AttributeValue(readAttributeValue()).also { lastToken = it }
                } else {
                    Token.AttributeEnd.also { lastToken = it }
                }
            }
        }
    }

    sealed interface Token {
        object None : Token

        data class ElementStart(val name: String, val prefix: String? = null) : Token

        object ElementStartEnd : Token

        data class ElementEnd(val name: String? = null, val prefix: String? = null) : Token

        data class AttributeName(val name: String, val prefix: String? = null) : Token

        data class AttributeValue(val value: String) : Token

        object AttributeEnd : Token

        data class Text(val content: String) : Token

        object DocumentEnd : Token
    }
}
