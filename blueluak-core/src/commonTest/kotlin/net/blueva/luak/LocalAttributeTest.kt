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
package net.blueva.luak

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import net.blueva.luak.lib.LuaPlatform

/**
 * Local variable attributes, `local x <const>` and `local x <close>`, from
 * Lua 5.4.
 *
 * `<const>` is implemented. `<close>` needs to-be-closed variable support in
 * the VM (upstream's `OP_TBC` and `OP_CLOSE`), which the port has not reached;
 * until then it is rejected with a message that says so rather than being
 * accepted and quietly ignored, which would lose resource cleanup with no
 * warning.
 */
class LocalAttributeTest {
    private lateinit var globals: Globals

    @BeforeTest
    fun buildGlobals() {
        globals = LuaPlatform.standardGlobals()
    }

    /** Compiles [source], returning the error message if it does not compile. */
    private fun compileError(source: String): String? = try {
        globals.load(source, "attribute-test")
        null
    } catch (failure: LuaError) {
        failure.message
    }

    @Test
    fun constLocalsBehaveLikeOrdinaryLocals() {
        val script = """
            local answer <const> = 42
            local other <const>, plain = 1, 2
            return answer, answer + 1, other + plain
        """.trimIndent()
        val result = globals.load(script, "const-read")!!.invoke()
        assertEquals(42L, result.arg(1).tolong())
        assertEquals(43L, result.arg(2).tolong())
        assertEquals(3L, result.arg(3).tolong())
    }

    @Test
    fun assigningToAConstLocalIsACompileError() {
        val message = compileError("local x <const> = 42; x = 1")
            ?: fail("assigning to a const local must not compile")
        assertTrue(
            message.contains("const variable") && message.contains("'x'"),
            "message should name the variable, was: $message",
        )
    }

    @Test
    fun assigningToAConstLocalIsCaughtInAMultipleAssignment() {
        val message = compileError("local a <const> = 1; local b = 2; b, a = 3, 4")
            ?: fail("assigning to a const local must not compile")
        assertTrue(message.contains("const variable"), message)
    }

    @Test
    fun aPlainLocalIsStillAssignable() {
        val script = """
            local x = 1
            x = x + 1
            return x
        """.trimIndent()
        assertEquals(2L, globals.load(script, "plain-local")!!.call()!!.tolong())
    }

    @Test
    fun unknownAttributesAreRejected() {
        val message = compileError("local x <bogus> = 1")
            ?: fail("an unknown attribute must not compile")
        assertTrue(message.contains("unknown attribute") && message.contains("bogus"), message)
    }

    @Test
    fun closeIsRejectedWithAnExplicitNotImplementedMessage() {
        val message = compileError("local x <close> = nil")
            ?: fail("<close> must not compile while the VM cannot honour it")
        assertTrue(
            message.contains("close") && message.contains("not implemented"),
            "message should say the feature is missing, was: $message",
        )
    }
}
