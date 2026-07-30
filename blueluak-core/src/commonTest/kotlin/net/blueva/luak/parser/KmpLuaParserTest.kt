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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import net.blueva.luak.ast.Stat

class KmpLuaParserTest {
    @Test
    fun parsesLua52ChunkOnEveryTarget() {
        val chunk = LuaParser(
            """
            local total = 0
            for index = 1, 4 do
                total = total + index
            end
            return total
            """.trimIndent()
        ).Chunk()

        assertNotNull(chunk.block)
        assertEquals(3, chunk.block!!.stats?.size)
        assertEquals(true, chunk.block!!.stats?.last() is Stat.Return)
    }

    @Test
    fun rejectsMalformedLuaOnEveryTarget() {
        assertFailsWith<ParseException> {
            LuaParser("local value = ").Chunk()
        }
    }

    @Test
    fun supportsLongBracketDelimitersOnEveryTarget() {
        val delimiter = "=".repeat(16)
        val chunk = LuaParser("return [$delimiter[portable]$delimiter]").Chunk()
        assertNotNull(chunk.block)
    }
}
