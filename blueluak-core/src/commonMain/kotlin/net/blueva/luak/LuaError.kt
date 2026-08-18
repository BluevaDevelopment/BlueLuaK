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


/**
 * RuntimeException that is thrown and caught in response to a lua error.
 * 
 * 
 * [LuaError] is used wherever a lua call to `error()`
 * would be used within a script.
 * 
 * 
 * Since it is an unchecked exception inheriting from [RuntimeException],
 * Java method signatures do notdeclare this exception, althoug it can
 * be thrown on almost any BlueLuaK operation.
 * This is analagous to the fact that any lua script can throw a lua error at any time.
 * 
 * 
 * The LuaError may be constructed with a message object, in which case the message
 * is the string representation of that object.  getMessageObject will get the object
 * supplied at construct time, or a LuaString containing the message of an object
 * was not supplied.
 */
class LuaError : RuntimeException {
    internal var level: Int

    internal var fileline: String? = null

    internal var traceback: String? = null

    /**
     * Get the cause, if any.
     */
    override var cause: Throwable? = null
        protected set

    private var `object`: LuaValue? = null

    /**
     * Set by the interpreter when it can enrich a raw "bad argument" message
     * with the argument index and/or calling function name, e.g. turning
     * "bad argument: string expected, got boolean" into
     * "bad argument #1 to 'tonumber' (string expected, got boolean)".
     * Takes precedence over the constructor-supplied message, but not over [traceback].
     */
    internal var argMessageOverride: String? = null

    override val message: String?
        /** Get the string message if it was supplied, or a string
         * representation of the message object if that was supplied.
         */
        get() {
            if (traceback != null) return traceback
            val m: String? = argMessageOverride ?: super.message
            if (m == null) return null
            if (fileline != null) return fileline.toString() + " " + m
            return m
        }

    val messageObject: LuaValue?
        /** Get the LuaValue that was provided in the constructor, or
         * a LuaString containing the message if it was a string error argument.
         * @return LuaValue which was used in the constructor, or a LuaString
         * containing the message.
         */
        get() {
            if (`object` != null) return `object`
            val m = this.message
            return if (m != null) LuaValue.valueOf(m) else null
        }

    /** Construct LuaError when a program exception occurs.
     * 
     * 
     * All errors generated from lua code should throw LuaError(String) instead.
     * @param cause the Throwable that caused the error, if known.
     */
    constructor(cause: Throwable?) : super("vm error: " + cause) {
        this.cause = cause
        this.level = 1
    }

    /**
     * Construct a LuaError with a specific message.
     * 
     * @param message message to supply
     */
    constructor(message: String?) : super(message) {
        this.level = 1
    }

    /** Construct a LuaError with a message and the exception that caused it. */
    constructor(message: String?, cause: Throwable?) : super(message) {
        this.cause = cause
        this.level = 1
    }

    /**
     * Construct a LuaError with a message, and level to draw line number information from.
     * @param message message to supply
     * @param level where to supply line info from in call stack
     */
    constructor(message: String?, level: Int) : super(message) {
        this.level = level
    }

    /**
     * Construct a LuaError with a LuaValue as the message object,
     * and level to draw line number information from.
     * @param message_object message string or object to supply
     */
    constructor(message_object: LuaValue) : super(message_object.tojstring()) {
        this.`object` = message_object
        this.level = 1
    }


    companion object {
        private const val serialVersionUID = 1L
    }
}
