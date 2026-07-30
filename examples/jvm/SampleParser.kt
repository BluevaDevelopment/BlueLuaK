import net.blueva.luak.ast.Exp
import net.blueva.luak.ast.Stat
import net.blueva.luak.ast.Visitor
import net.blueva.luak.parser.LuaParser
import net.blueva.luak.parser.ParseException
import java.io.FileInputStream

/** Parses a Lua file with the ANTLR Kotlin parser and prints function locations. */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("usage: SampleParser luafile")
        return
    }

    try {
        val chunk = FileInputStream(args[0]).bufferedReader().use { LuaParser(it.readText()).Chunk() }
        chunk.accept(object : Visitor() {
            override fun visit(exp: Exp.AnonFuncDef) {
                println("Anonymous function at ${exp.beginLine}.${exp.beginColumn}-${exp.endLine}.${exp.endColumn}")
                super.visit(exp)
            }

            override fun visit(stat: Stat.FuncDef) {
                println("Function '${stat.name?.name?.name}' at ${stat.beginLine}.${stat.beginColumn}-${stat.endLine}.${stat.endColumn}")
                super.visit(stat)
            }

            override fun visit(stat: Stat.LocalFuncDef) {
                println("Local function '${stat.name.name}' at ${stat.beginLine}.${stat.beginColumn}-${stat.endLine}.${stat.endColumn}")
                super.visit(stat)
            }
        })
    } catch (error: ParseException) {
        println("parse failed: ${error.message}")
    }
}
