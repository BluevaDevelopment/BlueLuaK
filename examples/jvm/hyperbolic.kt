import org.basaltmc.luak.LuaValue
import org.basaltmc.luak.lib.OneArgFunction
import org.basaltmc.luak.lib.TwoArgFunction
import kotlin.math.cosh
import kotlin.math.sinh

/** Kotlin library loadable from Lua with `require("hyperbolic")`. */
@Suppress("ClassName")
class hyperbolic : TwoArgFunction() {
    override fun call(arg1: LuaValue?, arg2: LuaValue?): LuaValue {
        val library = tableOf()
        library.set("sinh", unary(::sinh))
        library.set("cosh", unary(::cosh))
        arg2!!.set("hyperbolic", library)
        return library
    }

    private fun unary(operation: (Double) -> Double) = object : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue = valueOf(operation(arg!!.checkdouble()))
    }
}
