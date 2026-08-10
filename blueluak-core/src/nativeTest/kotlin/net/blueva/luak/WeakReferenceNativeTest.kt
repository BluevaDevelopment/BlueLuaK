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

import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WeakReferenceNativeTest {
    private class Payload(val value: Int)

    // The referent is only ever local to this function, so its stack slot is
    // fully gone by the time it returns - unlike keeping "var strong: Payload?"
    // in the test body itself, where the compiler's liveness analysis can keep
    // the slot (and the object) rooted for the rest of that frame even after
    // assigning null to it, the same reason JVM weak-ref tests hide object
    // creation behind a helper call too.
    private fun weakRefToUnreachablePayload(): WeakReference<Payload> {
        val strong = Payload(42)
        val weak = WeakReference(strong)
        assertNotNull(weak.get())
        return weak
    }

    // GC.collect() is documented to trigger a collection and wait for its
    // completion, so a single call is enough - no retry/sleep loop needed.
    @OptIn(NativeRuntimeApi::class)
    @Test
    fun getReturnsNullOnceTheReferentIsUnreachable() {
        val weak = weakRefToUnreachablePayload()
        GC.collect()
        assertNull(weak.get())
    }
}
