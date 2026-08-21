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
package net.blueva.luak

import net.blueva.luak.io.DataInputStream
import net.blueva.luak.io.IOException
import net.blueva.luak.io.InputStream

/**
 * Class to undump compiled lua bytecode into a [Prototype] instances.
 * 
 * 
 * The [LoadState] class provides the default [Globals.Undumper]
 * which is used to undump a string of bytes that represent a lua binary file
 * using either the C-based lua compiler, or BlueLuaK's
 * [net.blueva.luak.compiler.LuaC] compiler.
 * 
 * 
 * The canonical method to load and execute code is done
 * indirectly using the Globals:
 * <pre> `Globals globals = JvmPlatform.standardGlobals(); LuaValue chunk = globasl.load("print('hello, world')", "main.lua"); chunk.call(); ` </pre>
 * This should work regardless of which [Globals.Compiler] or [Globals.Undumper]
 * have been installed.
 * 
 * 
 * By default, when using [net.blueva.luak.lib.LuaPlatform] or
 * [net.blueva.luak.lib.jvm.JvmPlatform]
 * to construct globals, the [LoadState] default undumper is installed
 * as the default [Globals.Undumper].
 * 
 * 
 * 
 * A lua binary file is created via the [net.blueva.luak.compiler.DumpState] class
 * :
 * <pre> `Globals globals = JvmPlatform.standardGlobals(); Prototype p = globals.compilePrototype(new StringReader("print('hello, world')"), "main.lua"); ByteArrayOutputStream o = new ByteArrayOutputStream(); net.blueva.luak.compiler.DumpState.dump(p, o, false); byte[] lua_binary_file_bytes = o.toByteArray(); ` </pre>
 * 
 * The [LoadState]'s default undumper [.instance]
 * may be used directly to undump these bytes:
 * <pre> `Prototypep = LoadState.instance.undump(new ByteArrayInputStream(lua_binary_file_bytes), "main.lua"); LuaClosure c = new LuaClosure(p, globals); c.call(); ` </pre>
 * 
 * 
 * More commonly, the [Globals.Undumper] may be used to undump them:
 * <pre> `Prototype p = globals.loadPrototype(new ByteArrayInputStream(lua_binary_file_bytes), "main.lua", "b"); LuaClosure c = new LuaClosure(p, globals); c.call(); ` </pre>
 * 
 * @see Globals.Compiler
 * 
 * @see Globals.Undumper
 * 
 * @see LuaClosure
 * 
 * @see LuaFunction
 * 
 * @see net.blueva.luak.compiler.LuaC
 * 
 * @see net.blueva.luak.luajc.LuaJC
 * 
 * @see Globals.compiler
 * 
 * @see Globals.load
 */
class LoadState private constructor(
    stream: InputStream?,
    /** Name of what is being loaded?  */
    var name: String?
) {
    // values read from the header
    private var luacVersion = 0
    private var luacFormat = 0
    private var luacLittleEndian = false
    private var luacSizeofInt = 0
    private var luacSizeofSizeT = 0
    private var luacSizeofInstruction = 0
    private var luacSizeofLuaNumber = 0
    private var luacNumberFormat = 0

    /** input stream from which we are loading  */
    val `is`: DataInputStream

    /** Read buffer  */
    private var buf = ByteArray(512)

    /** Load a 4-byte int value from the input stream
     * @return the int value laoded.
     */
    @kotlin.Throws(IOException::class)
    fun loadInt(): Int {
        `is`.readFully(buf, 0, 4)
        return if (luacLittleEndian) (buf[3].toInt() shl 24) or ((0xff and buf[2].toInt()) shl 16) or ((0xff and buf[1].toInt()) shl 8) or (0xff and buf[0].toInt()) else (buf[0].toInt() shl 24) or ((0xff and buf[1].toInt()) shl 16) or ((0xff and buf[2].toInt()) shl 8) or (0xff and buf[3].toInt())
    }

    /** Load an array of int values from the input stream
     * @return the array of int values laoded.
     */
    @kotlin.Throws(IOException::class)
    fun loadIntArray(): IntArray {
        val n = loadInt()
        if (n == 0) return net.blueva.luak.LoadState.Companion.NOINTS


        // read all data at once
        val m = n shl 2
        if (buf.size < m) buf = ByteArray(m)
        `is`.readFully(buf, 0, m)
        val array = IntArray(n)
        var i = 0
        var j = 0
        while (i < n) {
            array[i] =
                if (luacLittleEndian) (buf[j + 3].toInt() shl 24) or ((0xff and buf[j + 2].toInt()) shl 16) or ((0xff and buf[j + 1].toInt()) shl 8) or (0xff and buf[j + 0].toInt()) else (buf[j + 0].toInt() shl 24) or ((0xff and buf[j + 1].toInt()) shl 16) or ((0xff and buf[j + 2].toInt()) shl 8) or (0xff and buf[j + 3].toInt())
            ++i
            j += 4
        }

        return array
    }

    /** Load a long  value from the input stream
     * @return the long value laoded.
     */
    @kotlin.Throws(IOException::class)
    fun loadInt64(): Long {
        val a: Int
        val b: Int
        if (this.luacLittleEndian) {
            a = loadInt()
            b = loadInt()
        } else {
            b = loadInt()
            a = loadInt()
        }
        return ((b.toLong()) shl 32) or ((a.toLong()) and 0xffffffffL)
    }

    /** Load a lua strin gvalue from the input stream
     * @return the [LuaString] value laoded.
     */
    @kotlin.Throws(IOException::class)
    fun loadString(): LuaString? {
        val size = if (this.luacSizeofSizeT == 8) loadInt64().toInt() else loadInt()
        if (size == 0) return null
        val bytes = ByteArray(size)
        `is`.readFully(bytes, 0, size)
        return LuaString.valueUsing(bytes, 0, bytes.size - 1)
    }

    /**
     * Load a number from a binary chunk
     * @return the [LuaValue] loaded
     * @throws IOException if an i/o exception occurs
     */
    @kotlin.Throws(IOException::class)
    fun loadNumber(): LuaValue {
        if (luacNumberFormat == net.blueva.luak.LoadState.Companion.NUMBER_FORMAT_INTS_ONLY) {
            return LuaInteger.valueOf(loadInt())!!
        } else {
            return net.blueva.luak.LoadState.Companion.longBitsToLuaNumber(loadInt64())
        }
    }

    /**
     * Load a list of constants from a binary chunk
     * @param f the function prototype
     * @throws IOException if an i/o exception occurs
     */
    @kotlin.Throws(IOException::class)
    fun loadConstants(f: Prototype) {
        var n = loadInt()
        val values: Array<LuaValue?> =
            if (n > 0) arrayOfNulls<LuaValue>(n) else net.blueva.luak.LoadState.Companion.NOVALUES
        for (i in 0..<n) {
            when (`is`.readByte().toInt()) {
                net.blueva.luak.LoadState.Companion.LUA_TNIL -> values[i] = LuaValue.NIL
                net.blueva.luak.LoadState.Companion.LUA_TBOOLEAN -> values[i] =
                    (if (0 != `is`.readUnsignedByte()) LuaValue.TRUE else LuaValue.FALSE)

                net.blueva.luak.LoadState.Companion.LUA_TINT -> values[i] = LuaInteger.valueOf(loadInt())!!
                net.blueva.luak.LoadState.Companion.LUA_TNUMINT -> values[i] = LuaInteger.valueOf(loadInt64())!!
                net.blueva.luak.LoadState.Companion.LUA_TNUMBER -> values[i] = loadNumber()
                net.blueva.luak.LoadState.Companion.LUA_TSTRING -> values[i] = loadString()
                else -> throw IllegalStateException("bad constant")
            }
        }
        f.k = values

        n = loadInt()
        val protos: Array<Prototype?>? =
            if (n > 0) arrayOfNulls<Prototype>(n) else net.blueva.luak.LoadState.Companion.NOPROTOS
        for (i in 0..<n) protos!![i] = loadFunction(f.source)
        f.p = protos
    }


    @kotlin.Throws(IOException::class)
    fun loadUpvalues(f: Prototype) {
        val n = loadInt()
        f.upvalues = if (n > 0) arrayOfNulls<Upvaldesc>(n) else net.blueva.luak.LoadState.Companion.NOUPVALDESCS
        for (i in 0..<n) {
            val instack = `is`.readByte().toInt() != 0
            val idx = (`is`.readByte().toInt()) and 0xff
            f.upvalues!![i] = Upvaldesc(null, instack, idx)
        }
    }

    /**
     * Load the debug info for a function prototype
     * @param f the function Prototype
     * @throws IOException if there is an i/o exception
     */
    @kotlin.Throws(IOException::class)
    fun loadDebug(f: Prototype) {
        f.source = loadString()
        f.lineinfo = loadIntArray()
        var n = loadInt()
        f.locvars = if (n > 0) arrayOfNulls<LocVars>(n) else net.blueva.luak.LoadState.Companion.NOLOCVARS
        for (i in 0..<n) {
            val varname: LuaString? = loadString()
            val startpc = loadInt()
            val endpc = loadInt()
            f.locvars[i] = LocVars(varname, startpc, endpc)
        }

        n = loadInt()
        for (i in 0..<n) f.upvalues!![i]!!.name = loadString()
    }

    /**
     * Load a function prototype from the input stream
     * @param p name of the source
     * @return [Prototype] instance that was loaded
     * @throws IOException
     */
    @kotlin.Throws(IOException::class)
    fun loadFunction(p: LuaString?): Prototype {
        val f: Prototype = Prototype()
        ////		this.L.push(f);
//		f.source = loadString();
//		if ( f.source == null )
//			f.source = p;
        f.linedefined = loadInt()
        f.lastlinedefined = loadInt()
        f.numparams = `is`.readUnsignedByte()
        f.is_vararg = `is`.readUnsignedByte()
        f.maxstacksize = `is`.readUnsignedByte()
        f.code = loadIntArray()
        loadConstants(f)
        loadUpvalues(f)
        loadDebug(f)


        // TODO: add check here, for debugging purposes, I believe
        // see ldebug.c
//		 IF (!luaG_checkcode(f), "bad code");

//		 this.L.pop();
        return f
    }

    /**
     * Load the lua chunk header values.
     * @throws IOException if an i/o exception occurs.
     */
    @kotlin.Throws(IOException::class)
    fun loadHeader() {
        luacVersion = `is`.readByte().toInt()
        luacFormat = `is`.readByte().toInt()
        luacLittleEndian = (0 != `is`.readByte().toInt())
        luacSizeofInt = `is`.readByte().toInt()
        luacSizeofSizeT = `is`.readByte().toInt()
        luacSizeofInstruction = `is`.readByte().toInt()
        luacSizeofLuaNumber = `is`.readByte().toInt()
        luacNumberFormat = `is`.readByte().toInt()
        for (i in net.blueva.luak.LoadState.Companion.LUAC_TAIL.indices) if (`is`.readByte().toInt() != net.blueva.luak.LoadState.Companion.LUAC_TAIL[i].toInt()) throw LuaError(
            "Unexpeted byte in luac tail of header, index=" + i
        )
    }

    /** Private constructor for create a load state  */
    init {
        this.`is` = DataInputStream(stream!!)
    }

    private class GlobalsUndumper : Globals.Undumper {
        @kotlin.Throws(IOException::class)
        override fun undump(stream: InputStream?, chunkname: String?): Prototype? {
            return net.blueva.luak.LoadState.Companion.undump(stream!!, chunkname!!)
        }
    }

    companion object {
        /** Shared instance of Globals.Undumper to use loading prototypes from binary lua files  */
        val instance: Globals.Undumper = net.blueva.luak.LoadState.GlobalsUndumper()

        /** format corresponding to non-number-patched lua, all numbers are floats or doubles  */
        const val NUMBER_FORMAT_FLOATS_OR_DOUBLES: Int = 0

        /** format corresponding to non-number-patched lua, all numbers are ints  */
        const val NUMBER_FORMAT_INTS_ONLY: Int = 1

        /** format corresponding to number-patched lua, all numbers are 32-bit (4 byte) ints  */
        const val NUMBER_FORMAT_NUM_PATCH_INT32: Int = 4

        // type constants
        val LUA_TINT: Int = (-2)
        val LUA_TNONE: Int = (-1)
        const val LUA_TNIL: Int = 0
        const val LUA_TBOOLEAN: Int = 1
        const val LUA_TLIGHTUSERDATA: Int = 2
        const val LUA_TNUMBER: Int = 3

        /**
         * Constant tag for the integer subtype of a number.
         *
         * Upstream tags a dumped constant with its full type tag, of which the
         * number type has two variants: `LUA_VNUMFLT` is the plain number tag
         * and `LUA_VNUMINT` sets the variant bit. Without the distinction a
         * dumped chunk cannot say which subtype a numeral had.
         */
        const val LUA_TNUMINT: Int = 3 or (1 shl 4)
        const val LUA_TSTRING: Int = 4
        const val LUA_TTABLE: Int = 5
        const val LUA_TFUNCTION: Int = 6
        const val LUA_TUSERDATA: Int = 7
        const val LUA_TTHREAD: Int = 8
        const val LUA_TVALUE: Int = 9

        /** The character encoding to use for file encoding.  Null means the default encoding  */
        var encoding: String? = null

        /** Signature byte indicating the file is a compiled binary chunk  */
        val LUA_SIGNATURE: ByteArray =
            byteArrayOf('\u001b'.code.toByte(), 'L'.code.toByte(), 'u'.code.toByte(), 'a'.code.toByte())

        /** Data to catch conversion errors  */
        val LUAC_TAIL: ByteArray = byteArrayOf(
            0x19.toByte(),
            0x93.toByte(),
            '\r'.code.toByte(),
            '\n'.code.toByte(),
            0x1a.toByte(),
            '\n'.code.toByte(),
        )


        /** Name for compiled chunks  */
        val SOURCE_BINARY_STRING: String = "binary string"


        /** for header of binary files -- this is Lua 5.2  */
        const val LUAC_VERSION: Int = 0x52

        /** for header of binary files -- this is the official format  */
        const val LUAC_FORMAT: Int = 0

        /** size of header of binary files  */
        const val LUAC_HEADERSIZE: Int = 12

        private val NOVALUES: Array<LuaValue?> = arrayOf<LuaValue?>()
        private val NOPROTOS: Array<Prototype?> = arrayOf<Prototype?>()
        private val NOLOCVARS: Array<LocVars?> = arrayOf<LocVars?>()
        private val NOUPVALDESCS: Array<Upvaldesc?> = arrayOf<Upvaldesc?>()
        private val NOINTS = intArrayOf()

        /** Install this class as the standard Globals.Undumper for the supplied Globals  */
        fun install(globals: Globals) {
            globals.undumper = net.blueva.luak.LoadState.Companion.instance
        }

        /**
         * Convert bits in a long value to a [LuaValue].
         * @param bits long value containing the bits
         * @return [LuaInteger] or [LuaDouble] whose value corresponds to the bits provided.
         */
        fun longBitsToLuaNumber(bits: Long): LuaValue {
            // A float constant stays a float. This used to hand back an integer
            // whenever the double had no fractional part, which turned a dumped
            // `2.0` into a `2` on the way back in, and a dumped `-0.0` into a
            // plain zero.
            return LuaValue.valueOf(Double.fromBits(bits))
        }

        /**
         * Load input stream as a lua binary chunk if the first 4 bytes are the lua binary signature.
         * @param stream InputStream to read, after having read the first byte already
         * @param chunkname Name to apply to the loaded chunk
         * @return [Prototype] that was loaded, or null if the first 4 bytes were not the lua signature.
         * @throws IOException if an IOException occurs
         */
        @kotlin.Throws(IOException::class)
        fun undump(stream: InputStream, chunkname: String): Prototype? {
            // check rest of signature
            if (stream.read() != LUA_SIGNATURE[0].toInt() || stream.read() != LUA_SIGNATURE[1].toInt() || stream.read() != LUA_SIGNATURE[2].toInt() || stream.read() != LUA_SIGNATURE[3].toInt()) return null


            // load file as a compiled chunk
            val sname: String? = net.blueva.luak.LoadState.Companion.getSourceName(chunkname)
            val s: LoadState = net.blueva.luak.LoadState(stream, sname)
            s.loadHeader()

            // check format
            when (s.luacNumberFormat) {
                net.blueva.luak.LoadState.Companion.NUMBER_FORMAT_FLOATS_OR_DOUBLES, net.blueva.luak.LoadState.Companion.NUMBER_FORMAT_INTS_ONLY, net.blueva.luak.LoadState.Companion.NUMBER_FORMAT_NUM_PATCH_INT32 -> {}
                else -> throw LuaError("unsupported int size")
            }
            return s.loadFunction(LuaString.valueOf(sname!!))
        }

        /**
         * Construct a source name from a supplied chunk name
         * @param name String name that appears in the chunk
         * @return source file name
         */
        fun getSourceName(name: String): String? {
            var sname: String? = name
            if (name.startsWith("@") || name.startsWith("=")) sname = name.substring(1)
            else if (name.startsWith("\u001b")) sname = net.blueva.luak.LoadState.Companion.SOURCE_BINARY_STRING
            return sname
        }
    }
}
