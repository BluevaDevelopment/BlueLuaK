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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile

class AntlrLuaParserTest {
    @Test
    fun parsesLua52Syntax() {
        val source = """
            #!/usr/bin/env lua
            local function sum(...)
                local values = {...}
                local result = 0
                for index = 1, #values do
                    result = result + values[index]
                end
                return result
            end

            ::again::
            local object = {
                name = [=[BlueLuaK]=],
                ["value"] = sum(1, 2, 3),
            }
            if object.value >= 6 and object.name ~= nil then
                object:run()
            else
                goto again
            end
        """.trimIndent()

        val chunk = LuaParser(source).Chunk()

        assertNotNull(chunk.block)
        assertEquals(4, chunk.block!!.stats.size)
    }

    @Test
    fun rejectsMalformedLua() {
        assertThrows(ParseException::class.java) {
            LuaParser("local value = )").Chunk()
        }
    }

    @Test
    fun parsesArbitraryLongBracketDelimiters() {
        val delimiter = "=".repeat(32)
        val source = """
            --[$delimiter[ comment with ]] and ]=] inside ]$delimiter]
            return [$delimiter[value with ]] and ]=] inside]$delimiter]
        """.trimIndent()

        assertNotNull(LuaParser(source).Chunk())
    }

    @Test
    fun parsesBundledLuaScripts() {
        val root = Path.of("src/test/resources/test/lua")
        Files.walk(root).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".lua") }
                .forEach { path ->
                    Files.newBufferedReader(path).use { reader ->
                        try {
                            LuaParser(reader.readText()).Chunk()
                        } catch (error: ParseException) {
                            throw AssertionError("Failed to parse $path: ${error.message}", error)
                        }
                    }
                }
        }
    }

    @Test
    fun parsesArchivedLuaSuite() {
        ZipFile("src/test/resources/test/lua/luaj3.0-tests.zip").use { archive ->
            archive.entries().asSequence()
                .filter { !it.isDirectory && it.name.endsWith(".lua") }
                .forEach { entry ->
                    archive.getInputStream(entry).reader().use { reader ->
                        try {
                            LuaParser(reader.readText()).Chunk()
                        } catch (error: ParseException) {
                            throw AssertionError("Failed to parse ${entry.name}: ${error.message}", error)
                        }
                    }
                }
        }
    }
}
