import net.blueva.luak.LuaValue
import net.blueva.luak.lib.jvm.JvmPlatform

/** Minimal Kotlin/JVM program that loads and executes a Lua script. */
fun main() {
    val script = "examples/lua/hello.lua"
    val globals = JvmPlatform.standardGlobals()
    val chunk = globals.loadfile(script)!!
    chunk.call(LuaValue.valueOf(script))
}
