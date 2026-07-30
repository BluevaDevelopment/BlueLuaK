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

import net.blueva.luak.LuaDouble
import net.blueva.luak.LuaTable
import net.blueva.luak.LuaValue
import net.blueva.luak.Varargs
import java.util.Random

/**
 * Subclass of [LibFunction] which implements the lua standard `math`
 * library.
 * 
 * 
 * It contains only the math library support that is possible on JME.
 * For a more complete implementation based on math functions specific to JSE
 * use [net.blueva.luak.lib.jse.JseMathLib].
 * In Particular the following math functions are **not** implemented by this library:
 * 
 *  * acos
 *  * asin
 *  * atan
 *  * cosh
 *  * log
 *  * sinh
 *  * tanh
 *  * atan2
 * 
 * 
 * 
 * The implementations of `exp()` and `pow()` are constructed by
 * hand for JME, so will be slower and less accurate than when executed on the JSE platform.
 * 
 * 
 * Typically, this library is included as part of a call to either
 * [net.blueva.luak.lib.jse.JsePlatform.standardGlobals] or
 * [net.blueva.luak.lib.jme.JmePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals(); System.out.println( globals.get("math").get("sqrt").call( LuaValue.valueOf(2) ) ); ` </pre>
 * When using [net.blueva.luak.lib.jse.JsePlatform] as in this example,
 * the subclass [net.blueva.luak.lib.jse.JseMathLib] will
 * be included, which also includes this base functionality.
 * 
 * 
 * To instantiate and use it directly,
 * link it into your globals table via [LuaValue.load] using code such as:
 * <pre> `Globals globals = new Globals(); globals.load(new JseBaseLib()); globals.load(new PackageLib()); globals.load(new MathLib()); System.out.println( globals.get("math").get("sqrt").call( LuaValue.valueOf(2) ) ); ` </pre>
 * Doing so will ensure the library is properly initialized
 * and loaded into the globals table.
 * 
 * 
 * This has been implemented to match as closely as possible the behavior in the corresponding library in C.
 * @see LibFunction
 * 
 * @see net.blueva.luak.lib.jse.JsePlatform
 * 
 * @see net.blueva.luak.lib.jme.JmePlatform
 * 
 * @see net.blueva.luak.lib.jse.JseMathLib
 * 
 * @see [Lua 5.2 Math Lib Reference](http://www.lua.org/manual/5.2/manual.html.6.6)
 */
open class MathLib : TwoArgFunction() {
    /** Construct a MathLib, which can be initialized by calling it with a
     * modname string, and a global environment table as arguments using
     * [.call].  */
    init {
        net.blueva.luak.lib.MathLib.Companion.MATHLIB = this
    }

    /** Perform one-time initialization on the library by creating a table
     * containing the library functions, adding that table to the supplied environment,
     * adding the table to package.loaded, and returning table as the return value.
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env the environment to load into, typically a Globals instance.
     */
    open fun call(modname: LuaValue?, env: LuaValue): LuaValue {
        val math: LuaTable = LuaTable(0, 30)
        math.set("abs", net.blueva.luak.lib.MathLib.abs())
        math.set("ceil", net.blueva.luak.lib.MathLib.ceil())
        math.set("cos", net.blueva.luak.lib.MathLib.cos())
        math.set("deg", net.blueva.luak.lib.MathLib.deg())
        math.set("exp", net.blueva.luak.lib.MathLib.exp(this))
        math.set("floor", net.blueva.luak.lib.MathLib.floor())
        math.set("fmod", net.blueva.luak.lib.MathLib.fmod())
        math.set("frexp", net.blueva.luak.lib.MathLib.frexp())
        math.set("huge", LuaDouble.POSINF)
        math.set("ldexp", net.blueva.luak.lib.MathLib.ldexp())
        math.set("max", net.blueva.luak.lib.MathLib.max())
        math.set("min", net.blueva.luak.lib.MathLib.min())
        math.set("modf", net.blueva.luak.lib.MathLib.modf())
        math.set("pi", Math.PI)
        math.set("pow", net.blueva.luak.lib.MathLib.pow())
        val r: random?
        math.set("random", net.blueva.luak.lib.MathLib.random().also { r = it })
        math.set("randomseed", net.blueva.luak.lib.MathLib.randomseed(r))
        math.set("rad", net.blueva.luak.lib.MathLib.rad())
        math.set("sin", net.blueva.luak.lib.MathLib.sin())
        math.set("sqrt", net.blueva.luak.lib.MathLib.sqrt())
        math.set("tan", net.blueva.luak.lib.MathLib.tan())
        env.set("math", math)
        if (!env.get("package").isnil()) env.get("package").get("loaded").set("math", math)
        return math
    }

    protected abstract class UnaryOp : OneArgFunction() {
        fun call(arg: LuaValue): LuaValue {
            return valueOf(call(arg.checkdouble()))
        }

        protected abstract fun call(d: Double): Double
    }

    protected abstract class BinaryOp : TwoArgFunction() {
        fun call(x: LuaValue, y: LuaValue): LuaValue {
            return valueOf(call(x.checkdouble(), y.checkdouble()))
        }

        protected abstract fun call(x: Double, y: Double): Double
    }

    internal class abs : UnaryOp() {
        override fun call(d: Double): Double {
            return Math.abs(d)
        }
    }

    internal class ceil : UnaryOp() {
        override fun call(d: Double): Double {
            return Math.ceil(d)
        }
    }

    internal class cos : UnaryOp() {
        override fun call(d: Double): Double {
            return Math.cos(d)
        }
    }

    internal class deg : UnaryOp() {
        override fun call(d: Double): Double {
            return Math.toDegrees(d)
        }
    }

    internal class floor : UnaryOp() {
        override fun call(d: Double): Double {
            return Math.floor(d)
        }
    }

    internal class rad : UnaryOp() {
        override fun call(d: Double): Double {
            return Math.toRadians(d)
        }
    }

    internal class sin : UnaryOp() {
        override fun call(d: Double): Double {
            return Math.sin(d)
        }
    }

    internal class sqrt : UnaryOp() {
        override fun call(d: Double): Double {
            return Math.sqrt(d)
        }
    }

    internal class tan : UnaryOp() {
        override fun call(d: Double): Double {
            return Math.tan(d)
        }
    }

    internal class exp(val mathlib: MathLib) : UnaryOp() {
        override fun call(d: Double): Double {
            return mathlib.dpow_lib(Math.E, d)
        }
    }

    internal class fmod : TwoArgFunction() {
        fun call(xv: LuaValue, yv: LuaValue): LuaValue {
            if (xv.islong() && yv.islong()) {
                return valueOf(xv.tolong() % yv.tolong())
            }
            return valueOf(xv.checkdouble() % yv.checkdouble())
        }
    }

    internal class ldexp : BinaryOp() {
        override fun call(x: Double, y: Double): Double {
            // This is the behavior on os-x, windows differs in rounding behavior.
            return x * Double.longBitsToDouble(((y.toLong()) + 1023) shl 52)
        }
    }

    internal class pow : BinaryOp() {
        override fun call(x: Double, y: Double): Double {
            return net.blueva.luak.lib.MathLib.Companion.dpow_default(x, y)
        }
    }

    internal class frexp : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val x: Double = args.checkdouble(1)
            if (x == 0.0) return varargsOf(ZERO, ZERO)
            val bits: Long = Double.doubleToLongBits(x)
            val m =
                ((bits and ((-1L shl 52).inv()).toLong()) + (1L shl 52)) * (if (bits >= 0) (.5 / (1L shl 52)) else (-.5 / (1L shl 52)))
            val e = ((((bits shr 52).toInt()) and 0x7ff) - 1022).toDouble()
            return varargsOf(valueOf(m), valueOf(e))
        }
    }

    internal class max : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            var m: LuaValue = args.checkvalue(1)
            var i = 2
            val n: Int = args.narg()
            while (i <= n) {
                val v: LuaValue = args.checkvalue(i)
                if (m.lt_b(v)) m = v
                ++i
            }
            return m
        }
    }

    internal class min : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs? {
            var m: LuaValue? = args.checkvalue(1)
            var i = 2
            val n: Int = args.narg()
            while (i <= n) {
                val v: LuaValue = args.checkvalue(i)
                if (v.lt_b(m)) m = v
                ++i
            }
            return m
        }
    }

    internal class modf : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val n: LuaValue = args.arg1()
            /* number is its own integer part, no fractional part */
            if (n.islong()) return varargsOf(n, valueOf(0.0))
            val x: Double = n.checkdouble()
            /* integer part (rounds toward zero) */
            val intPart: Double = if (x > 0) Math.floor(x) else Math.ceil(x)
            /* fractional part (test needed for inf/-inf) */
            val fracPart = if (x == intPart) 0.0 else x - intPart
            return varargsOf(valueOf(intPart), valueOf(fracPart))
        }
    }

    internal class random : LibFunction() {
        var random: Random = Random()
        override fun call(): LuaValue {
            return valueOf(random.nextDouble())
        }

        fun call(a: LuaValue): LuaValue {
            val m: Int = a.checkint()
            if (m < 1) argerror(1, "interval is empty")
            return valueOf(1 + random.nextInt(m))
        }

        fun call(a: LuaValue, b: LuaValue): LuaValue {
            val m: Int = a.checkint()
            val n: Int = b.checkint()
            if (n < m) argerror(2, "interval is empty")
            return valueOf(m + random.nextInt(n + 1 - m))
        }
    }

    internal class randomseed(val random: MathLib.random) : OneArgFunction() {
        fun call(arg: LuaValue): LuaValue {
            val seed: Long = arg.checklong()
            random.random = Random(seed)
            return NONE
        }
    }

    /**
     * Hook to override default dpow behavior with faster implementation.
     */
    open fun dpow_lib(a: Double, b: Double): Double {
        return net.blueva.luak.lib.MathLib.Companion.dpow_default(a, b)
    }

    companion object {
        /** Pointer to the latest MathLib instance, used only to dispatch
         * math.exp to tha correct platform math library.
         */
        var MATHLIB: MathLib? = null

        /** compute power using installed math library, or default if there is no math library installed  */
        fun dpow(a: Double, b: Double): LuaValue {
            return LuaDouble.valueOf(
                if (net.blueva.luak.lib.MathLib.Companion.MATHLIB != null) net.blueva.luak.lib.MathLib.Companion.MATHLIB.dpow_lib(
                    a,
                    b
                ) else net.blueva.luak.lib.MathLib.Companion.dpow_default(a, b)
            )
        }

        fun dpow_d(a: Double, b: Double): Double {
            return if (net.blueva.luak.lib.MathLib.Companion.MATHLIB != null) net.blueva.luak.lib.MathLib.Companion.MATHLIB.dpow_lib(
                a,
                b
            ) else net.blueva.luak.lib.MathLib.Companion.dpow_default(a, b)
        }

        /**
         * Default JME version computes using longhand heuristics.
         */
        protected fun dpow_default(a: Double, b: Double): Double {
            var a = a
            var b = b
            if (b < 0) return 1 / net.blueva.luak.lib.MathLib.Companion.dpow_default(a, -b)
            var p = 1.0
            var whole = b.toInt()
            var v = a
            while (whole > 0) {
                if ((whole and 1) != 0) p *= v
                whole = whole shr 1
                v *= v
            }
            if ((whole.let { b -= it; b }) > 0) {
                var frac = (0x10000 * b).toInt()
                while ((frac and 0xffff) != 0) {
                    a = Math.sqrt(a)
                    if ((frac and 0x8000) != 0) p *= a
                    frac = frac shl 1
                }
            }
            return p
        }
    }
}
