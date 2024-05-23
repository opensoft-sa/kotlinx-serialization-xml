package pt.opensoft.kotlinx.serialization.xml.internal

import pt.opensoft.kotlinx.serialization.xml.NO_NAMESPACE_PREFIX
import pt.opensoft.kotlinx.serialization.xml.UnexpectedXmlTokenException

internal class XmlLexer(private val source: String) {
    private var position = 0

    private var lastToken: XmlToken? = null

    fun copy(): XmlLexer {
        val other = XmlLexer(source)
        other.position = position
        other.lastToken = lastToken
        return other
    }

    private fun next(): Char? = if (position < source.length) source[position++] else null

    private fun peek(): Char? = if (position < source.length) source.getOrNull(position) else null

    private fun skipToChar(char: Char) {
        var c = next()
        while (c != null) {
            when (c) {
                char -> return
                else -> c = next()
            }
        }
    }

    private fun skipWhitespace() {
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

    private fun requireChar(char: Char) {
        skipWhitespace()
        require(peek() != null) { "Unexpected end of file" }
        require(peek() == char) { "Unexpected token '${peek()}', expecting '$char'" }
    }

    fun readNextToken(): XmlToken? {
        when (lastToken) {
            is XmlToken.DocumentEnd -> return lastToken
            null,
            XmlToken.ElementStartEnd,
            is XmlToken.ElementEnd,
            is XmlToken.Text -> {
                while (true) {
                    skipWhitespace()
                    when (peek()) {
                        null -> return XmlToken.DocumentEnd.also { lastToken = it }
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
                                    return XmlToken.ElementEnd(elementName.name, elementName.prefix)
                                        .also { lastToken = it }
                                }
                                else -> {
                                    val elementName = readElementName()
                                    return XmlToken.ElementStart(
                                            elementName.name,
                                            elementName.prefix
                                        )
                                        .also { lastToken = it }
                                }
                            }
                        }
                        '/' -> {
                            skipToChar('>')
                            return XmlToken.ElementEnd()
                        }
                        else -> {
                            return XmlToken.Text(readText()).also { lastToken = it }
                        }
                    }
                }
            }
            is XmlToken.ElementStart,
            is XmlToken.AttributeValue,
            is XmlToken.AttributeEnd -> {
                while (true) {
                    skipWhitespace()
                    return when (peek()) {
                        '/' -> {
                            skipToChar('>')
                            XmlToken.ElementEnd()
                        }
                        '>' -> {
                            next() // consume the bracket
                            XmlToken.ElementStartEnd
                        }
                        else -> {
                            val qname = readAttributeName()
                            XmlToken.AttributeStart(qname.name, qname.prefix)
                        }
                    }.also { lastToken = it }
                }
            }
            is XmlToken.AttributeStart -> {
                skipWhitespace()
                return if (peek() == '=') {
                    position++
                    XmlToken.AttributeValue(readAttributeValue()).also { lastToken = it }
                } else {
                    XmlToken.AttributeEnd.also { lastToken = it }
                }
            }
        }
    }

    private fun readElementName(): PrefixedName {
        skipWhitespace()
        var start = position
        var prefix: String = NO_NAMESPACE_PREFIX
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
        return PrefixedName(source.substring(start, --position), prefix)
    }

    private fun readAttributeName(): PrefixedName {
        skipWhitespace()
        var start = position
        var prefix: String = NO_NAMESPACE_PREFIX
        while (true) {
            when (peek()) {
                null -> throw IllegalArgumentException("Unexpected end of file")
                ':' -> {
                    prefix = source.substring(start, position++)
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
        return PrefixedName(source.substring(start, position), prefix)
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
                '&' -> throw IllegalArgumentException("Invalid character in attribute name: '$c'")
                quote -> break
                else -> c = next()
            }
        }
        return source.substring(s, position - 1)
    }

    private fun readText(): String {
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

    private fun throwUnexpectedToken(expected: String): Nothing =
        throw UnexpectedXmlTokenException(
            position,
            "expected '$expected' but found '${peek() ?: "EOF"}'"
        )
}
