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
package net.blueva.luak.lib.jse

import net.blueva.luak.LuaValue
import net.blueva.luak.lib.MathLib
import net.blueva.luak.lib.TwoArgFunction
import kotlin.math.ln
import kotlin.math.pow

/**
 * Subclass of [LibFunction] which implements the lua standard `math`
 * library.
 * 
 * 
 * It contains all lua math functions, including those not available on the JME platform.
 * See [MathLib] for the exception list.
 * 
 * 
 * Typically, this library is included as part of a call to
 * [JsePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals(); System.out.println( globals.get("math").get("sqrt").call( LuaValue.valueOf(2) ) ); ` </pre>
 * 
 * 
 * For special cases where the smallest possible footprint is desired,
 * a minimal set of libraries could be loaded
 * directly via [Globals.load] using code such as:
 * <pre> `Globals globals = new Globals(); globals.load(new JseBaseLib()); globals.load(new PackageLib()); globals.load(new JseMathLib()); System.out.println( globals.get("math").get("sqrt").call( LuaValue.valueOf(2) ) ); ` </pre>
 * 
 * However, other libraries such as *CoroutineLib* are not loaded in this case.
 * 
 * 
 * This has been implemented to match as closely as possible the behavior in the corresponding library in C.
 * @see LibFunction
 * 
 * @see JsePlatform
 * 
 * @see net.blueva.luak.lib.jme.JmePlatform
 * 
 * @see JseMathLib
 * 
 * @see [Lua 5.2 Math Lib Reference](http://www.lua.org/manual/5.2/manual.html.6.6)
 */
class JseMathLib : MathLib() {
    /** Perform one-time initialization on the library by creating a table
     * containing the library functions, adding that table to the supplied environment,
     * adding the table to package.loaded, and returning table as the return value.
     * <P>Specifically, adds all library functions that can be implemented directly
     * in JSE but not JME: acos, asin, atan, atan2, cosh, exp, log, pow, sinh, and tanh.
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env the environment to load into, which must be a Globals instance.
    </P> */
    override fun call(modname: LuaValue?, env: LuaValue?): LuaValue {
        super.call(modname, env)
        val math: LuaValue = env!!.get("math")!!
        math.set("acos", acos())
        math.set("asin", asin())
        val atan: LuaValue = atan2()
        math.set("atan", atan)
        math.set("atan2", atan)
        math.set("cosh", cosh())
        math.set("exp", exp())
        math.set("log", log())
        math.set("pow", pow())
        math.set("sinh", sinh())
        math.set("tanh", tanh())
        return math
    }

    private class acos : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.acos(d)
        }
    }

    private class asin : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.asin(d)
        }
    }

    internal class atan2 : TwoArgFunction() {
        override fun call(x: LuaValue?, y: LuaValue?): LuaValue? {
            return valueOf(kotlin.math.atan2(x!!.checkdouble(), y!!.optdouble(1.0)))
        }
    }

    private class cosh : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.cosh(d)
        }
    }

    private class exp : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.exp(d)
        }
    }

    internal class log : TwoArgFunction() {
        override fun call(x: LuaValue?, base: LuaValue?): LuaValue? {
            var nat = ln(x!!.checkdouble())
            val b = base!!.optdouble(Math.E)
            if (b != Math.E) nat /= ln(b)
            return valueOf(nat)
        }
    }

    private class pow : BinaryOp() {
        override fun call(x: Double, y: Double): Double {
            return x.pow(y)
        }
    }

    private class sinh : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.sinh(d)
        }
    }

    private class tanh : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.tanh(d)
        }
    }

    /** Faster, better version of pow() used by arithmetic operator ^  */
    override fun dpow_lib(a: Double, b: Double): Double {
        return a.pow(b)
    }
}
