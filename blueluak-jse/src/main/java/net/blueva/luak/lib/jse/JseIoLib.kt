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

import net.blueva.luak.LuaError
import net.blueva.luak.LuaString
import net.blueva.luak.lib.IoLib
import java.io.*

/**
 * Subclass of [IoLib] and therefore [LibFunction] which implements the lua standard `io`
 * library for the JSE platform.
 * 
 * 
 * It uses RandomAccessFile to implement seek on files.
 * 
 * 
 * Typically, this library is included as part of a call to
 * [JsePlatform.standardGlobals]
 * <pre> `Globals globals = JsePlatform.standardGlobals(); globals.get("io").get("write").call(LuaValue.valueOf("hello, world\n")); ` </pre>
 * 
 * 
 * For special cases where the smallest possible footprint is desired,
 * a minimal set of libraries could be loaded
 * directly via [Globals.load] using code such as:
 * <pre> `Globals globals = new Globals(); globals.load(new JseBaseLib()); globals.load(new PackageLib()); globals.load(new JseIoLib()); globals.get("io").get("write").call(LuaValue.valueOf("hello, world\n")); ` </pre>
 * 
 * However, other libraries such as *MathLib* are not loaded in this case.
 * 
 * 
 * This has been implemented to match as closely as possible the behavior in the corresponding library in C.
 * @see LibFunction
 * 
 * @see JsePlatform
 * 
 * @see net.blueva.luak.lib.jme.JmePlatform
 * 
 * @see IoLib
 * 
 * @see net.blueva.luak.lib.jme.JmeIoLib
 * 
 * @see [Lua 5.2 I/O Lib Reference](http://www.lua.org/manual/5.2/manual.html.6.8)
 */
class JseIoLib : IoLib() {
    @Throws(IOException::class)
    override fun wrapStdin(): File {
        return StdinFile()
    }

    @Throws(IOException::class)
    override fun wrapStdout(): File {
        return StdoutFile(FTYPE_STDOUT)
    }

    @Throws(IOException::class)
    override fun wrapStderr(): File {
        return StdoutFile(FTYPE_STDERR)
    }

    @Throws(IOException::class)
    override fun openFile(
        filename: String?,
        readMode: Boolean,
        appendMode: Boolean,
        updateMode: Boolean,
        binaryMode: Boolean
    ): File {
        val f = RandomAccessFile(filename, if (readMode) "r" else "rw")
        if (appendMode) {
            f.seek(f.length())
        } else {
            if (!readMode) f.setLength(0)
        }
        return FileImpl(f)
    }

    @Throws(IOException::class)
    override fun openProgram(prog: String?, mode: String?): File {
        val p = Runtime.getRuntime().exec(prog)
        return if ("w" == mode) FileImpl(p.getOutputStream()) else FileImpl(p.getInputStream())
    }

    @Throws(IOException::class)
    override fun tmpFile(): File {
        val f = java.io.File.createTempFile(".luaj", "bin")
        f.deleteOnExit()
        return FileImpl(RandomAccessFile(f, "rw"))
    }

    private inner class FileImpl(
        private val file: RandomAccessFile?,
        `is`: InputStream?,
        private val os: OutputStream?
    ) : File() {
        private val `is`: InputStream?
        private var closed = false
        private var nobuffer = false

        init {
            this.`is` = if (`is` != null) if (`is`.markSupported()) `is` else BufferedInputStream(`is`) else null
        }

        constructor(f: RandomAccessFile?) : this(f, null, null)
        constructor(i: InputStream?) : this(null, i, null)
        constructor(o: OutputStream?) : this(null, null, o)

        override fun tojstring(): String {
            return "file (" + (if (this.closed) "closed" else this.hashCode().toString()) + ")"
        }

        override fun isstdfile(): Boolean {
            return file == null
        }

        @Throws(IOException::class)
        override fun close() {
            closed = true
            if (file != null) {
                file.close()
            }
        }

        @Throws(IOException::class)
        override fun flush() {
            if (os != null) os.flush()
        }

        @Throws(IOException::class)
        override fun write(s: LuaString?) {
            if (os != null) os.write(s!!.m_bytes, s.m_offset, s.m_length)
            else if (file != null) file.write(s!!.m_bytes, s.m_offset, s.m_length)
            else notimplemented()
            if (nobuffer) flush()
        }

        override fun isclosed(): Boolean {
            return closed
        }

        @Throws(IOException::class)
        override fun seek(option: String?, pos: Int): Int {
            if (file != null) {
                if ("set" == option) {
                    file.seek(pos.toLong())
                } else if ("end" == option) {
                    file.seek(file.length() + pos)
                } else {
                    file.seek(file.getFilePointer() + pos)
                }
                return file.getFilePointer().toInt()
            }
            notimplemented()
            return 0
        }

        override fun setvbuf(mode: String?, size: Int) {
            nobuffer = "no" == mode
        }

        // get length remaining to read
        @Throws(IOException::class)
        override fun remaining(): Int {
            return if (file != null) (file.length() - file.getFilePointer()).toInt() else -1
        }

        // peek ahead one character
        @Throws(IOException::class)
        override fun peek(): Int {
            if (`is` != null) {
                `is`.mark(1)
                val c = `is`.read()
                `is`.reset()
                return c
            } else if (file != null) {
                val fp = file.getFilePointer()
                val c = file.read()
                file.seek(fp)
                return c
            }
            notimplemented()
            return 0
        }

        // return char if read, -1 if eof, throw IOException on other exception
        @Throws(IOException::class)
        override fun read(): Int {
            if (`is` != null) return `is`.read()
            else if (file != null) {
                return file.read()
            }
            notimplemented()
            return 0
        }

        // return number of bytes read if positive, -1 if eof, throws IOException
        @Throws(IOException::class)
        override fun read(bytes: ByteArray?, offset: Int, length: Int): Int {
            if (file != null) {
                return file.read(bytes!!, offset, length)
            } else if (`is` != null) {
                return `is`.read(bytes!!, offset, length)
            } else {
                notimplemented()
            }
            return length
        }
    }

    private inner class StdoutFile(private val file_type: Int) : File() {
        override fun tojstring(): String {
            return "file (" + this.hashCode() + ")"
        }

        val printStream: PrintStream?
            get() = if (file_type == FTYPE_STDERR) globals!!.STDERR else globals!!.STDOUT

        @Throws(IOException::class)
        override fun write(string: LuaString?) {
            this.printStream!!.write(string!!.m_bytes, string.m_offset, string.m_length)
        }

        @Throws(IOException::class)
        override fun flush() {
            this.printStream!!.flush()
        }

        override fun isstdfile(): Boolean {
            return true
        }

        @Throws(IOException::class)
        override fun close() {
            // do not close std files.
        }

        override fun isclosed(): Boolean {
            return false
        }

        @Throws(IOException::class)
        override fun seek(option: String?, bytecount: Int): Int {
            return 0
        }

        override fun setvbuf(mode: String?, size: Int) {
        }

        @Throws(IOException::class)
        override fun remaining(): Int {
            return 0
        }

        @Throws(IOException::class, EOFException::class)
        override fun peek(): Int {
            return 0
        }

        @Throws(IOException::class, EOFException::class)
        override fun read(): Int {
            return 0
        }

        @Throws(IOException::class)
        override fun read(bytes: ByteArray?, offset: Int, length: Int): Int {
            return 0
        }
    }

    private inner class StdinFile : File() {
        override fun tojstring(): String {
            return "file (" + this.hashCode() + ")"
        }

        @Throws(IOException::class)
        override fun write(string: LuaString?) {
        }

        @Throws(IOException::class)
        override fun flush() {
        }

        override fun isstdfile(): Boolean {
            return true
        }

        @Throws(IOException::class)
        override fun close() {
            // do not close std files.
        }

        override fun isclosed(): Boolean {
            return false
        }

        @Throws(IOException::class)
        override fun seek(option: String?, bytecount: Int): Int {
            return 0
        }

        override fun setvbuf(mode: String?, size: Int) {
        }

        @Throws(IOException::class)
        override fun remaining(): Int {
            return -1
        }

        @Throws(IOException::class, EOFException::class)
        override fun peek(): Int {
            globals!!.STDIN!!.mark(1)
            val c = globals!!.STDIN!!.read()
            globals!!.STDIN!!.reset()
            return c
        }

        @Throws(IOException::class, EOFException::class)
        override fun read(): Int {
            return globals!!.STDIN!!.read()
        }

        @Throws(IOException::class)
        override fun read(bytes: ByteArray?, offset: Int, length: Int): Int {
            return globals!!.STDIN!!.read(bytes!!, offset, length)
        }
    }

    companion object {
        private fun notimplemented() {
            throw LuaError("not implemented")
        }
    }
}
