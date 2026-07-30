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

import net.blueva.luak.Buffer
import net.blueva.luak.Globals
import net.blueva.luak.LuaTable
import net.blueva.luak.LuaValue
import net.blueva.luak.Varargs
import java.io.IOException
import java.util.Calendar
import java.util.Date

/**
 * Subclass of [LibFunction] which implements the standard lua `os` library.
 * 
 * 
 * It is a usable base with simplified stub functions
 * for library functions that cannot be implemented uniformly
 * on Jse and Jme.
 * 
 * 
 * This can be installed as-is on either platform, or extended
 * and refined to be used in a complete Jse implementation.
 * 
 * 
 * Because the nature of the `os` library is to encapsulate
 * os-specific features, the behavior of these functions varies considerably
 * from their counterparts in the C platform.
 * 
 * 
 * The following functions have limited implementations of features
 * that are not supported well on Jme:
 * 
 *  * `execute()`
 *  * `remove()`
 *  * `rename()`
 *  * `tmpname()`
 * 
 * 
 * 
 * Typically, this library is included as part of a call to either
 * [net.blueva.luak.lib.jse.JsePlatform.standardGlobals] or [net.blueva.luak.lib.jme.JmePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals(); System.out.println( globals.get("os").get("time").call() ); ` </pre>
 * In this example the platform-specific [net.blueva.luak.lib.jse.JseOsLib] library will be loaded, which will include
 * the base functionality provided by this class.
 * 
 * 
 * To instantiate and use it directly,
 * link it into your globals table via [LuaValue.load] using code such as:
 * <pre> `Globals globals = new Globals(); globals.load(new JseBaseLib()); globals.load(new PackageLib()); globals.load(new OsLib()); System.out.println( globals.get("os").get("time").call() ); ` </pre>
 * 
 * 
 * @see LibFunction
 * 
 * @see net.blueva.luak.lib.jse.JseOsLib
 * 
 * @see net.blueva.luak.lib.jse.JsePlatform
 * 
 * @see net.blueva.luak.lib.jme.JmePlatform
 * 
 * @see [http://www.lua.org/manual/5.1/manual.html.5.8](http://www.lua.org/manual/5.1/manual.html.5.8)
 */
class OsLib
/**
 * Create and OsLib instance.
 */
    : TwoArgFunction() {
    protected var globals: Globals? = null

    /** Perform one-time initialization on the library by creating a table
     * containing the library functions, adding that table to the supplied environment,
     * adding the table to package.loaded, and returning table as the return value.
     * @param modname the module name supplied if this is loaded via 'require'.
     * @param env the environment to load into, typically a Globals instance.
     */
    fun call(modname: LuaValue?, env: LuaValue): LuaValue {
        globals = env.checkglobals()
        val os: LuaTable = LuaTable()
        for (i in net.blueva.luak.lib.OsLib.Companion.NAMES.indices) os.set(
            net.blueva.luak.lib.OsLib.Companion.NAMES[i],
            net.blueva.luak.lib.OsLib.OsLibFunc(i, net.blueva.luak.lib.OsLib.Companion.NAMES[i])
        )
        env.set("os", os)
        if (!env.get("package")!!.isnil()) env.get("package")!!.get("loaded")!!.set("os", os)
        return os
    }

    internal inner class OsLibFunc(opcode: Int, name: String?) : VarArgFunction() {
        init {
            this.opcode = opcode
            this.name = name
        }

        override fun invoke(args: Varargs): Varargs {
            try {
                when (opcode) {
                    net.blueva.luak.lib.OsLib.Companion.CLOCK -> return valueOf(clock())
                    net.blueva.luak.lib.OsLib.Companion.DATE -> {
                        val s: String = args.optjstring(1, "%c")
                        val t = if (args.isnumber(2)) args.todouble(2) else time(null)
                        if (s.equals("*t")) {
                            val d: Calendar = Calendar.getInstance()
                            d.setTime(Date((t * 1000).toLong()))
                            val tbl: LuaTable = LuaValue.tableOf()
                            tbl.set("year", LuaValue.valueOf(d.get(Calendar.YEAR)))
                            tbl.set("month", LuaValue.valueOf(d.get(Calendar.MONTH) + 1))
                            tbl.set("day", LuaValue.valueOf(d.get(Calendar.DAY_OF_MONTH)))
                            tbl.set("hour", LuaValue.valueOf(d.get(Calendar.HOUR_OF_DAY)))
                            tbl.set("min", LuaValue.valueOf(d.get(Calendar.MINUTE)))
                            tbl.set("sec", LuaValue.valueOf(d.get(Calendar.SECOND)))
                            tbl.set("wday", LuaValue.valueOf(d.get(Calendar.DAY_OF_WEEK)))
                            tbl.set("yday", LuaValue.valueOf(d.get(0x6))) // Day of year
                            tbl.set("isdst", LuaValue.valueOf(isDaylightSavingsTime(d)))
                            return tbl
                        }
                        return valueOf(date(s, if (t == -1.0) time(null) else t))
                    }

                    net.blueva.luak.lib.OsLib.Companion.DIFFTIME -> return valueOf(
                        difftime(
                            args.checkdouble(1),
                            args.checkdouble(2)
                        )
                    )

                    net.blueva.luak.lib.OsLib.Companion.EXECUTE -> return execute(args.optjstring(1, null))
                    net.blueva.luak.lib.OsLib.Companion.EXIT -> {
                        exit(args.optint(1, 0))
                        return NONE
                    }

                    net.blueva.luak.lib.OsLib.Companion.GETENV -> {
                        val `val`: String? = getenv(args.checkjstring(1))
                        return if (`val` != null) valueOf(`val`) else NIL
                    }

                    net.blueva.luak.lib.OsLib.Companion.REMOVE -> {
                        remove(args.checkjstring(1))
                        return LuaValue.TRUE
                    }

                    net.blueva.luak.lib.OsLib.Companion.RENAME -> {
                        rename(args.checkjstring(1), args.checkjstring(2))
                        return LuaValue.TRUE
                    }

                    net.blueva.luak.lib.OsLib.Companion.SETLOCALE -> {
                        val s = setlocale(args.optjstring(1, null), args.optjstring(2, "all"))
                        return if (s != null) valueOf(s) else NIL
                    }

                    net.blueva.luak.lib.OsLib.Companion.TIME -> return valueOf(time(args.opttable(1, null)))
                    net.blueva.luak.lib.OsLib.Companion.TMPNAME -> return valueOf(tmpname())
                }
                return NONE
            } catch (e: IOException) {
                return varargsOf(NIL, valueOf(e.message))
            }
        }
    }

    /**
     * @return an approximation of the amount in seconds of CPU time used by
     * the program.  For luaj this simple returns the elapsed time since the
     * OsLib class was loaded.
     */
    protected fun clock(): Double {
        return (System.currentTimeMillis() - net.blueva.luak.lib.OsLib.Companion.t0) / 1000.0
    }

    /**
     * Returns the number of seconds from time t1 to time t2.
     * In POSIX, Windows, and some other systems, this value is exactly t2-t1.
     * @param t2
     * @param t1
     * @return diffeence in time values, in seconds
     */
    protected fun difftime(t2: Double, t1: Double): Double {
        return t2 - t1
    }

    /**
     * If the time argument is present, this is the time to be formatted
     * (see the os.time function for a description of this value).
     * Otherwise, date formats the current time.
     * 
     * Date returns the date as a string,
     * formatted according to the same rules as ANSII strftime, but without
     * support for %g, %G, or %V.
     * 
     * When called without arguments, date returns a reasonable date and
     * time representation that depends on the host system and on the
     * current locale (that is, os.date() is equivalent to os.date("%c")).
     * 
     * @param format
     * @param time time since epoch, or -1 if not supplied
     * @return a LString or a LTable containing date and time,
     * formatted according to the given string format.
     */
    fun date(format: String, time: Double): String {
        var format = format
        var time = time
        val d: Calendar = Calendar.getInstance()
        d.setTime(Date((time * 1000).toLong()))
        if (format.startsWith("!")) {
            time -= timeZoneOffset(d).toDouble()
            d.setTime(Date((time * 1000).toLong()))
            format = format.substring(1)
        }
        val fmt: ByteArray = format.toByteArray()
        val n = fmt.size
        val result: Buffer = Buffer(n)
        var c: Byte
        var i = 0
        while (i < n) {
            when (fmt[i++].also { c = it }) {
                '\n' -> result.append("\n")
                '%' -> {
                    if (i >= n) break
                    when (fmt[i++].also { c = it }) {
                        '%' -> result.append('%'.code.toByte())
                        'a' -> result.append(net.blueva.luak.lib.OsLib.Companion.WeekdayNameAbbrev[d.get(Calendar.DAY_OF_WEEK) - 1])
                        'A' -> result.append(net.blueva.luak.lib.OsLib.Companion.WeekdayName[d.get(Calendar.DAY_OF_WEEK) - 1])
                        'b' -> result.append(net.blueva.luak.lib.OsLib.Companion.MonthNameAbbrev[d.get(Calendar.MONTH)])
                        'B' -> result.append(net.blueva.luak.lib.OsLib.Companion.MonthName[d.get(Calendar.MONTH)])
                        'c' -> result.append(date("%a %b %d %H:%M:%S %Y", time))
                        'd' -> result.append((100 + d.get(Calendar.DAY_OF_MONTH)).toString().substring(1))
                        'H' -> result.append((100 + d.get(Calendar.HOUR_OF_DAY)).toString().substring(1))
                        'I' -> result.append((100 + (d.get(Calendar.HOUR_OF_DAY) % 12)).toString().substring(1))
                        'j' -> {
                            // day of year.
                            val y0: Calendar = beginningOfYear(d)
                            val dayOfYear =
                                ((d.getTime().getTime() - y0.getTime().getTime()) / (24 * 3600L * 1000L)) as Int
                            result.append((1001 + dayOfYear).toString().substring(1))
                        }

                        'm' -> result.append((101 + d.get(Calendar.MONTH)).toString().substring(1))
                        'M' -> result.append((100 + d.get(Calendar.MINUTE)).toString().substring(1))
                        'p' -> result.append(if (d.get(Calendar.HOUR_OF_DAY) < 12) "AM" else "PM")
                        'S' -> result.append((100 + d.get(Calendar.SECOND)).toString().substring(1))
                        'U' -> result.append((weekNumber(d, 0)).toString())
                        'w' -> result.append(((d.get(Calendar.DAY_OF_WEEK) + 6) % 7).toString())
                        'W' -> result.append((weekNumber(d, 1)).toString())
                        'x' -> result.append(date("%m/%d/%y", time))
                        'X' -> result.append(date("%H:%M:%S", time))
                        'y' -> result.append((d.get(Calendar.YEAR)).toString().substring(2))
                        'Y' -> result.append((d.get(Calendar.YEAR)).toString())
                        'z' -> {
                            val tzo = timeZoneOffset(d) / 60
                            val a: Int = Math.abs(tzo)
                            val h: String? = (100 + a / 60).toString().substring(1)
                            val m: String? = (100 + a % 60).toString().substring(1)
                            result.append((if (tzo >= 0) "+" else "-") + h + m)
                        }

                        else -> LuaValue.argerror(1, "invalid conversion specifier '%" + c + "'")
                    }
                }

                else -> result.append(c)
            }
        }
        return result.tojstring()
    }

    private fun beginningOfYear(d: Calendar): Calendar {
        val y0: Calendar = Calendar.getInstance()
        y0.setTime(d.getTime())
        y0.set(Calendar.MONTH, 0)
        y0.set(Calendar.DAY_OF_MONTH, 1)
        y0.set(Calendar.HOUR_OF_DAY, 0)
        y0.set(Calendar.MINUTE, 0)
        y0.set(Calendar.SECOND, 0)
        y0.set(Calendar.MILLISECOND, 0)
        return y0
    }

    private fun weekNumber(d: Calendar, startDay: Int): Int {
        val y0: Calendar = beginningOfYear(d)
        y0.set(Calendar.DAY_OF_MONTH, 1 + (startDay + 8 - y0.get(Calendar.DAY_OF_WEEK)) % 7)
        if (y0.after(d)) {
            y0.set(Calendar.YEAR, y0.get(Calendar.YEAR) - 1)
            y0.set(Calendar.DAY_OF_MONTH, 1 + (startDay + 8 - y0.get(Calendar.DAY_OF_WEEK)) % 7)
        }
        val dt: Long = d.getTime().getTime() - y0.getTime().getTime()
        return 1 + (dt / (7L * 24L * 3600L * 1000L)).toInt()
    }

    private fun timeZoneOffset(d: Calendar): Int {
        val localStandarTimeMillis: Int = (d.get(Calendar.HOUR_OF_DAY) * 3600 + d.get(Calendar.MINUTE) * 60 +
                d.get(Calendar.SECOND)) * 1000
        return d.getTimeZone().getOffset(
            1,
            d.get(Calendar.YEAR),
            d.get(Calendar.MONTH),
            d.get(Calendar.DAY_OF_MONTH),
            d.get(Calendar.DAY_OF_WEEK),
            localStandarTimeMillis
        ) / 1000
    }

    private fun isDaylightSavingsTime(d: Calendar): Boolean {
        return timeZoneOffset(d) != d.getTimeZone().getRawOffset() / 1000
    }

    /**
     * This function is equivalent to the C function system.
     * It passes command to be executed by an operating system shell.
     * It returns a status code, which is system-dependent.
     * If command is absent, then it returns nonzero if a shell
     * is available and zero otherwise.
     * @param command command to pass to the system
     */
    protected fun execute(command: String?): Varargs {
        return varargsOf(NIL, valueOf("exit"), ONE)
    }

    /**
     * Calls the C function exit, with an optional code, to terminate the host program.
     * @param code
     */
    protected fun exit(code: Int) {
        System.exit(code)
    }

    /**
     * Returns the value of the process environment variable varname,
     * or the System property value for varname,
     * or null if the variable is not defined in either environment.
     * 
     * The default implementation, which is used by the JmePlatform,
     * only queryies System.getProperty().
     * 
     * The JsePlatform overrides this behavior and returns the
     * environment variable value using System.getenv() if it exists,
     * or the System property value if it does not.
     * 
     * A SecurityException may be thrown if access is not allowed
     * for 'varname'.
     * @param varname
     * @return String value, or null if not defined
     */
    protected fun getenv(varname: String?): String {
        return System.getProperty(varname)
    }

    /**
     * Deletes the file or directory with the given name.
     * Directories must be empty to be removed.
     * If this function fails, it throws and IOException
     * 
     * @param filename
     * @throws IOException if it fails
     */
    @kotlin.Throws(IOException::class)
    protected fun remove(filename: String?) {
        throw IOException("not implemented")
    }

    /**
     * Renames file or directory named oldname to newname.
     * If this function fails,it throws and IOException
     * 
     * @param oldname old file name
     * @param newname new file name
     * @throws IOException if it fails
     */
    @kotlin.Throws(IOException::class)
    protected fun rename(oldname: String?, newname: String?) {
        throw IOException("not implemented")
    }

    /**
     * Sets the current locale of the program. locale is a string specifying
     * a locale; category is an optional string describing which category to change:
     * "all", "collate", "ctype", "monetary", "numeric", or "time"; the default category
     * is "all".
     * 
     * If locale is the empty string, the current locale is set to an implementation-
     * defined native locale. If locale is the string "C", the current locale is set
     * to the standard C locale.
     * 
     * When called with null as the first argument, this function only returns the
     * name of the current locale for the given category.
     * 
     * @param locale
     * @param category
     * @return the name of the new locale, or null if the request
     * cannot be honored.
     */
    protected fun setlocale(locale: String?, category: String?): String? {
        return "C"
    }

    /**
     * Returns the current time when called without arguments,
     * or a time representing the date and time specified by the given table.
     * This table must have fields year, month, and day,
     * and may have fields hour, min, sec, and isdst
     * (for a description of these fields, see the os.date function).
     * @param table
     * @return long value for the time
     */
    protected fun time(table: LuaTable?): Double {
        val d: java.util.Date
        if (table == null) {
            d = Date()
        } else {
            val c: Calendar = Calendar.getInstance()
            c.set(Calendar.YEAR, table.get("year")!!.checkint())
            c.set(Calendar.MONTH, table.get("month")!!.checkint() - 1)
            c.set(Calendar.DAY_OF_MONTH, table.get("day")!!.checkint())
            c.set(Calendar.HOUR_OF_DAY, table.get("hour")!!.optint(12))
            c.set(Calendar.MINUTE, table.get("min")!!.optint(0))
            c.set(Calendar.SECOND, table.get("sec")!!.optint(0))
            c.set(Calendar.MILLISECOND, 0)
            d = c.getTime()
        }
        return d.getTime() / 1000.0
    }

    /**
     * Returns a string with a file name that can be used for a temporary file.
     * The file must be explicitly opened before its use and explicitly removed
     * when no longer needed.
     * 
     * On some systems (POSIX), this function also creates a file with that name,
     * to avoid security risks. (Someone else might create the file with wrong
     * permissions in the time between getting the name and creating the file.)
     * You still have to open the file to use it and to remove it (even if you
     * do not use it).
     * 
     * @return String filename to use
     */
    protected fun tmpname(): String {
        kotlin.synchronized(net.blueva.luak.lib.OsLib::class.java) {
            return net.blueva.luak.lib.OsLib.Companion.TMP_PREFIX + (net.blueva.luak.lib.OsLib.Companion.tmpnames++) + net.blueva.luak.lib.OsLib.Companion.TMP_SUFFIX
        }
    }

    companion object {
        val TMP_PREFIX: String = ".luaj"
        val TMP_SUFFIX: String = "tmp"

        private const val CLOCK = 0
        private const val DATE = 1
        private const val DIFFTIME = 2
        private const val EXECUTE = 3
        private const val EXIT = 4
        private const val GETENV = 5
        private const val REMOVE = 6
        private const val RENAME = 7
        private const val SETLOCALE = 8
        private const val TIME = 9
        private const val TMPNAME = 10

        private val NAMES = arrayOf<String?>(
            "clock",
            "date",
            "difftime",
            "execute",
            "exit",
            "getenv",
            "remove",
            "rename",
            "setlocale",
            "time",
            "tmpname",
        )

        private val t0: Long = System.currentTimeMillis()
        private var tmpnames: Long = net.blueva.luak.lib.OsLib.Companion.t0

        private val WeekdayNameAbbrev = arrayOf<String?>("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        private val WeekdayName =
            arrayOf<String?>("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        private val MonthNameAbbrev =
            arrayOf<String?>("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        private val MonthName = arrayOf<String?>(
            "January",
            "February",
            "March",
            "April",
            "May",
            "June",
            "July",
            "August",
            "September",
            "October",
            "November",
            "December"
        )
    }
}
