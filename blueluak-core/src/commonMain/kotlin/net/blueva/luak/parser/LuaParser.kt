/******************************************************************************
 *  ____  _            _                _  __
 * | __ )| |_   _  ___| |   _   _  __ _| |/ /
 * |  _ \| | | | |/ _ \ |  | | | |/ _` | ' /
 * | |_) | | |_| |  __/ |__| |_| | (_| | . \
 * |____/|_|\__,_|\___|_____\__,_|\__,_|_|\_\
 *
 *  BlueLuaK
 *  https://github.com/BluevaDevelopment/BlueLuaK
 *
 *  Copyright (c) 2026 Blueva Development
 *
 *  SPDX-License-Identifier: MIT
 ******************************************************************************/
package net.blueva.luak.parser

import net.blueva.luak.ast.Chunk
import net.blueva.luak.parser.antlr.LuaLexer
import net.blueva.luak.parser.antlr.LuaParser as AntlrLuaParser
import org.antlr.v4.kotlinruntime.BaseErrorListener
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.RecognitionException
import org.antlr.v4.kotlinruntime.Recognizer
class LuaParser(
    private val source: String,
) {
    @Throws(ParseException::class)
    fun Chunk(): Chunk {
        try {
            val lexer = LuaLexer(CharStreams.fromString(source))
            lexer.removeErrorListeners()
            lexer.addErrorListener(ThrowingErrorListener)

            val parser = AntlrLuaParser(CommonTokenStream(lexer))
            parser.removeErrorListeners()
            parser.addErrorListener(ThrowingErrorListener)

            return LuaAstBuilder().chunk(parser.chunk())
        } catch (e: ParseException) {
            throw e
        } catch (e: Exception) {
            throw ParseException(e.message ?: "Unable to parse Lua source", e)
        }
    }

    private object ThrowingErrorListener : BaseErrorListener() {
        override fun syntaxError(
            recognizer: Recognizer<*, *>,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException?,
        ) {
            throw ParseException("line $line:${charPositionInLine + 1} $msg", e)
        }
    }
}
