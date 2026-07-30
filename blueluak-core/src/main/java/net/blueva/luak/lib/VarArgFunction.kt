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
package net.blueva.luak.lib

import net.blueva.luak.LuaValue
import net.blueva.luak.Varargs

/** Abstract base class for Java function implementations that takes varaiable arguments and
 * returns multiple return values.
 * 
 * 
 * Subclasses need only implement [LuaValue.invoke] to complete this class,
 * simplifying development.
 * All other uses of [.call], [.invoke],etc,
 * are routed through this method by this class,
 * converting arguments to [Varargs] and
 * dropping or extending return values with `nil` values as required.
 * 
 * 
 * If between one and three arguments are required, and only one return value is returned,
 * [ZeroArgFunction], [OneArgFunction], [TwoArgFunction], or [ThreeArgFunction].
 * 
 * 
 * See [LibFunction] for more information on implementation libraries and library functions.
 * @see .invoke
 * @see LibFunction
 * 
 * @see ZeroArgFunction
 * 
 * @see OneArgFunction
 * 
 * @see TwoArgFunction
 * 
 * @see ThreeArgFunction
 */
abstract class VarArgFunction : LibFunction() {
    override fun call(): LuaValue? {
        return (invoke(NONE).arg1())!!
    }

    override fun call(arg: LuaValue?): LuaValue? {
        return (invoke(arg).arg1())!!
    }

    override fun call(arg1: LuaValue?, arg2: LuaValue?): LuaValue? {
        return (invoke(varargsOf(arg1, (arg2)!!)).arg1())!!
    }

    override fun call(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?): LuaValue? {
        return (invoke(varargsOf(arg1, arg2, (arg3)!!)).arg1())!!
    }

    /**
     * Subclass responsibility.
     * May not have expected behavior for tail calls.
     * Should not be used if:
     * - function has a possibility of returning a TailcallVarargs
     * @param args the arguments to the function call.
     */
    open fun invoke(args: Varargs?): Varargs {
        return onInvoke(args).eval()
    }

    fun onInvoke(args: Varargs?): Varargs {
        return invoke(args)
    }
}
