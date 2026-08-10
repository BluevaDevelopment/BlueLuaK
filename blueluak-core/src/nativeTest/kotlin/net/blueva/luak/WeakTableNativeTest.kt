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
 *  Based on LuaJ (https://luaj.org)
 *  Original work Copyright (c) 2009 Luaj.org
 *  Modifications Copyright (c) 2026 Blueva Development
 *
 *  SPDX-License-Identifier: MIT
 ******************************************************************************/
package net.blueva.luak

import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi
import kotlin.test.Test
import kotlin.test.assertEquals

// Native-specific: WeakTable (commonMain) relies on the expect/actual
// WeakReference, which was a permanently-strong stub on Native until now.
// These confirm real weak-table semantics work end to end under Kotlin/
// Native's memory manager, using GC.collect() for a deterministic collection
// instead of a JVM-style retry/sleep loop.
class WeakTableNativeTest {
    // Dropped entries must only ever be locals of a function that has already
    // returned by the time GC.collect() runs - see WeakReferenceNativeTest
    // for why keeping them in the @Test body itself is unreliable.
    private fun populateWithDroppableValue(t: LuaTable) {
        val value: LuaValue = LuaValue.userdataOf(Any())
        t.set("key", value)
        assertEquals(value, t.get("key"))
    }

    @OptIn(NativeRuntimeApi::class)
    @Test
    fun weakValuesAreDroppedAfterCollection() {
        val t = WeakTable.make(false, true)
        populateWithDroppableValue(t)

        GC.collect()

        assertEquals(LuaValue.NIL, t.get("key"))
    }

    private fun populateWithDroppableKey(t: LuaTable) {
        val key: LuaValue = LuaValue.userdataOf(Any())
        t.set(key, LuaValue.valueOf("value"))
        assertEquals(LuaValue.valueOf("value"), t.get(key))
    }

    @OptIn(NativeRuntimeApi::class)
    @Test
    fun weakKeysAreDroppedAfterCollection() {
        val t = WeakTable.make(true, false)
        populateWithDroppableKey(t)

        GC.collect()

        var size = 0
        var k: LuaValue = t.next(LuaValue.NIL).arg1()!!
        while (!k.isnil()) {
            size++
            k = t.next(k).arg1()!!
        }
        assertEquals(0, size)
    }
}
