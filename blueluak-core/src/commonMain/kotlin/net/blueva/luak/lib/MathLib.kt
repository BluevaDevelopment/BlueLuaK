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
import kotlin.math.pow
import kotlin.random.Random

/**
 * Subclass of [LibFunction] which implements the lua standard `math`
 * library.
 *
 *
 * The whole library is implemented in `commonMain` on top of [kotlin.math],
 * so every target - JVM, JavaScript, Wasm, and Native - gets the same set of
 * functions with the same accuracy. There is no reduced variant: the
 * hand-rolled `exp()`/`pow()` approximations BlueLuaK inherited from LuaJ's
 * J2ME profile are gone, and `acos`, `asin`, `atan`, `atan2`, `cosh`, `log`,
 * `sinh`, and `tanh` are part of this class rather than a JVM-only subclass.
 *
 *
 * Typically this library is included as part of a call to
 * [net.blueva.luak.lib.LuaPlatform.standardGlobals] (any target) or
 * [net.blueva.luak.lib.jvm.JvmPlatform.standardGlobals] (JVM only):
 * ```kotlin
 * val globals = LuaPlatform.standardGlobals()
 * println(globals.get("math").get("sqrt").call(LuaValue.valueOf(2)))
 * ```
 *
 *
 * To instantiate and use it directly, link it into your globals table via
 * [Globals.load] using code such as:
 * ```kotlin
 * val globals = Globals()
 * globals.load(BaseLib())
 * globals.load(PackageLib())
 * globals.load(MathLib())
 * ```
 * Doing so will ensure the library is properly initialized
 * and loaded into the globals table.
 *
 *
 * This has been implemented to match as closely as possible the behavior in the corresponding library in C.
 * @see LibFunction
 *
 * @see net.blueva.luak.lib.LuaPlatform
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
    open override fun call(modname: LuaValue?, env: LuaValue?): LuaValue? {
        val math: LuaTable = LuaTable(0, 30)
        math.set("abs", net.blueva.luak.lib.MathLib.abs())
        math.set("acos", net.blueva.luak.lib.MathLib.acos())
        math.set("asin", net.blueva.luak.lib.MathLib.asin())
        val atan: LuaValue = net.blueva.luak.lib.MathLib.atan()
        math.set("atan", atan)
        math.set("atan2", atan)
        math.set("ceil", net.blueva.luak.lib.MathLib.ceil())
        math.set("cos", net.blueva.luak.lib.MathLib.cos())
        math.set("cosh", net.blueva.luak.lib.MathLib.cosh())
        math.set("deg", net.blueva.luak.lib.MathLib.deg())
        math.set("exp", net.blueva.luak.lib.MathLib.exp())
        math.set("floor", net.blueva.luak.lib.MathLib.floor())
        math.set("fmod", net.blueva.luak.lib.MathLib.fmod())
        math.set("frexp", net.blueva.luak.lib.MathLib.frexp())
        math.set("huge", LuaDouble.POSINF)
        math.set("ldexp", net.blueva.luak.lib.MathLib.ldexp())
        math.set("log", net.blueva.luak.lib.MathLib.log())
        math.set("max", net.blueva.luak.lib.MathLib.max())
        math.set("min", net.blueva.luak.lib.MathLib.min())
        math.set("modf", net.blueva.luak.lib.MathLib.modf())
        math.set("pi", kotlin.math.PI)
        math.set("pow", net.blueva.luak.lib.MathLib.pow())
        val r: random?
        math.set("random", net.blueva.luak.lib.MathLib.random().also { r = it })
        math.set("randomseed", net.blueva.luak.lib.MathLib.randomseed((r)!!))
        math.set("rad", net.blueva.luak.lib.MathLib.rad())
        math.set("sin", net.blueva.luak.lib.MathLib.sin())
        math.set("sinh", net.blueva.luak.lib.MathLib.sinh())
        math.set("sqrt", net.blueva.luak.lib.MathLib.sqrt())
        math.set("tan", net.blueva.luak.lib.MathLib.tan())
        math.set("tanh", net.blueva.luak.lib.MathLib.tanh())
        env!!.set("math", math)
        if (!env!!.get("package")!!.isnil()) env!!.get("package")!!.get("loaded")!!.set("math", math)
        return math
    }

    abstract class UnaryOp : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            return valueOf(call(arg!!.checkdouble()))
        }

        protected abstract fun call(d: Double): Double
    }

    abstract class BinaryOp : TwoArgFunction() {
        override fun call(x: LuaValue?, y: LuaValue?): LuaValue? {
            return valueOf(call(x!!.checkdouble(), y!!.checkdouble()))
        }

        protected abstract fun call(x: Double, y: Double): Double
    }

    internal class abs : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.abs(d)
        }
    }

    internal class acos : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.acos(d)
        }
    }

    internal class asin : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.asin(d)
        }
    }

    /** Serves both `math.atan` and its Lua 5.1 alias `math.atan2`; the second
     * argument defaults to 1.0, which makes the one-argument form the plain
     * arc tangent. */
    internal class atan : TwoArgFunction() {
        override fun call(y: LuaValue?, x: LuaValue?): LuaValue? {
            return valueOf(kotlin.math.atan2(y!!.checkdouble(), x!!.optdouble(1.0)))
        }
    }

    internal class cosh : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.cosh(d)
        }
    }

    internal class sinh : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.sinh(d)
        }
    }

    internal class tanh : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.tanh(d)
        }
    }

    /** `math.log(x [, base])`; the base defaults to [kotlin.math.E]. */
    internal class log : TwoArgFunction() {
        override fun call(x: LuaValue?, base: LuaValue?): LuaValue? {
            var natural: Double = kotlin.math.ln(x!!.checkdouble())
            val b: Double = base!!.optdouble(kotlin.math.E)
            if (b != kotlin.math.E) natural /= kotlin.math.ln(b)
            return valueOf(natural)
        }
    }

    internal class ceil : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.ceil(d)
        }
    }

    internal class cos : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.cos(d)
        }
    }

    internal class deg : UnaryOp() {
        override fun call(d: Double): Double {
            return d * 180.0 / kotlin.math.PI
        }
    }

    internal class floor : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.floor(d)
        }
    }

    internal class rad : UnaryOp() {
        override fun call(d: Double): Double {
            return d * kotlin.math.PI / 180.0
        }
    }

    internal class sin : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.sin(d)
        }
    }

    internal class sqrt : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.sqrt(d)
        }
    }

    internal class tan : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.tan(d)
        }
    }

    internal class exp : UnaryOp() {
        override fun call(d: Double): Double {
            return kotlin.math.exp(d)
        }
    }

    internal class fmod : TwoArgFunction() {
        override fun call(xv: LuaValue?, yv: LuaValue?): LuaValue? {
            if (xv!!.islong() && yv!!.islong() && yv!!.tolong() != 0L) {
                return valueOf((xv!!.tolong() % yv!!.tolong()).toDouble())
            }
            return valueOf(xv!!.checkdouble() % yv!!.checkdouble())
        }
    }

    internal class ldexp : BinaryOp() {
        override fun call(x: Double, y: Double): Double {
            // This is the behavior on os-x, windows differs in rounding behavior.
            return x * Double.fromBits(((y.toLong()) + 1023) shl 52)
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
            if (x == 0.0) return (varargsOf(ZERO, (ZERO)!!))!!
            val bits: Long = (x).toBits()
            val m =
                ((bits and ((-1L shl 52).inv()).toLong()) + (1L shl 52)) * (if (bits >= 0) (.5 / (1L shl 52)) else (-.5 / (1L shl 52)))
            val e = ((((bits shr 52).toInt()) and 0x7ff) - 1022).toDouble()
            return (varargsOf(valueOf(m), valueOf(e)))!!
        }
    }

    internal class max : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            var m: LuaValue = args.checknumber(1)
            var i = 2
            val n: Int = args.narg()
            while (i <= n) {
                val v: LuaValue = args.checknumber(i)
                if (m.lt_b(v)) m = v
                ++i
            }
            return m
        }
    }

    internal class min : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            var m: LuaValue = args.checknumber(1)
            var i = 2
            val n: Int = args.narg()
            while (i <= n) {
                val v: LuaValue = args.checknumber(i)
                if (v.lt_b(m)) m = v
                ++i
            }
            return m
        }
    }

    internal class modf : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val n: LuaValue = args.arg1()!!
            /* number is its own integer part, no fractional part */
            if (n.islong()) return (varargsOf(n, valueOf(0.0)))!!
            val x: Double = n.checkdouble()
            /* integer part (rounds toward zero) */
            val intPart: Double = if (x > 0) kotlin.math.floor(x) else kotlin.math.ceil(x)
            /* fractional part (test needed for inf/-inf) */
            val fracPart = if (x == intPart) 0.0 else x - intPart
            return (varargsOf(valueOf(intPart), valueOf(fracPart)))!!
        }
    }

    internal class random : LibFunction() {
        var random: Random = Random.Default
        override fun call(): LuaValue? {
            return valueOf(random.nextDouble())
        }

        override fun call(a: LuaValue?): LuaValue? {
            val m: Int = a!!.checkint()
            if (m < 1) argerror(1, "interval is empty")
            return valueOf(1 + random.nextInt(m))
        }

        override fun call(a: LuaValue?, b: LuaValue?): LuaValue? {
            val m: Int = a!!.checkint()
            val n: Int = b!!.checkint()
            if (n < m) argerror(2, "interval is empty")
            return valueOf(m + random.nextInt(n + 1 - m))
        }
    }

    internal class randomseed(val random: MathLib.random) : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            val seed: Long = arg!!.checklong()
            random.random = kotlin.random.Random(seed)
            return (NONE)!!
        }
    }

    /**
     * Hook to override the default `^` / `math.pow` behavior with a different
     * implementation. The default is already [kotlin.math.pow] on every target.
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
            return LuaDouble.valueOf(net.blueva.luak.lib.MathLib.Companion.dpow_d(a, b))!!
        }

        fun dpow_d(a: Double, b: Double): Double {
            val installed: MathLib? = net.blueva.luak.lib.MathLib.Companion.MATHLIB
            return installed?.dpow_lib(a, b) ?: net.blueva.luak.lib.MathLib.Companion.dpow_default(a, b)
        }

        /**
         * Accurate power on every target; [kotlin.math.pow] is part of the
         * Kotlin standard library, so nothing here needs a platform-specific
         * override any more. [dpow_lib] survives only as an extension point
         * for embedders that want a different implementation.
         */
        protected fun dpow_default(a: Double, b: Double): Double = a.pow(b)
    }
}
