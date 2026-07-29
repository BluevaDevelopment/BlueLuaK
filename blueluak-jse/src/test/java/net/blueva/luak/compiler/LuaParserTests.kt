package net.blueva.luak.compiler

import net.blueva.luak.LuaValue
import net.blueva.luak.parser.LuaParser
import java.io.InputStreamReader
import java.io.Reader

class LuaParserTests : CompilerUnitTests() {
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        LuaValue.valueOf(true)
    }

    override fun doTest(file: String?) {
        try {
            val `is` = inputStreamOfFile(file)
            val r: Reader = InputStreamReader(`is`, "ISO-8859-1")
            val parser = LuaParser(r)
            parser.Chunk()
        } catch (e: Exception) {
            fail(e.message)
            e.printStackTrace()
        }
    }
}
