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

import net.blueva.luak.Globals
import net.blueva.luak.LuaString
import net.blueva.luak.LuaTable
import net.blueva.luak.LuaValue
import net.blueva.luak.Varargs
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException

/**
 * Abstract base class extending [LibFunction] which implements the
 * core of the lua standard `io` library.
 * 
 * 
 * It contains the implementation of the io library support that is common to
 * the JSE and JME platforms.
 * In practice on of the concrete IOLib subclasses is chosen:
 * [net.blueva.luak.lib.jse.JseIoLib] for the JSE platform, and
 * [net.blueva.luak.lib.jme.JmeIoLib] for the JME platform.
 * 
 * 
 * The JSE implementation conforms almost completely to the C-based lua library,
 * while the JME implementation follows closely except in the area of random-access files,
 * which are difficult to support properly on JME.
 * 
 * 
 * Typically, this library is included as part of a call to either
 * [net.blueva.luak.lib.jse.JsePlatform.standardGlobals] or [net.blueva.luak.lib.jme.JmePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals(); globals.get("io").get("write").call(LuaValue.valueOf("hello, world\n")); ` </pre>
 * In this example the platform-specific [net.blueva.luak.lib.jse.JseIoLib] library will be loaded, which will include
 * the base functionality provided by this class, whereas the [net.blueva.luak.lib.jse.JsePlatform] would load the
 * [net.blueva.luak.lib.jse.JseIoLib].
 * 
 * 
 * To instantiate and use it directly,
 * link it into your globals table via [LuaValue.load] using code such as:
 * <pre> `Globals globals = new Globals(); globals.load(new JseBaseLib()); globals.load(new PackageLib()); globals.load(new OsLib()); globals.get("io").get("write").call(LuaValue.valueOf("hello, world\n")); ` </pre>
 * 
 * 
 * This has been implemented to match as closely as possible the behavior in the corresponding library in C.
 * @see LibFunction
 * 
 * @see net.blueva.luak.lib.jse.JsePlatform
 * 
 * @see net.blueva.luak.lib.jme.JmePlatform
 * 
 * @see net.blueva.luak.lib.jse.JseIoLib
 * 
 * @see net.blueva.luak.lib.jme.JmeIoLib
 * 
 * @see [http://www.lua.org/manual/5.1/manual.html.5.7](http://www.lua.org/manual/5.1/manual.html.5.7)
 */
abstract
class IoLib : TwoArgFunction() {
    protected abstract
    inner class File : LuaValue() {
        @kotlin.Throws(IOException::class)
        abstract fun write(string: LuaString?)

        @kotlin.Throws(IOException::class)
        abstract fun flush()
        abstract fun isstdfile(): Boolean

        @kotlin.Throws(IOException::class)
        abstract fun close()
        abstract fun isclosed(): Boolean

        // returns new position
        @kotlin.Throws(IOException::class)
        abstract fun seek(option: String?, bytecount: Int): Int
        abstract fun setvbuf(mode: String?, size: Int)

        // get length remaining to read
        @kotlin.Throws(IOException::class)
        abstract fun remaining(): Int

        // peek ahead one character
        @kotlin.Throws(IOException::class, EOFException::class)
        abstract fun peek(): Int

        // return char if read, -1 if eof, throw IOException on other exception
        @kotlin.Throws(IOException::class, EOFException::class)
        abstract fun read(): Int

        // return number of bytes read if positive, false if eof, throw IOException on other exception
        @kotlin.Throws(IOException::class)
        abstract fun read(bytes: ByteArray?, offset: Int, length: Int): Int

        @kotlin.Throws(IOException::class)
        fun eof(): Boolean {
            try {
                return peek() < 0
            } catch (e: EOFException) {
                return true
            }
        }

        // delegate method access to file methods table
        fun get(key: LuaValue?): LuaValue {
            return filemethods.get(key)
        }

        // essentially a userdata instance
        fun type(): Int {
            return LuaValue.TUSERDATA
        }

        fun typename(): String? {
            return "userdata"
        }

        // displays as "file" type
        fun tojstring(): String? {
            return "file: " + Integer.toHexString(hashCode())
        }

        fun finalize() {
            if (!isclosed()) {
                try {
                    close()
                } catch (ignore: IOException) {
                }
            }
        }
    }

    /**
     * Wrap the standard input.
     * @return File
     * @throws IOException
     */
    @kotlin.Throws(IOException::class)
    protected abstract fun wrapStdin(): File?

    /**
     * Wrap the standard output.
     * @return File
     * @throws IOException
     */
    @kotlin.Throws(IOException::class)
    protected abstract fun wrapStdout(): File?

    /**
     * Wrap the standard error output.
     * @return File
     * @throws IOException
     */
    @kotlin.Throws(IOException::class)
    protected abstract fun wrapStderr(): File?

    /**
     * Open a file in a particular mode.
     * @param filename
     * @param readMode true if opening in read mode
     * @param appendMode true if opening in append mode
     * @param updateMode true if opening in update mode
     * @param binaryMode true if opening in binary mode
     * @return File object if successful
     * @throws IOException if could not be opened
     */
    @kotlin.Throws(IOException::class)
    protected abstract fun openFile(
        filename: String?,
        readMode: Boolean,
        appendMode: Boolean,
        updateMode: Boolean,
        binaryMode: Boolean
    ): File?

    /**
     * Open a temporary file.
     * @return File object if successful
     * @throws IOException if could not be opened
     */
    @kotlin.Throws(IOException::class)
    protected abstract fun tmpFile(): File?

    /**
     * Start a new process and return a file for input or output
     * @param prog the program to execute
     * @param mode "r" to read, "w" to write
     * @return File to read to or write from
     * @throws IOException if an i/o exception occurs
     */
    @kotlin.Throws(IOException::class)
    protected abstract fun openProgram(prog: String?, mode: String?): File?

    private var infile: File? = null
    private var outfile: File? = null
    private var errfile: File? = null

    var filemethods: LuaTable? = null

    protected var globals: Globals? = null

    fun call(modname: LuaValue?, env: LuaValue): LuaValue {
        globals = env.checkglobals()


        // io lib functions
        val t: LuaTable = LuaTable()
        bind(t, net.blueva.luak.lib.IoLib.IoLibV::class.java, net.blueva.luak.lib.IoLib.Companion.IO_NAMES)


        // create file methods table
        filemethods = LuaTable()
        bind(
            filemethods,
            net.blueva.luak.lib.IoLib.IoLibV::class.java,
            net.blueva.luak.lib.IoLib.Companion.FILE_NAMES,
            net.blueva.luak.lib.IoLib.Companion.FILE_CLOSE
        )

        // set up file metatable
        val mt: LuaTable = LuaTable()
        bind(
            mt,
            net.blueva.luak.lib.IoLib.IoLibV::class.java,
            arrayOf<String>("__index"),
            net.blueva.luak.lib.IoLib.Companion.IO_INDEX
        )
        t.setmetatable(mt)


        // all functions link to library instance
        setLibInstance(t)
        setLibInstance(filemethods)
        setLibInstance(mt)


        // return the table
        env.set("io", t)
        if (!env.get("package").isnil()) env.get("package").get("loaded").set("io", t)
        return t
    }

    private fun setLibInstance(t: LuaTable) {
        val k: Array<LuaValue?> = t.keys()
        var i = 0
        val n = k.size
        while (i < n) {
            (t.get(k[i]) as IoLibV).iolib = this
            i++
        }
    }

    internal class IoLibV : VarArgFunction {
        private var f: File? = null
        var iolib: IoLib? = null
        private var toclose = false
        private var args: Varargs? = null

        constructor()
        constructor(f: File?, name: String?, opcode: Int, iolib: IoLib, toclose: Boolean, args: Varargs) : this(
            f,
            name,
            opcode,
            iolib
        ) {
            this.toclose = toclose
            this.args = args.dealias()
        }

        constructor(f: File?, name: String?, opcode: Int, iolib: IoLib) : super() {
            this.f = f
            this.name = name
            this.opcode = opcode
            this.iolib = iolib
        }

        fun invoke(args: Varargs): Varargs? {
            try {
                when (opcode) {
                    net.blueva.luak.lib.IoLib.Companion.IO_FLUSH -> return iolib!!._io_flush()
                    net.blueva.luak.lib.IoLib.Companion.IO_TMPFILE -> return iolib!!._io_tmpfile()
                    net.blueva.luak.lib.IoLib.Companion.IO_CLOSE -> return iolib!!._io_close(args.arg1())
                    net.blueva.luak.lib.IoLib.Companion.IO_INPUT -> return iolib!!._io_input(args.arg1())
                    net.blueva.luak.lib.IoLib.Companion.IO_OUTPUT -> return iolib!!._io_output(args.arg1())
                    net.blueva.luak.lib.IoLib.Companion.IO_TYPE -> return iolib!!._io_type(args.arg1())
                    net.blueva.luak.lib.IoLib.Companion.IO_POPEN -> return iolib!!._io_popen(
                        args.checkjstring(1),
                        args.optjstring(2, "r")
                    )

                    net.blueva.luak.lib.IoLib.Companion.IO_OPEN -> return iolib!!._io_open(
                        args.checkjstring(1),
                        args.optjstring(2, "r")
                    )

                    net.blueva.luak.lib.IoLib.Companion.IO_LINES -> return iolib!!._io_lines(args)
                    net.blueva.luak.lib.IoLib.Companion.IO_READ -> return iolib!!._io_read(args)
                    net.blueva.luak.lib.IoLib.Companion.IO_WRITE -> return iolib!!._io_write(args)

                    net.blueva.luak.lib.IoLib.Companion.FILE_CLOSE -> return iolib!!._file_close(args.arg1())
                    net.blueva.luak.lib.IoLib.Companion.FILE_FLUSH -> return iolib!!._file_flush(args.arg1())
                    net.blueva.luak.lib.IoLib.Companion.FILE_SETVBUF -> return iolib!!._file_setvbuf(
                        args.arg1(),
                        args.checkjstring(2),
                        args.optint(3, 8192)
                    )

                    net.blueva.luak.lib.IoLib.Companion.FILE_LINES -> return iolib!!._file_lines(args)
                    net.blueva.luak.lib.IoLib.Companion.FILE_READ -> return iolib!!._file_read(
                        args.arg1(),
                        args.subargs(2)
                    )

                    net.blueva.luak.lib.IoLib.Companion.FILE_SEEK -> return iolib!!._file_seek(
                        args.arg1(),
                        args.optjstring(2, "cur"),
                        args.optint(3, 0)
                    )

                    net.blueva.luak.lib.IoLib.Companion.FILE_WRITE -> return iolib!!._file_write(
                        args.arg1(),
                        args.subargs(2)
                    )

                    net.blueva.luak.lib.IoLib.Companion.IO_INDEX -> return iolib!!._io_index(args.arg(2))
                    net.blueva.luak.lib.IoLib.Companion.LINES_ITER -> return iolib!!._lines_iter(f, toclose, this.args)
                }
            } catch (ioe: IOException) {
                if (opcode === net.blueva.luak.lib.IoLib.Companion.LINES_ITER) {
                    val s: String? = ioe.getMessage()
                    error(if (s != null) s else ioe.toString())
                }
                return errorresult(ioe)
            }
            return NONE
        }
    }

    private fun input(): File? {
        return if (infile != null) infile else (ioopenfile(
            net.blueva.luak.lib.IoLib.Companion.FTYPE_STDIN,
            "-",
            "r"
        ).also { infile = it })
    }

    //	io.flush() -> bool
    @kotlin.Throws(IOException::class)
    fun _io_flush(): Varargs {
        net.blueva.luak.lib.IoLib.Companion.checkopen(output())
        outfile!!.flush()
        return LuaValue.TRUE
    }

    //	io.tmpfile() -> file
    @kotlin.Throws(IOException::class)
    fun _io_tmpfile(): Varargs? {
        return tmpFile()
    }

    //	io.close([file]) -> void
    @kotlin.Throws(IOException::class)
    fun _io_close(file: LuaValue): Varargs {
        val f = if (file.isnil()) output() else net.blueva.luak.lib.IoLib.Companion.checkfile(file)
        net.blueva.luak.lib.IoLib.Companion.checkopen(f)
        return net.blueva.luak.lib.IoLib.Companion.ioclose(f)
    }

    //	io.input([file]) -> file
    fun _io_input(file: LuaValue): Varargs? {
        infile = if (file.isnil()) input() else if (file.isstring()) ioopenfile(
            net.blueva.luak.lib.IoLib.Companion.FTYPE_NAMED,
            file.checkjstring(),
            "r"
        ) else net.blueva.luak.lib.IoLib.Companion.checkfile(file)
        return infile
    }

    // io.output(filename) -> file
    fun _io_output(filename: LuaValue): Varargs? {
        outfile = if (filename.isnil()) output() else if (filename.isstring()) ioopenfile(
            net.blueva.luak.lib.IoLib.Companion.FTYPE_NAMED,
            filename.checkjstring(),
            "w"
        ) else net.blueva.luak.lib.IoLib.Companion.checkfile(filename)
        return outfile
    }

    //	io.type(obj) -> "file" | "closed file" | nil
    fun _io_type(obj: LuaValue?): Varargs? {
        val f: File? = net.blueva.luak.lib.IoLib.Companion.optfile(obj)
        return if (f != null) if (f.isclosed()) net.blueva.luak.lib.IoLib.Companion.CLOSED_FILE else net.blueva.luak.lib.IoLib.Companion.FILE else NIL
    }

    // io.popen(prog, [mode]) -> file
    @kotlin.Throws(IOException::class)
    fun _io_popen(prog: String?, mode: String?): Varargs? {
        if (!"r".equals(mode) && !"w".equals(mode)) argerror(
            2,
            "invalid value: '" + mode + "'; must be one of 'r' or 'w'"
        )
        return openProgram(prog, mode)
    }

    //	io.open(filename, [mode]) -> file | nil,err
    @kotlin.Throws(IOException::class)
    fun _io_open(filename: String?, mode: String): Varargs? {
        return rawopenfile(net.blueva.luak.lib.IoLib.Companion.FTYPE_NAMED, filename, mode)
    }

    //	io.lines(filename, ...) -> iterator
    fun _io_lines(args: Varargs): Varargs? {
        val filename: String? = args.optjstring(1, null)
        val infile = if (filename == null) input() else ioopenfile(
            net.blueva.luak.lib.IoLib.Companion.FTYPE_NAMED,
            filename,
            "r"
        )
        net.blueva.luak.lib.IoLib.Companion.checkopen(infile)
        return lines(infile, filename != null, args.subargs(2))
    }

    //	io.read(...) -> (...)
    @kotlin.Throws(IOException::class)
    fun _io_read(args: Varargs): Varargs {
        net.blueva.luak.lib.IoLib.Companion.checkopen(input())
        return ioread(infile!!, args)
    }

    //	io.write(...) -> void
    @kotlin.Throws(IOException::class)
    fun _io_write(args: Varargs): Varargs {
        net.blueva.luak.lib.IoLib.Companion.checkopen(output())
        return net.blueva.luak.lib.IoLib.Companion.iowrite(outfile, args)
    }

    // file:close() -> void
    @kotlin.Throws(IOException::class)
    fun _file_close(file: LuaValue?): Varargs {
        return net.blueva.luak.lib.IoLib.Companion.ioclose(net.blueva.luak.lib.IoLib.Companion.checkfile(file))
    }

    // file:flush() -> void
    @kotlin.Throws(IOException::class)
    fun _file_flush(file: LuaValue?): Varargs {
        net.blueva.luak.lib.IoLib.Companion.checkfile(file).flush()
        return LuaValue.TRUE
    }

    // file:setvbuf(mode,[size]) -> void
    fun _file_setvbuf(file: LuaValue?, mode: String?, size: Int): Varargs {
        if ("no".equals(mode)) {
        } else if ("full".equals(mode)) {
        } else if ("line".equals(mode)) {
        } else {
            argerror(1, "invalid value: '" + mode + "'; must be one of 'no', 'full' or 'line'")
        }
        net.blueva.luak.lib.IoLib.Companion.checkfile(file).setvbuf(mode, size)
        return LuaValue.TRUE
    }

    // file:lines(...) -> iterator
    fun _file_lines(args: Varargs): Varargs? {
        return lines(net.blueva.luak.lib.IoLib.Companion.checkfile(args.arg1()), false, args.subargs(2))
    }

    //	file:read(...) -> (...)
    @kotlin.Throws(IOException::class)
    fun _file_read(file: LuaValue?, subargs: Varargs): Varargs {
        return ioread(net.blueva.luak.lib.IoLib.Companion.checkfile(file), subargs)
    }

    //  file:seek([whence][,offset]) -> pos | nil,error
    @kotlin.Throws(IOException::class)
    fun _file_seek(file: LuaValue?, whence: String?, offset: Int): Varargs {
        if ("set".equals(whence)) {
        } else if ("end".equals(whence)) {
        } else if ("cur".equals(whence)) {
        } else {
            argerror(1, "invalid value: '" + whence + "'; must be one of 'set', 'cur' or 'end'")
        }
        return valueOf(net.blueva.luak.lib.IoLib.Companion.checkfile(file).seek(whence, offset))
    }

    //	file:write(...) -> void
    @kotlin.Throws(IOException::class)
    fun _file_write(file: LuaValue?, subargs: Varargs): Varargs {
        return net.blueva.luak.lib.IoLib.Companion.iowrite(net.blueva.luak.lib.IoLib.Companion.checkfile(file), subargs)
    }

    // __index, returns a field
    fun _io_index(v: LuaValue): Varargs? {
        return if (v.equals(net.blueva.luak.lib.IoLib.Companion.STDOUT)) output() else if (v.equals(net.blueva.luak.lib.IoLib.Companion.STDIN)) input() else if (v.equals(
                net.blueva.luak.lib.IoLib.Companion.STDERR
            )
        ) errput() else NIL
    }

    //	lines iterator(s,var) -> var'
    @kotlin.Throws(IOException::class)
    fun _lines_iter(file: LuaValue?, toclose: Boolean, args: Varargs): Varargs {
        val f: File? = net.blueva.luak.lib.IoLib.Companion.optfile(file)
        if (f == null) argerror(1, "not a file: " + file)
        if (f!!.isclosed()) error("file is already closed")
        val ret: Varargs = ioread(f, args)
        if (toclose && ret.isnil(1) && f.eof()) f.close()
        return ret
    }

    private fun output(): File {
        return (if (outfile != null) outfile else (ioopenfile(
            net.blueva.luak.lib.IoLib.Companion.FTYPE_STDOUT,
            "-",
            "w"
        ).also { outfile = it }))!!
    }

    private fun errput(): File? {
        return if (errfile != null) errfile else (ioopenfile(
            net.blueva.luak.lib.IoLib.Companion.FTYPE_STDERR,
            "-",
            "w"
        ).also { errfile = it })
    }

    private fun ioopenfile(filetype: Int, filename: String?, mode: String): File? {
        try {
            return rawopenfile(filetype, filename, mode)
        } catch (e: Exception) {
            error("io error: " + e.getMessage())
            return null
        }
    }

    private fun lines(f: File?, toclose: Boolean, args: Varargs): Varargs? {
        try {
            return net.blueva.luak.lib.IoLib.IoLibV(
                f,
                "lnext",
                net.blueva.luak.lib.IoLib.Companion.LINES_ITER,
                this,
                toclose,
                args
            )
        } catch (e: Exception) {
            return error("lines: " + e)
        }
    }

    @kotlin.Throws(IOException::class)
    private fun ioread(f: File, args: Varargs): Varargs {
        var i: Int
        val n: Int = args.narg()
        if (n == 0) return net.blueva.luak.lib.IoLib.Companion.freadline(f, false)
        val v: Array<LuaValue?> = arrayOfNulls<LuaValue>(n)
        var ai: LuaValue?
        var vi: LuaValue?
        var fmt: LuaString
        i = 0
        while (i < n) {
            item@ when ((args.arg(i + 1).also { ai = it }).type()) {
                LuaValue.TNUMBER -> {
                    vi = net.blueva.luak.lib.IoLib.Companion.freadbytes(f, ai.toint())
                    break@item
                }

                LuaValue.TSTRING -> {
                    fmt = ai.checkstring()
                    if (fmt.m_length >= 2 && fmt.m_bytes[fmt.m_offset] === '*') {
                        when (fmt.m_bytes[fmt.m_offset + 1]) {
                            'n' -> {
                                vi = net.blueva.luak.lib.IoLib.Companion.freadnumber(f)
                                break@item
                            }

                            'l' -> {
                                vi = net.blueva.luak.lib.IoLib.Companion.freadline(f, false)
                                break@item
                            }

                            'L' -> {
                                vi = net.blueva.luak.lib.IoLib.Companion.freadline(f, true)
                                break@item
                            }

                            'a' -> {
                                vi = net.blueva.luak.lib.IoLib.Companion.freadall(f)
                                break@item
                            }
                        }
                    }
                    return argerror(i + 1, "(invalid format)")
                }

                else -> return argerror(i + 1, "(invalid format)")
            }
            if ((vi.also { v[i++] = it }).isnil()) break
        }
        return if (i == 0) NIL else varargsOf(v, 0, i)
    }

    @kotlin.Throws(IOException::class)
    private fun rawopenfile(filetype: Int, filename: String?, mode: String): File? {
        var len: Int = mode.length()
        var i = 0
        while (i < len) {
            // [rwa][+]?b*
            val ch: Char = mode.charAt(i)
            if (i == 0 && "rwa".indexOf(ch) >= 0) {
                i++
                continue
            }
            if (i == 1 && ch == '+') {
                i++
                continue
            }
            if (i >= 1 && ch == 'b') {
                i++
                continue
            }
            len = -1
            break
            i++
        }
        if (len <= 0) argerror(2, "invalid mode: '" + mode + "'")

        when (filetype) {
            net.blueva.luak.lib.IoLib.Companion.FTYPE_STDIN -> return wrapStdin()
            net.blueva.luak.lib.IoLib.Companion.FTYPE_STDOUT -> return wrapStdout()
            net.blueva.luak.lib.IoLib.Companion.FTYPE_STDERR -> return wrapStderr()
        }
        val isreadmode: Boolean = mode.startsWith("r")
        val isappend: Boolean = mode.startsWith("a")
        val isupdate = mode.indexOf('+') > 0
        val isbinary: Boolean = mode.endsWith("b")
        return openFile(filename, isreadmode, isappend, isupdate, isbinary)
    }


    companion object {
        /** Enumerated value representing stdin  */
        protected const val FTYPE_STDIN: Int = 0

        /** Enumerated value representing stdout  */
        protected const val FTYPE_STDOUT: Int = 1

        /** Enumerated value representing stderr  */
        protected const val FTYPE_STDERR: Int = 2

        /** Enumerated value representing a file type for a named file  */
        protected const val FTYPE_NAMED: Int = 3

        private val STDIN: LuaValue? = valueOf("stdin")
        private val STDOUT: LuaValue? = valueOf("stdout")
        private val STDERR: LuaValue? = valueOf("stderr")
        private val FILE: LuaValue? = valueOf("file")
        private val CLOSED_FILE: LuaValue? = valueOf("closed file")

        private const val IO_CLOSE = 0
        private const val IO_FLUSH = 1
        private const val IO_INPUT = 2
        private const val IO_LINES = 3
        private const val IO_OPEN = 4
        private const val IO_OUTPUT = 5
        private const val IO_POPEN = 6
        private const val IO_READ = 7
        private const val IO_TMPFILE = 8
        private const val IO_TYPE = 9
        private const val IO_WRITE = 10

        private const val FILE_CLOSE = 11
        private const val FILE_FLUSH = 12
        private const val FILE_LINES = 13
        private const val FILE_READ = 14
        private const val FILE_SEEK = 15
        private const val FILE_SETVBUF = 16
        private const val FILE_WRITE = 17

        private const val IO_INDEX = 18
        private const val LINES_ITER = 19

        val IO_NAMES: Array<String?> = arrayOf<String?>(
            "close",
            "flush",
            "input",
            "lines",
            "open",
            "output",
            "popen",
            "read",
            "tmpfile",
            "type",
            "write",
        )

        val FILE_NAMES: Array<String?> = arrayOf<String?>(
            "close",
            "flush",
            "lines",
            "read",
            "seek",
            "setvbuf",
            "write",
        )

        @kotlin.Throws(IOException::class)
        private fun ioclose(f: File): Varargs {
            if (f.isstdfile()) return net.blueva.luak.lib.IoLib.Companion.errorresult("cannot close standard file")
            else {
                f.close()
                return net.blueva.luak.lib.IoLib.Companion.successresult()
            }
        }

        private fun successresult(): Varargs {
            return LuaValue.TRUE
        }

        fun errorresult(ioe: Exception): Varargs {
            val s: String? = ioe.getMessage()
            return net.blueva.luak.lib.IoLib.Companion.errorresult("io error: " + (if (s != null) s else ioe.toString()))
        }

        private fun errorresult(errortext: String?): Varargs {
            return varargsOf(NIL, valueOf(errortext))
        }

        @kotlin.Throws(IOException::class)
        private fun iowrite(f: File, args: Varargs): Varargs {
            var i = 1
            val n: Int = args.narg()
            while (i <= n) {
                f.write(args.checkstring(i))
                i++
            }
            return f
        }

        private fun checkfile(`val`: LuaValue?): File {
            val f: File? = net.blueva.luak.lib.IoLib.Companion.optfile(`val`)
            if (f == null) argerror(1, "file")
            net.blueva.luak.lib.IoLib.Companion.checkopen(f)
            return f!!
        }

        private fun optfile(`val`: LuaValue?): File? {
            return if (`val` is File) `val` as File? else null
        }

        private fun checkopen(file: File): File {
            if (file.isclosed()) error("attempt to use a closed file")
            return file
        }

        // ------------- file reading utilitied ------------------
        @kotlin.Throws(IOException::class)
        fun freadbytes(f: File, count: Int): LuaValue {
            if (count == 0) return if (f.eof()) NIL else EMPTYSTRING
            val b = ByteArray(count)
            val r: Int
            if ((f.read(b, 0, b.size).also { r = it }) < 0) return NIL
            return LuaString.valueUsing(b, 0, r)
        }

        @kotlin.Throws(IOException::class)
        fun freaduntil(f: File, lineonly: Boolean, withend: Boolean): LuaValue {
            val baos: ByteArrayOutputStream = ByteArrayOutputStream()
            var c: Int
            try {
                if (lineonly) {
                    loop@ while ((f.read().also { c = it }) >= 0) {
                        when (c) {
                            '\r' -> if (withend) baos.write(c)
                            '\n' -> {
                                if (withend) baos.write(c)
                                break@loop
                            }

                            else -> baos.write(c)
                        }
                    }
                } else {
                    while ((f.read().also { c = it }) >= 0) baos.write(c)
                }
            } catch (e: EOFException) {
                c = -1
            }
            return if (c < 0 && baos.size() === 0) NIL as LuaValue else LuaString.valueUsing(baos.toByteArray()) as LuaValue?
        }

        @kotlin.Throws(IOException::class)
        fun freadline(f: File, withend: Boolean): LuaValue {
            return net.blueva.luak.lib.IoLib.Companion.freaduntil(f, true, withend)
        }

        @kotlin.Throws(IOException::class)
        fun freadall(f: File): LuaValue? {
            val n = f.remaining()
            if (n >= 0) {
                return if (n == 0) EMPTYSTRING else net.blueva.luak.lib.IoLib.Companion.freadbytes(f, n)
            } else {
                return net.blueva.luak.lib.IoLib.Companion.freaduntil(f, false, false)
            }
        }

        @kotlin.Throws(IOException::class)
        fun freadnumber(f: File): LuaValue {
            val baos: ByteArrayOutputStream = ByteArrayOutputStream()
            net.blueva.luak.lib.IoLib.Companion.freadchars(f, " \t\r\n", null)
            net.blueva.luak.lib.IoLib.Companion.freadchars(f, "-+", baos)
            //freadchars(f,"0",baos);
            //freadchars(f,"xX",baos);
            net.blueva.luak.lib.IoLib.Companion.freadchars(f, "0123456789", baos)
            net.blueva.luak.lib.IoLib.Companion.freadchars(f, ".", baos)
            net.blueva.luak.lib.IoLib.Companion.freadchars(f, "0123456789", baos)
            //freadchars(f,"eEfFgG",baos);
            // freadchars(f,"+-",baos);
            //freadchars(f,"0123456789",baos);
            val s = baos.toString()
            return if (s.length() > 0) valueOf(Double.parseDouble(s)) else NIL
        }

        @kotlin.Throws(IOException::class)
        private fun freadchars(f: File, chars: String, baos: ByteArrayOutputStream?) {
            var c: Int
            while (true) {
                c = f.peek()
                if (chars.indexOf(c) < 0) {
                    return
                }
                f.read()
                if (baos != null) baos.write(c)
            }
        }
    }
}
