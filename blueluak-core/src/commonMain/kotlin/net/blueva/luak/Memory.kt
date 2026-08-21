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
 *  SPDX-License-Identifier: MIT
 ******************************************************************************/
package net.blueva.luak

/**
 * What `collectgarbage("count")` answers with.
 *
 * The host's own collector is the one that reclaims memory here, and what it
 * reports - a heap shared with everything else the host is doing - says
 * nothing about how much of it is Lua's. So Lua's own objects are counted as
 * they are made, the way a reference build counts what it allocates, and the
 * tally goes back to nothing when a collection finishes: what is left after
 * one is not known object by object, and a program that watches this number
 * is watching it grow with what it allocates and drop when that is reclaimed.
 *
 * The sizes are the ones a reference build would use, so a program that works
 * out how much a table of a given shape costs gets the answer it expects.
 */
internal object Memory {
    /** What a table costs before any of its storage. */
    const val TABLE: Long = 56

    /** One slot of a table's array part. */
    const val SLOT: Long = 16

    /** One entry of a table's hash part. */
    const val NODE: Long = 32

    /** What a string costs beyond its own bytes. */
    const val STRING: Long = 24

    /** What a function written in Lua costs before its upvalues. */
    const val CLOSURE: Long = 32

    /** One upvalue of such a function. */
    const val UPVALUE: Long = 8

    /** What Lua holds with nothing allocated, so a count is never nothing. */
    private const val BASE: Long = 32 * 1024

    /** Where the collector would have run of its own accord. */
    private const val THRESHOLD: Long = 1024 * 1024

    /** Bytes of Lua's own objects made since the last collection. */
    var accounted: Long = 0
        private set

    /**
     * Bytes made since the host was last asked to collect.
     *
     * Unlike [accounted] this is not reset by a cycle going by on its own:
     * it says how much has been allocated since anything was actually
     * reclaimed, which is what decides when a program waiting on a finalizer
     * is worth interrupting for.
     */
    var sincecollect: Long = 0
        private set

    /** How much may be allocated before the host is asked to collect. */
    const val COLLECT_EVERY: Long = 1024 * 1024

    /** False while `collectgarbage("stop")` is in force. */
    var running: Boolean = true

    /** Notes [bytes] just allocated, collecting if that is now overdue. */
    fun account(bytes: Long) {
        accounted += bytes
        sincecollect += bytes
        // The host reclaims on its own; what happens here is only that the
        // tally starts again, which is what a finished cycle looks like from
        // a program watching the count.
        if (running && accounted > THRESHOLD) accounted = 0
    }

    /**
     * Takes back [bytes] just counted, for something that is not an object.
     *
     * A reference build keeps a named vararg parameter on the stack rather
     * than in an object of its own, so what stands in for it here is not
     * something a program should see the cost of.
     */
    fun uncount(bytes: Long) {
        accounted -= bytes
        if (accounted < 0) accounted = 0
    }

    /** Ends a collection cycle: nothing made since the last one still counts. */
    fun collected() {
        accounted = 0
        sincecollect = 0
    }

    /** Bytes in use, as `collectgarbage("count")` reports them. */
    fun used(): Long = BASE + accounted
}
