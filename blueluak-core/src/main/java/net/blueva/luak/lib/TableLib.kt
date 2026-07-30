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

import net.blueva.luak.LuaTable
import net.blueva.luak.LuaValue
import net.blueva.luak.Varargs

/**
 * Subclass of [LibFunction] which implements the lua standard `table`
 * library.
 * 
 * 
 * 
 * Typically, this library is included as part of a call to either
 * [net.blueva.luak.lib.jse.JsePlatform.standardGlobals] or [net.blueva.luak.lib.jme.JmePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals(); System.out.println( globals.get("table").get("length").call( LuaValue.tableOf() ) ); ` </pre>
 * 
 * 
 * To instantiate and use it directly,
 * link it into your globals table via [LuaValue.load] using code such as:
 * <pre> `Globals globals = new Globals(); globals.load(new JseBaseLib()); globals.load(new PackageLib()); globals.load(new TableLib()); System.out.println( globals.get("table").get("length").call( LuaValue.tableOf() ) ); ` </pre>
 * 
 * 
 * This has been implemented to match as closely as possible the behavior in the corresponding library in C.
 * @see LibFunction
 * 
 * @see net.blueva.luak.lib.jse.JsePlatform
 * 
 * @see net.blueva.luak.lib.jme.JmePlatform
 * 
 * @see [Lua 5.2 Table Lib Reference](http://www.lua.org/manual/5.2/manual.html.6.5)
 */
class TableLib : TwoArgFunction() {
    /** Perform one-time initialization on the library by creating a table
     * containing the library functions, adding that table to the supplied environment,
     * adding the table to package.loaded, and returning table as the return value.
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env the environment to load into, typically a Globals instance.
     */
    fun call(modname: LuaValue?, env: LuaValue): LuaValue {
        val table: LuaTable = LuaTable()
        table.set("concat", net.blueva.luak.lib.TableLib.concat())
        table.set("insert", net.blueva.luak.lib.TableLib.insert())
        table.set("pack", net.blueva.luak.lib.TableLib.pack())
        table.set("remove", net.blueva.luak.lib.TableLib.remove())
        table.set("sort", net.blueva.luak.lib.TableLib.sort())
        table.set("unpack", net.blueva.luak.lib.TableLib.unpack())
        env.set("table", table)
        if (!env.get("package")!!.isnil()) env.get("package")!!.get("loaded")!!.set("table", table)
        return NIL
    }

    // "concat" (table [, sep [, i [, j]]]) -> string
    internal class concat : TableLibFunction() {
        fun call(list: LuaValue): LuaValue {
            return list.checktable()!!.concat(EMPTYSTRING, 1, list.length())
        }

        fun call(list: LuaValue, sep: LuaValue): LuaValue {
            return list.checktable()!!.concat(sep.checkstring(), 1, list.length())
        }

        fun call(list: LuaValue, sep: LuaValue, i: LuaValue): LuaValue {
            return list.checktable()!!.concat(sep.checkstring(), i.checkint(), list.length())
        }

        fun call(list: LuaValue, sep: LuaValue, i: LuaValue, j: LuaValue): LuaValue {
            return list.checktable()!!.concat(sep.checkstring(), i.checkint(), j.checkint())
        }
    }

    // "insert" (table, [pos,] value)
    internal class insert : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            when (args.narg()) {
                2 -> {
                    val table: LuaTable = args.checktable(1)
                    table.insert(table.length() + 1, args.arg(2))
                    return NONE
                }

                3 -> {
                    val table: LuaTable = args.checktable(1)
                    val pos: Int = args.checkint(2)
                    val max: Int = table.length() + 1
                    if (pos < 1 || pos > max) argerror(
                        2,
                        "position out of bounds: " + pos + " not between 1 and " + max
                    )
                    table.insert(pos, args.arg(3))
                    return NONE
                }

                else -> {
                    return error("wrong number of arguments to 'table.insert': " + args.narg() + " (must be 2 or 3)")
                }
            }
        }
    }

    // "pack" (...) -> table
    internal class pack : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val t: LuaValue = tableOf(args, 1)
            t.set("n", args.narg())
            return t
        }
    }

    // "remove" (table [, pos]) -> removed-ele
    internal class remove : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val table: LuaTable = args.checktable(1)
            val size: Int = table.length()
            val pos: Int = args.optint(2, size)
            if (pos != size && (pos < 1 || pos > size + 1)) {
                argerror(2, "position out of bounds: " + pos + " not between 1 and " + (size + 1))
            }
            return table.remove(pos)
        }
    }

    // "sort" (table [, comp])
    internal class sort : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            args.checktable(1).sort(
                if (args.isnil(2)) NIL else args.checkfunction(2)
            )
            return NONE
        }
    }


    // "unpack", // (list [,i [,j]]) -> result1, ...
    internal class unpack : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val t: LuaTable = args.checktable(1)
            // do not waste resource for calc rawlen if arg3 is not nil
            val len = if (args.arg(3)!!.isnil()) t.length() else 0
            return t.unpack(args.optint(2, 1), args.optint(3, len))
        }
    }
}
