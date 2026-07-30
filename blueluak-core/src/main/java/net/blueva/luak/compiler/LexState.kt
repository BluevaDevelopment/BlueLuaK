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
package net.blueva.luak.compiler

import net.blueva.luak.LocVars
import net.blueva.luak.Lua
import net.blueva.luak.LuaError
import net.blueva.luak.LuaInteger
import net.blueva.luak.LuaString
import net.blueva.luak.LuaValue
import net.blueva.luak.Prototype
import net.blueva.luak.compiler.FuncState.BlockCnt
import net.blueva.luak.lib.MathLib
import java.io.IOException
import java.io.InputStream
import java.util.Hashtable

internal class LexState internal constructor(state: LuaC.CompileState?, stream: InputStream?) : Constants() {
    /* semantics information */
    internal class SemInfo {
        var r: LuaValue? = null
        var ts: LuaString? = null
    }

    internal class Token {
        var token: Int = 0
        val seminfo: SemInfo = net.blueva.luak.compiler.LexState.SemInfo()
        fun set(other: Token) {
            this.token = other.token
            this.seminfo.r = other.seminfo.r
            this.seminfo.ts = other.seminfo.ts
        }
    }

    var current: Int = 0 /* current character (charint) */
    var linenumber: Int = 0 /* input line counter */
    var lastline: Int = 0 /* line of last token `consumed' */
    internal val t: Token = net.blueva.luak.compiler.LexState.Token() /* current token */
    internal val lookahead: Token = net.blueva.luak.compiler.LexState.Token() /* look ahead token */
    internal var fs: FuncState? = null /* `FuncState' is private to the parser */
    internal var L: LuaC.CompileState?
    var z: InputStream? /* input stream */
    var buff: CharArray /* buffer for tokens */
    var nbuff: Int = 0 /* length of buffer */
    internal var dyd: Dyndata = net.blueva.luak.compiler.LexState.Dyndata() /* dynamic structures used by the parser */
    var source: LuaString? = null /* current source name */
    var envn: LuaString? = null /* environment variable name */
    var decpoint: Byte = 0 /* locale decimal point */

    private fun isalnum(c: Int): Boolean {
        return (c >= '0'.code && c <= '9'.code)
                || (c >= 'a'.code && c <= 'z'.code)
                || (c >= 'A'.code && c <= 'Z'.code)
                || (c == '_'.code)
        // return Character.isLetterOrDigit(c);
    }

    private fun isalpha(c: Int): Boolean {
        return (c >= 'a'.code && c <= 'z'.code)
                || (c >= 'A'.code && c <= 'Z'.code)
    }

    private fun isdigit(c: Int): Boolean {
        return (c >= '0'.code && c <= '9'.code)
    }

    private fun isxdigit(c: Int): Boolean {
        return (c >= '0'.code && c <= '9'.code)
                || (c >= 'a'.code && c <= 'f'.code)
                || (c >= 'A'.code && c <= 'F'.code)
    }

    private fun isspace(c: Int): Boolean {
        return (c >= 0 && c <= ' '.code)
    }


    fun nextChar() {
        try {
            current = z!!.read()
        } catch (e: IOException) {
            e.printStackTrace()
            current = net.blueva.luak.compiler.LexState.Companion.EOZ
        }
    }

    fun currIsNewline(): Boolean {
        return current == '\n'.code || current == '\r'.code
    }

    fun save_and_next() {
        save(current)
        nextChar()
    }

    fun save(c: Int) {
        if (buff == null || nbuff + 1 > buff.size) buff = realloc(buff, nbuff * 2 + 1)
        buff[nbuff++] = c.toChar()
    }


    fun token2str(token: Int): String? {
        if (token < net.blueva.luak.compiler.LexState.Companion.FIRST_RESERVED) {
            return if (net.blueva.luak.compiler.LexState.Companion.iscntrl(token)) L!!.pushfstring("char(" + token + ")") else L!!.pushfstring(
                String.valueOf(token.toChar())
            )
        } else {
            return net.blueva.luak.compiler.LexState.Companion.luaX_tokens!![token - net.blueva.luak.compiler.LexState.Companion.FIRST_RESERVED]
        }
    }

    fun txtToken(token: Int): String? {
        when (token) {
            net.blueva.luak.compiler.LexState.Companion.TK_NAME, net.blueva.luak.compiler.LexState.Companion.TK_STRING, net.blueva.luak.compiler.LexState.Companion.TK_NUMBER -> return String(
                buff,
                0,
                nbuff
            )

            else -> return token2str(token)
        }
    }

    fun lexerror(msg: String?, token: Int) {
        val cid: String? = Lua.chunkid(source!!.tojstring())
        L!!.pushfstring(cid.toString() + ":" + linenumber + ": " + msg)
        if (token != 0) L!!.pushfstring("syntax error: " + msg + " near " + txtToken(token))
        throw LuaError(cid.toString() + ":" + linenumber + ": " + msg)
    }

    fun syntaxerror(msg: String?) {
        lexerror(msg, t.token)
    }

    // only called by new_localvarliteral() for var names.
    fun newstring(s: String?): LuaString {
        return L!!.newTString(s)
    }

    fun newstring(chars: CharArray?, offset: Int, len: Int): LuaString {
        return L!!.newTString(String(chars, offset, len))
    }

    fun inclinenumber() {
        val old = current
        _assert(currIsNewline())
        nextChar() /* skip '\n' or '\r' */
        if (currIsNewline() && current != old) nextChar() /* skip '\n\r' or '\r\n' */
        if (++linenumber >= net.blueva.luak.compiler.LexState.Companion.MAX_INT) syntaxerror("chunk has too many lines")
    }

    internal fun setinput(L: LuaC.CompileState?, firstByte: Int, z: InputStream?, source: LuaString?) {
        this.decpoint = '.'.code.toByte()
        this.L = L
        this.lookahead.token = net.blueva.luak.compiler.LexState.Companion.TK_EOS /* no look-ahead token */
        this.z = z
        this.fs = null
        this.linenumber = 1
        this.lastline = 1
        this.source = source
        this.envn = LuaValue.ENV /* environment variable name */
        this.nbuff = 0 /* initialize buffer */
        this.current = firstByte /* read first char */
        this.skipShebang()
    }

    private fun skipShebang() {
        if (current == '#'.code) while (!currIsNewline() && current != net.blueva.luak.compiler.LexState.Companion.EOZ) nextChar()
    }


    /*
	** =======================================================
	** LEXICAL ANALYZER
	** =======================================================
	*/
    fun check_next(set: String): Boolean {
        if (set.indexOf(current) < 0) return false
        save_and_next()
        return true
    }

    fun buffreplace(from: Char, to: Char) {
        var n = nbuff
        val p = buff
        while ((--n) >= 0) if (p[n] == from) p[n] = to
    }

    internal fun strx2number(str: String, seminfo: SemInfo?): LuaValue {
        val c: CharArray = str.toCharArray()
        var s = 0
        while (s < c.size && isspace(c[s].code)) ++s
        // Check for negative sign
        var sgn = 1.0
        if (s < c.size && c[s] == '-') {
            sgn = -1.0
            ++s
        }
        /* Check for "0x" */
        if (s + 2 >= c.size) return LuaValue.ZERO
        if (c[s++] != '0') return LuaValue.ZERO
        if (c[s] != 'x' && c[s] != 'X') return LuaValue.ZERO
        ++s

        // read integer part.
        var m = 0.0
        var e = 0
        while (s < c.size && isxdigit(c[s].code)) m = (m * 16) + hexvalue(c[s++].code)
        if (s < c.size && c[s] == '.') {
            ++s // skip dot
            while (s < c.size && isxdigit(c[s].code)) {
                m = (m * 16) + hexvalue(c[s++].code)
                e -= 4 // Each fractional part shifts right by 2^4
            }
        }
        if (s < c.size && (c[s] == 'p' || c[s] == 'P')) {
            ++s
            var exp1 = 0
            var neg1 = false
            if (s < c.size && c[s] == '-') {
                neg1 = true
                ++s
            }
            while (s < c.size && isdigit(c[s].code)) exp1 = exp1 * 10 + c[s++].code - '0'.code
            if (neg1) exp1 = -exp1
            e += exp1
        }
        return LuaValue.valueOf(sgn * m * MathLib.dpow_d(2.0, e))
    }

    internal fun str2d(str: String, seminfo: SemInfo): Boolean {
        if (str.indexOf('n') >= 0 || str.indexOf('N') >= 0) seminfo.r = LuaValue.ZERO
        else if (str.indexOf('x') >= 0 || str.indexOf('X') >= 0) seminfo.r = strx2number(str, seminfo)
        else {
            try {
                seminfo.r = LuaValue.valueOf((str.trim()).toDouble())
            } catch (e: NumberFormatException) {
                lexerror(
                    "malformed number (" + e.message + ")",
                    net.blueva.luak.compiler.LexState.Companion.TK_NUMBER
                )
            }
        }
        return true
    }

    internal fun read_numeral(seminfo: SemInfo) {
        var expo = "Ee"
        val first = current
        _assert(isdigit(current))
        save_and_next()
        if (first == '0'.code && check_next("Xx")) expo = "Pp"
        while (true) {
            if (check_next(expo)) check_next("+-")
            if (isxdigit(current) || current == '.'.code) save_and_next()
            else break
        }
        val str = String(buff, 0, nbuff)
        str2d(str, seminfo)
    }

    fun skip_sep(): Int {
        var count = 0
        val s = current
        _assert(s == '['.code || s == ']'.code)
        save_and_next()
        while (current == '='.code) {
            save_and_next()
            count++
        }
        return if (current == s) count else (-count) - 1
    }

    internal fun read_long_string(seminfo: SemInfo?, sep: Int) {
        var cont = 0
        save_and_next() /* skip 2nd `[' */
        if (currIsNewline())  /* string starts with a newline? */
            inclinenumber() /* skip it */
        var endloop = false
        while (!endloop) {
            when (current) {
                net.blueva.luak.compiler.LexState.Companion.EOZ -> lexerror(
                    if (seminfo != null)
                        "unfinished long string"
                    else
                        "unfinished long comment", net.blueva.luak.compiler.LexState.Companion.TK_EOS
                )

                '[' -> {
                    if (skip_sep() == sep) {
                        save_and_next() /* skip 2nd `[' */
                        cont++
                        if (net.blueva.luak.compiler.LexState.Companion.LUA_COMPAT_LSTR == 1) {
                            if (sep == 0) lexerror("nesting of [[...]] is deprecated", '['.code)
                        }
                    }
                }

                ']' -> {
                    if (skip_sep() == sep) {
                        save_and_next() /* skip 2nd `]' */
                        if (net.blueva.luak.compiler.LexState.Companion.LUA_COMPAT_LSTR == 2) {
                            cont--
                            if (sep == 0 && cont >= 0) break
                        }
                        endloop = true
                    }
                }

                '\n', '\r' -> {
                    save('\n'.code)
                    inclinenumber()
                    if (seminfo == null) nbuff = 0 /* avoid wasting space */
                }

                else -> {
                    if (seminfo != null) save_and_next()
                    else nextChar()
                }
            }
        }
        if (seminfo != null) seminfo.ts = L!!.newTString(LuaString.valueOf(buff, 2 + sep, nbuff - 2 * (2 + sep)))
    }

    fun hexvalue(c: Int): Int {
        return if (c <= '9'.code) c - '0'.code else if (c <= 'F'.code) c + 10 - 'A'.code else c + 10 - 'a'.code
    }

    fun readhexaesc(): Int {
        nextChar()
        val c1 = current
        nextChar()
        val c2 = current
        if (!isxdigit(c1) || !isxdigit(c2)) lexerror(
            "hexadecimal digit expected 'x" + (c1.toChar()) + (c2.toChar()),
            net.blueva.luak.compiler.LexState.Companion.TK_STRING
        )
        return (hexvalue(c1) shl 4) + hexvalue(c2)
    }

    internal fun read_string(del: Int, seminfo: SemInfo) {
        save_and_next()
        while (current != del) {
            when (current) {
                net.blueva.luak.compiler.LexState.Companion.EOZ -> {
                    lexerror("unfinished string", net.blueva.luak.compiler.LexState.Companion.TK_EOS)
                    continue  /* to avoid warnings */
                }

                '\n', '\r' -> {
                    lexerror("unfinished string", net.blueva.luak.compiler.LexState.Companion.TK_STRING)
                    continue  /* to avoid warnings */
                }

                '\\' -> {
                    var c: Int
                    nextChar() /* do not save the `\' */
                    when (current) {
                        'a' -> c = '\u0007'.code
                        'b' -> c = '\b'.code
                        'f' -> c = '\u000C'.code
                        'n' -> c = '\n'.code
                        'r' -> c = '\r'.code
                        't' -> c = '\t'.code
                        'v' -> c = '\u000B'.code
                        'x' -> c = readhexaesc()
                        '\n', '\r' -> {
                            save('\n'.code)
                            inclinenumber()
                            continue
                        }

                        net.blueva.luak.compiler.LexState.Companion.EOZ -> continue  /* will raise an error next loop */
                        'z' -> {
                            /* zap following span of spaces */
                            nextChar() /* skip the 'z' */
                            while (isspace(current)) {
                                if (currIsNewline()) inclinenumber()
                                else nextChar()
                            }
                            continue
                        }

                        else -> {
                            if (!isdigit(current)) save_and_next() /* handles \\, \", \', and \? */
                            else { /* \xxx */
                                var i = 0
                                c = 0
                                do {
                                    c = 10 * c + (current - '0'.code)
                                    nextChar()
                                } while (++i < 3 && isdigit(current))
                                if (c > net.blueva.luak.compiler.LexState.Companion.UCHAR_MAX) lexerror(
                                    "escape sequence too large",
                                    net.blueva.luak.compiler.LexState.Companion.TK_STRING
                                )
                                save(c)
                            }
                            continue
                        }
                    }
                    save(c)
                    nextChar()
                    continue
                }

                else -> save_and_next()
            }
        }
        save_and_next() /* skip delimiter */
        seminfo.ts = L!!.newTString(LuaString.valueOf(buff, 1, nbuff - 2))
    }

    internal fun llex(seminfo: SemInfo): Int {
        nbuff = 0
        while (true) {
            when (current) {
                '\n', '\r' -> {
                    inclinenumber()
                    continue
                }

                ' ', '\u000C', '\t', 0x0B -> {
                    nextChar()
                    continue
                }

                '-' -> {
                    nextChar()
                    if (current != '-'.code) return '-'.code
                    /* else is a comment */
                    nextChar()
                    if (current == '['.code) {
                        val sep = skip_sep()
                        nbuff = 0 /* `skip_sep' may dirty the buffer */
                        if (sep >= 0) {
                            read_long_string(null, sep) /* long comment */
                            nbuff = 0
                            continue
                        }
                    }
                    /* else short comment */
                    while (!currIsNewline() && current != net.blueva.luak.compiler.LexState.Companion.EOZ) nextChar()
                    continue
                }

                '[' -> {
                    run {
                        val sep = skip_sep()
                        if (sep >= 0) {
                            read_long_string(seminfo, sep)
                            return net.blueva.luak.compiler.LexState.Companion.TK_STRING
                        } else if (sep == -1) return '['.code
                        else lexerror(
                            "invalid long string delimiter",
                            net.blueva.luak.compiler.LexState.Companion.TK_STRING
                        )
                    }
                    run {
                        nextChar()
                        if (current != '='.code) return '='.code
                        else {
                            nextChar()
                            return net.blueva.luak.compiler.LexState.Companion.TK_EQ
                        }
                    }
                }

                '=' -> {
                    nextChar()
                    if (current != '='.code) return '='.code
                    else {
                        nextChar()
                        return net.blueva.luak.compiler.LexState.Companion.TK_EQ
                    }
                }

                '<' -> {
                    nextChar()
                    if (current != '='.code) return '<'.code
                    else {
                        nextChar()
                        return net.blueva.luak.compiler.LexState.Companion.TK_LE
                    }
                }

                '>' -> {
                    nextChar()
                    if (current != '='.code) return '>'.code
                    else {
                        nextChar()
                        return net.blueva.luak.compiler.LexState.Companion.TK_GE
                    }
                }

                '~' -> {
                    nextChar()
                    if (current != '='.code) return '~'.code
                    else {
                        nextChar()
                        return net.blueva.luak.compiler.LexState.Companion.TK_NE
                    }
                }

                ':' -> {
                    nextChar()
                    if (current != ':'.code) return ':'.code
                    else {
                        nextChar()
                        return net.blueva.luak.compiler.LexState.Companion.TK_DBCOLON
                    }
                }

                '"', '\'' -> {
                    read_string(current, seminfo)
                    return net.blueva.luak.compiler.LexState.Companion.TK_STRING
                }

                '.' -> {
                    save_and_next()
                    if (check_next(".")) {
                        if (check_next(".")) return net.blueva.luak.compiler.LexState.Companion.TK_DOTS /* ... */
                        else return net.blueva.luak.compiler.LexState.Companion.TK_CONCAT /* .. */
                    } else if (!isdigit(current)) return '.'.code
                    else {
                        read_numeral(seminfo)
                        return net.blueva.luak.compiler.LexState.Companion.TK_NUMBER
                    }
                }

                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                    read_numeral(seminfo)
                    return net.blueva.luak.compiler.LexState.Companion.TK_NUMBER
                }

                net.blueva.luak.compiler.LexState.Companion.EOZ -> {
                    return net.blueva.luak.compiler.LexState.Companion.TK_EOS
                }

                else -> {
                    if (isalpha(current) || current == '_'.code) {
                        /* identifier or reserved word */
                        val ts: LuaString?
                        do {
                            save_and_next()
                        } while (isalnum(current))
                        ts = newstring(buff, 0, nbuff)
                        if (net.blueva.luak.compiler.LexState.Companion.RESERVED.containsKey(ts)) return (net.blueva.luak.compiler.LexState.Companion.RESERVED.get(
                            ts
                        ) as Integer).toInt()
                        else {
                            seminfo.ts = ts
                            return net.blueva.luak.compiler.LexState.Companion.TK_NAME
                        }
                    } else {
                        val c = current
                        nextChar()
                        return c /* single-char tokens (+ - / ...) */
                    }
                }
            }
        }
    }

    fun next() {
        lastline = linenumber
        if (lookahead.token != net.blueva.luak.compiler.LexState.Companion.TK_EOS) { /* is there a look-ahead token? */
            t.set(lookahead) /* use this one */
            lookahead.token = net.blueva.luak.compiler.LexState.Companion.TK_EOS /* and discharge it */
        } else t.token = llex(t.seminfo) /* read next token */
    }

    fun lookahead() {
        _assert(lookahead.token == net.blueva.luak.compiler.LexState.Companion.TK_EOS)
        lookahead.token = llex(lookahead.seminfo)
    }


    internal class expdesc {
        var k: Int = 0 // expkind, from enumerated list, above

        internal class U {
            // originally a union
            var ind_idx: Short = 0 // index (R/K)
            var ind_t: Short = 0 // table(register or upvalue)
            var ind_vt: Short = 0 // whether 't' is register (VLOCAL) or (UPVALUE)
            private var _nval: LuaValue? = null
            var info: Int = 0
            fun setNval(r: LuaValue?) {
                _nval = r
            }

            fun nval(): LuaValue? {
                return (if (_nval == null) LuaInteger.valueOf(info) else _nval)
            }
        }

        val u: U = net.blueva.luak.compiler.LexState.expdesc.U()
        val t: IntPtr = IntPtr() /* patch list of `exit when true' */
        val f: IntPtr = IntPtr() /* patch list of `exit when false' */
        fun init(k: Int, i: Int) {
            this.f.i = net.blueva.luak.compiler.LexState.Companion.NO_JUMP
            this.t.i = net.blueva.luak.compiler.LexState.Companion.NO_JUMP
            this.k = k
            this.u.info = i
        }

        fun hasjumps(): Boolean {
            return (t.i !== f.i)
        }

        fun isnumeral(): Boolean {
            return (k == net.blueva.luak.compiler.LexState.Companion.VKNUM && t.i === net.blueva.luak.compiler.LexState.Companion.NO_JUMP && f.i === net.blueva.luak.compiler.LexState.Companion.NO_JUMP)
        }

        fun setvalue(other: expdesc) {
            this.f.i = other.f.i
            this.k = other.k
            this.t.i = other.t.i
            this.u._nval = other.u._nval
            this.u.ind_idx = other.u.ind_idx
            this.u.ind_t = other.u.ind_t
            this.u.ind_vt = other.u.ind_vt
            this.u.info = other.u.info
        }
    }


    /* description of active local variable */
    internal class Vardesc(idx: Int) {
        val idx: Short /* variable index in stack */

        init {
            this.idx = idx.toShort()
        }
    }


    /* description of pending goto statements and label statements */
    internal class Labeldesc(name: LuaString?, pc: Int, line: Int, nactvar: Short) {
        var name: LuaString? /* label identifier */
        var pc: Int /* position in code */
        var line: Int /* line where it appeared */
        var nactvar: Short /* local level where it appears in current block */

        init {
            this.name = name
            this.pc = pc
            this.line = line
            this.nactvar = nactvar
        }
    }


    /* dynamic structures used by the parser */
    internal class Dyndata {
        var actvar: Array<Vardesc?>? /* list of active local variables */
        var n_actvar: Int = 0
        var gt: Array<Labeldesc> /* list of pending gotos */
        var n_gt: Int = 0
        var label: Array<Labeldesc>? /* list of active labels */
        var n_label: Int = 0
    }


    fun hasmultret(k: Int): Boolean {
        return ((k) == net.blueva.luak.compiler.LexState.Companion.VCALL || (k) == net.blueva.luak.compiler.LexState.Companion.VVARARG)
    }

    /*----------------------------------------------------------------------
	name		args	description
	------------------------------------------------------------------------*/
    fun anchor_token() {
        /* last token from outer function must be EOS */
        _assert(fs != null || t.token == net.blueva.luak.compiler.LexState.Companion.TK_EOS)
        if (t.token == net.blueva.luak.compiler.LexState.Companion.TK_NAME || t.token == net.blueva.luak.compiler.LexState.Companion.TK_STRING) {
            val ts: LuaString? = t.seminfo.ts
            // TODO: is this necessary?
            L!!.cachedLuaString(t.seminfo.ts)
        }
    }

    /* semantic error */
    fun semerror(msg: String?) {
        t.token = 0 /* remove 'near to' from final message */
        syntaxerror(msg)
    }

    fun error_expected(token: Int) {
        syntaxerror(
            L!!.pushfstring(
                net.blueva.luak.compiler.LexState.Companion.LUA_QS(token2str(token)).toString() + " expected"
            )
        )
    }

    fun testnext(c: Int): Boolean {
        if (t.token == c) {
            next()
            return true
        } else return false
    }

    fun check(c: Int) {
        if (t.token != c) error_expected(c)
    }

    fun checknext(c: Int) {
        check(c)
        next()
    }

    fun check_condition(c: Boolean, msg: String?) {
        if (!(c)) syntaxerror(msg)
    }


    fun check_match(what: Int, who: Int, where: Int) {
        if (!testnext(what)) {
            if (where == linenumber) error_expected(what)
            else {
                syntaxerror(
                    L!!.pushfstring(
                        (net.blueva.luak.compiler.LexState.Companion.LUA_QS(token2str(what))
                            .toString() + " expected " + "(to close " + net.blueva.luak.compiler.LexState.Companion.LUA_QS(
                            token2str(who)
                        )
                                + " at line " + where + ")")
                    )
                )
            }
        }
    }

    fun str_checkname(): LuaString? {
        val ts: LuaString?
        check(net.blueva.luak.compiler.LexState.Companion.TK_NAME)
        ts = t.seminfo.ts
        next()
        return ts
    }

    internal fun codestring(e: expdesc, s: LuaString?) {
        e.init(net.blueva.luak.compiler.LexState.Companion.VK, fs!!.stringK(s))
    }

    internal fun checkname(e: expdesc) {
        codestring(e, str_checkname())
    }


    fun registerlocalvar(varname: LuaString?): Int {
        val fs: FuncState = this.fs
        val f: Prototype = fs.f
        if (f.locvars == null || fs.nlocvars + 1 > f.locvars.size) f.locvars = realloc(f.locvars, fs.nlocvars * 2 + 1)
        f.locvars[fs.nlocvars] = LocVars(varname, 0, 0)
        return fs.nlocvars++
    }

    fun new_localvar(name: LuaString?) {
        val reg = registerlocalvar(name)
        fs!!.checklimit(dyd.n_actvar + 1, FuncState.LUAI_MAXVARS, "local variables")
        if (dyd.actvar == null || dyd.n_actvar + 1 > dyd.actvar!!.size) dyd.actvar =
            realloc(dyd.actvar, Math.max(1, dyd.n_actvar * 2))
        dyd.actvar!![dyd.n_actvar++] = net.blueva.luak.compiler.LexState.Vardesc(reg)
    }

    fun new_localvarliteral(v: String?) {
        val ts: LuaString = newstring(v)
        new_localvar(ts)
    }

    fun adjustlocalvars(nvars: Int) {
        var nvars = nvars
        val fs: FuncState = this.fs
        fs.nactvar = (fs.nactvar + nvars) as Short
        while (nvars > 0) {
            fs.getlocvar(fs.nactvar - nvars).startpc = fs.pc
            nvars--
        }
    }

    fun removevars(tolevel: Int) {
        val fs: FuncState = this.fs
        while (fs.nactvar > tolevel) fs.getlocvar(--fs.nactvar).endpc = fs.pc
    }

    internal fun singlevar(`var`: expdesc) {
        val varname: LuaString? = this.str_checkname()
        val fs: FuncState = this.fs
        if (FuncState.singlevaraux(
                fs,
                varname,
                `var`,
                1
            ) === net.blueva.luak.compiler.LexState.Companion.VVOID
        ) { /* global name? */
            val key: expdesc = net.blueva.luak.compiler.LexState.expdesc()
            FuncState.singlevaraux(fs, this.envn, `var`, 1) /* get environment variable */
            _assert(`var`.k == net.blueva.luak.compiler.LexState.Companion.VLOCAL || `var`.k == net.blueva.luak.compiler.LexState.Companion.VUPVAL)
            this.codestring(key, varname) /* key is variable name */
            fs.indexed(`var`, key) /* env[varname] */
        }
    }

    internal fun adjust_assign(nvars: Int, nexps: Int, e: expdesc) {
        val fs: FuncState = this.fs
        var extra = nvars - nexps
        if (hasmultret(e.k)) {
            /* includes call itself */
            extra++
            if (extra < 0) extra = 0
            /* last exp. provides the difference */
            fs.setreturns(e, extra)
            if (extra > 1) fs.reserveregs(extra - 1)
        } else {
            /* close last expression */
            if (e.k != net.blueva.luak.compiler.LexState.Companion.VVOID) fs.exp2nextreg(e)
            if (extra > 0) {
                val reg: Int = fs.freereg
                fs.reserveregs(extra)
                fs.nil(reg, extra)
            }
        }
    }

    fun enterlevel() {
        if (++L!!.nCcalls > net.blueva.luak.compiler.LexState.Companion.LUAI_MAXCCALLS) lexerror(
            "chunk has too many syntax levels",
            0
        )
    }

    fun leavelevel() {
        L!!.nCcalls--
    }

    internal fun closegoto(g: Int, label: Labeldesc) {
        val fs: FuncState = this.fs
        val gl = this.dyd.gt
        val gt = gl[g]
        _assert(gt.name!!.eq_b(label.name))
        if (gt.nactvar < label.nactvar) {
            val vname: LuaString = fs.getlocvar(gt.nactvar).varname
            val msg: String? = L!!.pushfstring(
                ("<goto " + gt.name + "> at line "
                        + gt.line + " jumps into the scope of local '"
                        + vname.tojstring() + "'")
            )
            semerror(msg)
        }
        fs.patchlist(gt.pc, label.pc)
        /* remove goto from pending list */
        System.arraycopy(gl, g + 1, gl, g, this.dyd.n_gt - g - 1)
        gl[--this.dyd.n_gt] = null
    }

    /*
	 ** try to close a goto with existing labels; this solves backward jumps
	 */
    fun findlabel(g: Int): Boolean {
        var i: Int
        val bl: BlockCnt = fs!!.bl
        val dyd = this.dyd
        val gt = dyd.gt[g]
        /* check labels in current block for a match */
        i = bl.firstlabel
        while (i < dyd.n_label) {
            val lb = dyd.label!![i]
            if (lb.name!!.eq_b(gt.name)) {  /* correct label? */
                if (gt.nactvar > lb.nactvar &&
                    (bl.upval || dyd.n_label > bl.firstlabel)
                ) fs!!.patchclose(gt.pc, lb.nactvar)
                closegoto(g, lb) /* close it */
                return true
            }
            i++
        }
        return false /* label not found; cannot close goto */
    }

    /* Caller must grow() the vector before calling this. */
    internal fun newlabelentry(l: Array<Labeldesc?>, index: Int, name: LuaString?, line: Int, pc: Int): Int {
        l[index] = net.blueva.luak.compiler.LexState.Labeldesc(name, pc, line, fs!!.nactvar)
        return index
    }

    /*
	 ** check whether new label 'lb' matches any pending gotos in current
	 ** block; solves forward jumps
	 */
    internal fun findgotos(lb: Labeldesc) {
        val gl = dyd.gt
        var i: Int = fs!!.bl!!.firstgoto
        while (i < dyd.n_gt) {
            if (gl[i].name!!.eq_b(lb.name)) closegoto(i, lb)
            else i++
        }
    }


    /*
	** create a label named "break" to resolve break statements
	*/
    fun breaklabel() {
        val n: LuaString? = LuaString.valueOf("break")
        val l = newlabelentry(grow(dyd.label, dyd.n_label + 1).also { dyd.label = it }, dyd.n_label++, n, 0, fs!!.pc)
        findgotos(dyd.label!![l])
    }

    /*
	** generates an error for an undefined 'goto'; choose appropriate
	** message when label name is a reserved word (which can only be 'break')
	*/
    internal fun undefgoto(gt: Labeldesc) {
        val msg: String? = L!!.pushfstring(
            if (net.blueva.luak.compiler.LexState.Companion.isReservedKeyword(gt.name!!.tojstring()))
                "<" + gt.name + "> at line " + gt.line + " not inside a loop"
            else
                "no visible label '" + gt.name + "' for <goto> at line " + gt.line
        )
        semerror(msg)
    }

    fun addprototype(): Prototype? {
        val clp: Prototype?
        val f: Prototype = fs!!.f /* prototype of current function */
        if (f.p == null || fs!!.np >= f.p.size) {
            f.p = realloc(f.p, Math.max(1, fs!!.np * 2))
        }
        clp = Prototype()
        f.p!![fs!!.np++] = clp
        return clp
    }

    internal fun codeclosure(v: expdesc) {
        val fs: FuncState = this.fs!!.prev
        v.init(net.blueva.luak.compiler.LexState.Companion.VRELOCABLE, fs.codeABx(OP_CLOSURE, 0, fs.np - 1))
        fs.exp2nextreg(v) /* fix it at stack top (for GC) */
    }

    internal fun open_func(fs: FuncState, bl: BlockCnt?) {
        fs.prev = this.fs /* linked list of funcstates */
        fs.ls = this
        this.fs = fs
        fs.pc = 0
        fs.lasttarget = -1
        fs.jpc = IntPtr(net.blueva.luak.compiler.LexState.Companion.NO_JUMP)
        fs.freereg = 0
        fs.nk = 0
        fs.np = 0
        fs.nups = 0
        fs.nlocvars = 0
        fs.nactvar = 0
        fs.firstlocal = dyd.n_actvar
        fs.bl = null
        fs.f!!.source = this.source
        fs.f!!.maxstacksize = 2 /* registers 0/1 are always valid */
        fs.enterblock(bl, false)
    }

    fun close_func() {
        val fs: FuncState = this.fs
        val f: Prototype = fs.f
        fs.ret(0, 0) /* final return */
        fs.leaveblock()
        f.code = realloc(f.code, fs.pc)
        f.lineinfo = realloc(f.lineinfo, fs.pc)
        f.k = realloc(f.k, fs.nk)
        f.p = realloc(f.p, fs.np)
        f.locvars = realloc(f.locvars, fs.nlocvars)
        f.upvalues = realloc(f.upvalues, fs.nups)
        _assert(fs.bl == null)
        this.fs = fs.prev
        // last token read was anchored in defunct function; must reanchor it
        // ls.anchor_token();
    }

    /*============================================================*/ /* GRAMMAR RULES */ /*============================================================*/
    internal fun fieldsel(v: expdesc?) {
        /* fieldsel -> ['.' | ':'] NAME */
        val fs: FuncState = this.fs
        val key: expdesc = net.blueva.luak.compiler.LexState.expdesc()
        fs.exp2anyregup(v)
        this.next() /* skip the dot or colon */
        this.checkname(key)
        fs.indexed(v, key)
    }

    internal fun yindex(v: expdesc) {
        /* index -> '[' expr ']' */
        this.next() /* skip the '[' */
        this.expr(v)
        this.fs!!.exp2val(v)
        this.checknext(']'.code)
    }


    /*
	** {======================================================================
	** Rules for Constructors
	** =======================================================================
	*/
    internal class ConsControl {
        var v: expdesc = net.blueva.luak.compiler.LexState.expdesc() /* last list item read */
        var t: expdesc? = null /* table descriptor */
        var nh: Int = 0 /* total number of `record' elements */
        var na: Int = 0 /* total number of array elements */
        var tostore: Int = 0 /* number of array elements pending to be stored */
    }


    internal fun recfield(cc: ConsControl) {
        /* recfield -> (NAME | `['exp1`]') = exp1 */
        val fs: FuncState? = this.fs
        val reg: Int = this.fs!!.freereg
        val key: expdesc = net.blueva.luak.compiler.LexState.expdesc()
        val `val`: expdesc = net.blueva.luak.compiler.LexState.expdesc()
        val rkkey: Int
        if (this.t.token == net.blueva.luak.compiler.LexState.Companion.TK_NAME) {
            fs!!.checklimit(cc.nh, net.blueva.luak.compiler.LexState.Companion.MAX_INT, "items in a constructor")
            this.checkname(key)
        } else  /* this.t.token == '[' */
            this.yindex(key)
        cc.nh++
        this.checknext('='.code)
        rkkey = fs!!.exp2RK(key)
        this.expr(`val`)
        fs!!.codeABC(Lua.OP_SETTABLE, cc.t!!.u.info, rkkey, fs!!.exp2RK(`val`))
        fs!!.freereg = reg.toShort() /* free registers */
    }

    internal fun listfield(cc: ConsControl) {
        this.expr(cc.v)
        fs!!.checklimit(cc.na, net.blueva.luak.compiler.LexState.Companion.MAX_INT, "items in a constructor")
        cc.na++
        cc.tostore++
    }


    internal fun constructor(t: expdesc) {
        /* constructor -> ?? */
        val fs: FuncState = this.fs
        val line = this.linenumber
        val pc: Int = fs.codeABC(Lua.OP_NEWTABLE, 0, 0, 0)
        val cc: ConsControl = net.blueva.luak.compiler.LexState.ConsControl()
        cc.tostore = 0
        cc.nh = cc.tostore
        cc.na = cc.nh
        cc.t = t
        t.init(net.blueva.luak.compiler.LexState.Companion.VRELOCABLE, pc)
        cc.v.init(net.blueva.luak.compiler.LexState.Companion.VVOID, 0) /* no value (yet) */
        fs.exp2nextreg(t) /* fix it at stack top (for gc) */
        this.checknext('{'.code)
        do {
            _assert(cc.v.k == net.blueva.luak.compiler.LexState.Companion.VVOID || cc.tostore > 0)
            if (this.t.token == '}'.code) break
            fs.closelistfield(cc)
            when (this.t.token) {
                net.blueva.luak.compiler.LexState.Companion.TK_NAME -> {
                    /* may be listfields or recfields */
                    this.lookahead()
                    if (this.lookahead.token != '='.code)  /* expression? */
                        this.listfield(cc)
                    else this.recfield(cc)
                }

                '[' -> {
                    /* constructor_item -> recfield */
                    this.recfield(cc)
                }

                else -> {
                    /* constructor_part -> listfield */
                    this.listfield(cc)
                }
            }
        } while (this.testnext(','.code) || this.testnext(';'.code))
        this.check_match('}'.code, '{'.code, line)
        fs.lastlistfield(cc)
        val i: InstructionPtr = InstructionPtr(fs.f!!.code, pc)
        SETARG_B(i, net.blueva.luak.compiler.LexState.Companion.luaO_int2fb(cc.na)) /* set initial array size */
        SETARG_C(i, net.blueva.luak.compiler.LexState.Companion.luaO_int2fb(cc.nh)) /* set initial table size */
    }

    /* }====================================================================== */
    fun parlist() {
        /* parlist -> [ param { `,' param } ] */
        val fs: FuncState = this.fs
        val f: Prototype = fs.f
        var nparams = 0
        f.is_vararg = 0
        if (this.t.token != ')'.code) {  /* is `parlist' not empty? */
            do {
                when (this.t.token) {
                    net.blueva.luak.compiler.LexState.Companion.TK_NAME -> {
                        /* param . NAME */
                        this.new_localvar(this.str_checkname())
                        ++nparams
                    }

                    net.blueva.luak.compiler.LexState.Companion.TK_DOTS -> {
                        /* param . `...' */
                        this.next()
                        f.is_vararg = 1
                    }

                    else -> this.syntaxerror("<name> or " + net.blueva.luak.compiler.LexState.Companion.LUA_QL("...") + " expected")
                }
            } while ((f.is_vararg === 0) && this.testnext(','.code))
        }
        this.adjustlocalvars(nparams)
        f.numparams = fs.nactvar
        fs.reserveregs(fs.nactvar) /* reserve register for parameters */
    }


    internal fun body(e: expdesc, needself: Boolean, line: Int) {
        /* body -> `(' parlist `)' chunk END */
        val new_fs: FuncState = FuncState()
        val bl: BlockCnt = BlockCnt()
        new_fs.f = addprototype()
        new_fs.f!!.linedefined = line
        open_func(new_fs, bl)
        this.checknext('('.code)
        if (needself) {
            new_localvarliteral("self")
            adjustlocalvars(1)
        }
        this.parlist()
        this.checknext(')'.code)
        this.statlist()
        new_fs.f!!.lastlinedefined = this.linenumber
        this.check_match(
            net.blueva.luak.compiler.LexState.Companion.TK_END,
            net.blueva.luak.compiler.LexState.Companion.TK_FUNCTION,
            line
        )
        this.codeclosure(e)
        this.close_func()
    }

    internal fun explist(v: expdesc): Int {
        /* explist1 -> expr { `,' expr } */
        var n = 1 /* at least one expression */
        this.expr(v)
        while (this.testnext(','.code)) {
            fs!!.exp2nextreg(v)
            this.expr(v)
            n++
        }
        return n
    }


    internal fun funcargs(f: expdesc, line: Int) {
        val fs: FuncState = this.fs
        val args: expdesc = net.blueva.luak.compiler.LexState.expdesc()
        val base: Int
        val nparams: Int
        when (this.t.token) {
            '(' -> {
                /* funcargs -> `(' [ explist1 ] `)' */
                this.next()
                if (this.t.token == ')'.code)  /* arg list is empty? */
                    args.k = net.blueva.luak.compiler.LexState.Companion.VVOID
                else {
                    this.explist(args)
                    fs.setmultret(args)
                }
                this.check_match(')'.code, '('.code, line)
            }

            '{' -> {
                /* funcargs -> constructor */
                this.constructor(args)
            }

            net.blueva.luak.compiler.LexState.Companion.TK_STRING -> {
                /* funcargs -> STRING */
                this.codestring(args, this.t.seminfo.ts)
                this.next() /* must use `seminfo' before `next' */
            }

            else -> {
                this.syntaxerror("function arguments expected")
                return
            }
        }
        _assert(f.k == net.blueva.luak.compiler.LexState.Companion.VNONRELOC)
        base = f.u.info /* base register for call */
        if (hasmultret(args.k)) nparams = Lua.LUA_MULTRET /* open call */
        else {
            if (args.k != net.blueva.luak.compiler.LexState.Companion.VVOID) fs.exp2nextreg(args) /* close last argument */
            nparams = fs.freereg - (base + 1)
        }
        f.init(net.blueva.luak.compiler.LexState.Companion.VCALL, fs.codeABC(Lua.OP_CALL, base, nparams + 1, 2))
        fs.fixline(line)
        fs.freereg = (base + 1).toShort() /* call remove function and arguments and leaves
							 * (unless changed) one result */
    }


    /*
	** {======================================================================
	** Expression parsing
	** =======================================================================
	*/
    internal fun primaryexp(v: expdesc) {
        /* primaryexp -> NAME | '(' expr ')' */
        when (t.token) {
            '(' -> {
                val line = linenumber
                this.next()
                this.expr(v)
                this.check_match(')'.code, '('.code, line)
                fs!!.dischargevars(v)
                return
            }

            net.blueva.luak.compiler.LexState.Companion.TK_NAME -> {
                singlevar(v)
                return
            }

            else -> {
                this.syntaxerror("unexpected symbol " + t.token + " (" + (t.token.toChar()) + ")")
                return
            }
        }
    }


    internal fun suffixedexp(v: expdesc) {
        /* suffixedexp ->
       	primaryexp { '.' NAME | '[' exp ']' | ':' NAME funcargs | funcargs } */
        val line = linenumber
        primaryexp(v)
        while (true) {
            when (t.token) {
                '.' -> {
                    /* fieldsel */
                    this.fieldsel(v)
                }

                '[' -> {
                    /* `[' exp1 `]' */
                    val key: expdesc = net.blueva.luak.compiler.LexState.expdesc()
                    fs!!.exp2anyregup(v)
                    this.yindex(key)
                    fs!!.indexed(v, key)
                }

                ':' -> {
                    /* `:' NAME funcargs */
                    val key: expdesc = net.blueva.luak.compiler.LexState.expdesc()
                    this.next()
                    this.checkname(key)
                    fs!!.self(v, key)
                    this.funcargs(v, line)
                }

                '(', net.blueva.luak.compiler.LexState.Companion.TK_STRING, '{' -> {
                    /* funcargs */
                    fs!!.exp2nextreg(v)
                    this.funcargs(v, line)
                }

                else -> return
            }
        }
    }


    internal fun simpleexp(v: expdesc) {
        /*
		 * simpleexp -> NUMBER | STRING | NIL | true | false | ... | constructor |
		 * FUNCTION body | primaryexp
		 */
        when (this.t.token) {
            net.blueva.luak.compiler.LexState.Companion.TK_NUMBER -> {
                v.init(net.blueva.luak.compiler.LexState.Companion.VKNUM, 0)
                v.u.setNval(this.t.seminfo.r)
            }

            net.blueva.luak.compiler.LexState.Companion.TK_STRING -> {
                this.codestring(v, this.t.seminfo.ts)
            }

            net.blueva.luak.compiler.LexState.Companion.TK_NIL -> {
                v.init(net.blueva.luak.compiler.LexState.Companion.VNIL, 0)
            }

            net.blueva.luak.compiler.LexState.Companion.TK_TRUE -> {
                v.init(net.blueva.luak.compiler.LexState.Companion.VTRUE, 0)
            }

            net.blueva.luak.compiler.LexState.Companion.TK_FALSE -> {
                v.init(net.blueva.luak.compiler.LexState.Companion.VFALSE, 0)
            }

            net.blueva.luak.compiler.LexState.Companion.TK_DOTS -> {
                /* vararg */
                val fs: FuncState = this.fs
                this.check_condition(
                    fs.f!!.is_vararg !== 0, ("cannot use " + net.blueva.luak.compiler.LexState.Companion.LUA_QL("...")
                            + " outside a vararg function")
                )
                v.init(net.blueva.luak.compiler.LexState.Companion.VVARARG, fs.codeABC(Lua.OP_VARARG, 0, 1, 0))
            }

            '{' -> {
                /* constructor */
                this.constructor(v)
                return
            }

            net.blueva.luak.compiler.LexState.Companion.TK_FUNCTION -> {
                this.next()
                this.body(v, false, this.linenumber)
                return
            }

            else -> {
                this.suffixedexp(v)
                return
            }
        }
        this.next()
    }


    fun getunopr(op: Int): Int {
        when (op) {
            net.blueva.luak.compiler.LexState.Companion.TK_NOT -> return net.blueva.luak.compiler.LexState.Companion.OPR_NOT
            '-' -> return net.blueva.luak.compiler.LexState.Companion.OPR_MINUS
            '#' -> return net.blueva.luak.compiler.LexState.Companion.OPR_LEN
            else -> return net.blueva.luak.compiler.LexState.Companion.OPR_NOUNOPR
        }
    }


    fun getbinopr(op: Int): Int {
        when (op) {
            '+' -> return net.blueva.luak.compiler.LexState.Companion.OPR_ADD
            '-' -> return net.blueva.luak.compiler.LexState.Companion.OPR_SUB
            '*' -> return net.blueva.luak.compiler.LexState.Companion.OPR_MUL
            '/' -> return net.blueva.luak.compiler.LexState.Companion.OPR_DIV
            '%' -> return net.blueva.luak.compiler.LexState.Companion.OPR_MOD
            '^' -> return net.blueva.luak.compiler.LexState.Companion.OPR_POW
            net.blueva.luak.compiler.LexState.Companion.TK_CONCAT -> return net.blueva.luak.compiler.LexState.Companion.OPR_CONCAT
            net.blueva.luak.compiler.LexState.Companion.TK_NE -> return net.blueva.luak.compiler.LexState.Companion.OPR_NE
            net.blueva.luak.compiler.LexState.Companion.TK_EQ -> return net.blueva.luak.compiler.LexState.Companion.OPR_EQ
            '<' -> return net.blueva.luak.compiler.LexState.Companion.OPR_LT
            net.blueva.luak.compiler.LexState.Companion.TK_LE -> return net.blueva.luak.compiler.LexState.Companion.OPR_LE
            '>' -> return net.blueva.luak.compiler.LexState.Companion.OPR_GT
            net.blueva.luak.compiler.LexState.Companion.TK_GE -> return net.blueva.luak.compiler.LexState.Companion.OPR_GE
            net.blueva.luak.compiler.LexState.Companion.TK_AND -> return net.blueva.luak.compiler.LexState.Companion.OPR_AND
            net.blueva.luak.compiler.LexState.Companion.TK_OR -> return net.blueva.luak.compiler.LexState.Companion.OPR_OR
            else -> return net.blueva.luak.compiler.LexState.Companion.OPR_NOBINOPR
        }
    }

    internal class Priority(i: Int, j: Int) {
        val left: Byte /* left priority for each binary operator */

        val right: Byte /* right priority */

        init {
            left = i.toByte()
            right = j.toByte()
        }
    }

    init {
        this.z = stream
        this.buff = CharArray(32)
        this.L = state
    }

    /*
	** subexpr -> (simpleexp | unop subexpr) { binop subexpr }
	** where `binop' is any binary operator with a priority higher than `limit'
	*/
    internal fun subexpr(v: expdesc, limit: Int): Int {
        var op: Int
        val uop: Int
        this.enterlevel()
        uop = getunopr(this.t.token)
        if (uop != net.blueva.luak.compiler.LexState.Companion.OPR_NOUNOPR) {
            val line = linenumber
            this.next()
            this.subexpr(v, net.blueva.luak.compiler.LexState.Companion.UNARY_PRIORITY)
            fs!!.prefix(uop, v, line)
        } else this.simpleexp(v)
        /* expand while operators have priorities higher than `limit' */
        op = getbinopr(this.t.token)
        while (op != net.blueva.luak.compiler.LexState.Companion.OPR_NOBINOPR && net.blueva.luak.compiler.LexState.Companion.priority[op]!!.left > limit) {
            val v2: expdesc = net.blueva.luak.compiler.LexState.expdesc()
            val line = linenumber
            this.next()
            fs!!.infix(op, v)
            /* read sub-expression with higher priority */
            val nextop = this.subexpr(v2, net.blueva.luak.compiler.LexState.Companion.priority[op]!!.right.toInt())
            fs!!.posfix(op, v, v2, line)
            op = nextop
        }
        this.leavelevel()
        return op /* return first untreated operator */
    }

    internal fun expr(v: expdesc) {
        this.subexpr(v, 0)
    }


    /* }==================================================================== */ /*
	** {======================================================================
	** Rules for Statements
	** =======================================================================
	*/
    fun block_follow(withuntil: Boolean): Boolean {
        when (t.token) {
            net.blueva.luak.compiler.LexState.Companion.TK_ELSE, net.blueva.luak.compiler.LexState.Companion.TK_ELSEIF, net.blueva.luak.compiler.LexState.Companion.TK_END, net.blueva.luak.compiler.LexState.Companion.TK_EOS -> return true
            net.blueva.luak.compiler.LexState.Companion.TK_UNTIL -> return withuntil
            else -> return false
        }
    }


    fun block() {
        /* block -> chunk */
        val fs: FuncState = this.fs
        val bl: BlockCnt = BlockCnt()
        fs.enterblock(bl, false)
        this.statlist()
        fs.leaveblock()
    }


    /*
	** structure to chain all variables in the left-hand side of an
	** assignment
	*/
    internal class LHS_assign {
        var prev: LHS_assign? = null

        /* variable (global, local, upvalue, or indexed) */
        var v: expdesc = net.blueva.luak.compiler.LexState.expdesc()
    }


    /*
	** check whether, in an assignment to a local variable, the local variable
	** is needed in a previous assignment (to a table). If so, save original
	** local value in a safe place and use this safe copy in the previous
	** assignment.
	*/
    internal fun check_conflict(lh: LHS_assign?, v: expdesc) {
        var lh = lh
        val fs: FuncState = this.fs
        val extra = fs.freereg as Short /* eventual position to save local variable */
        var conflict = false
        while (lh != null) {
            if (lh.v.k == net.blueva.luak.compiler.LexState.Companion.VINDEXED) {
                /* table is the upvalue/local being assigned now? */
                if (lh.v.u.ind_vt.toInt() == v.k && lh.v.u.ind_t.toInt() == v.u.info) {
                    conflict = true
                    lh.v.u.ind_vt = net.blueva.luak.compiler.LexState.Companion.VLOCAL.toShort()
                    lh.v.u.ind_t = extra /* previous assignment will use safe copy */
                }
                /* index is the local being assigned? (index cannot be upvalue) */
                if (v.k == net.blueva.luak.compiler.LexState.Companion.VLOCAL && lh.v.u.ind_idx.toInt() == v.u.info) {
                    conflict = true
                    lh.v.u.ind_idx = extra /* previous assignment will use safe copy */
                }
            }
            lh = lh.prev
        }
        if (conflict) {
            /* copy upvalue/local value to a temporary (in position 'extra') */
            val op: Int =
                if (v.k == net.blueva.luak.compiler.LexState.Companion.VLOCAL) Lua.OP_MOVE else Lua.OP_GETUPVAL
            fs.codeABC(op, extra, v.u.info, 0)
            fs.reserveregs(1)
        }
    }


    internal fun assignment(lh: LHS_assign, nvars: Int) {
        val e: expdesc = net.blueva.luak.compiler.LexState.expdesc()
        this.check_condition(
            net.blueva.luak.compiler.LexState.Companion.VLOCAL <= lh.v.k && lh.v.k <= net.blueva.luak.compiler.LexState.Companion.VINDEXED,
            "syntax error"
        )
        if (this.testnext(','.code)) {  /* assignment -> `,' primaryexp assignment */
            val nv: LHS_assign = net.blueva.luak.compiler.LexState.LHS_assign()
            nv.prev = lh
            this.suffixedexp(nv.v)
            if (nv.v.k != net.blueva.luak.compiler.LexState.Companion.VINDEXED) this.check_conflict(lh, nv.v)
            this.assignment(nv, nvars + 1)
        } else {  /* assignment . `=' explist1 */
            val nexps: Int
            this.checknext('='.code)
            nexps = this.explist(e)
            if (nexps != nvars) {
                this.adjust_assign(nvars, nexps, e)
                if (nexps > nvars) this.fs!!.freereg -= nexps - nvars /* remove extra values */
            } else {
                fs!!.setoneret(e) /* close last expression */
                fs!!.storevar(lh.v, e)
                return  /* avoid default */
            }
        }
        e.init(net.blueva.luak.compiler.LexState.Companion.VNONRELOC, this.fs!!.freereg - 1) /* default assignment */
        fs!!.storevar(lh.v, e)
    }


    fun cond(): Int {
        /* cond -> exp */
        val v: expdesc = net.blueva.luak.compiler.LexState.expdesc()
        /* read condition */
        this.expr(v)
        /* `falses' are all equal here */
        if (v.k == net.blueva.luak.compiler.LexState.Companion.VNIL) v.k =
            net.blueva.luak.compiler.LexState.Companion.VFALSE
        fs!!.goiftrue(v)
        return v.f.i
    }

    fun gotostat(pc: Int) {
        val line = linenumber
        val label: LuaString?
        val g: Int
        if (testnext(net.blueva.luak.compiler.LexState.Companion.TK_GOTO)) label = str_checkname()
        else {
            next() /* skip break */
            label = LuaString.valueOf("break")
        }
        g = newlabelentry(grow(dyd.gt, dyd.n_gt + 1).also { dyd.gt = it }, dyd.n_gt++, label, line, pc)
        findlabel(g) /* close it if label already defined */
    }


    /* skip no-op statements */
    fun skipnoopstat() {
        while (t.token == ';'.code || t.token == net.blueva.luak.compiler.LexState.Companion.TK_DBCOLON) statement()
    }


    fun labelstat(label: LuaString?, line: Int) {
        /* label -> '::' NAME '::' */
        val l: Int /* index of new label being created */
        fs!!.checkrepeated(dyd.label, dyd.n_label, label) /* check for repeated labels */
        checknext(net.blueva.luak.compiler.LexState.Companion.TK_DBCOLON) /* skip double colon */
        /* create new entry for this label */
        l = newlabelentry(
            grow(dyd.label, dyd.n_label + 1).also { dyd.label = it },
            dyd.n_label++,
            label,
            line,
            fs!!.getlabel()
        )
        skipnoopstat() /* skip other no-op statements */
        if (block_follow(false)) {  /* label is last no-op statement in the block? */
            /* assume that locals are already out of scope */
            dyd.label!![l].nactvar = fs!!.bl!!.nactvar
        }
        findgotos(dyd.label!![l])
    }


    fun whilestat(line: Int) {
        /* whilestat -> WHILE cond DO block END */
        val fs: FuncState = this.fs
        val whileinit: Int
        val condexit: Int
        val bl: BlockCnt = BlockCnt()
        this.next() /* skip WHILE */
        whileinit = fs.getlabel()
        condexit = this.cond()
        fs.enterblock(bl, true)
        this.checknext(net.blueva.luak.compiler.LexState.Companion.TK_DO)
        this.block()
        fs.patchlist(fs.jump(), whileinit)
        this.check_match(
            net.blueva.luak.compiler.LexState.Companion.TK_END,
            net.blueva.luak.compiler.LexState.Companion.TK_WHILE,
            line
        )
        fs.leaveblock()
        fs.patchtohere(condexit) /* false conditions finish the loop */
    }

    fun repeatstat(line: Int) {
        /* repeatstat -> REPEAT block UNTIL cond */
        val condexit: Int
        val fs: FuncState = this.fs
        val repeat_init: Int = fs.getlabel()
        val bl1: BlockCnt = BlockCnt()
        val bl2: BlockCnt = BlockCnt()
        fs.enterblock(bl1, true) /* loop block */
        fs.enterblock(bl2, false) /* scope block */
        this.next() /* skip REPEAT */
        this.statlist()
        this.check_match(
            net.blueva.luak.compiler.LexState.Companion.TK_UNTIL,
            net.blueva.luak.compiler.LexState.Companion.TK_REPEAT,
            line
        )
        condexit = this.cond() /* read condition (inside scope block) */
        if (bl2.upval) { /* upvalues? */
            fs.patchclose(condexit, bl2.nactvar)
        }
        fs.leaveblock() /* finish scope */
        fs.patchlist(condexit, repeat_init) /* close the loop */
        fs.leaveblock() /* finish loop */
    }


    fun exp1(): Int {
        val e: expdesc = net.blueva.luak.compiler.LexState.expdesc()
        val k: Int
        this.expr(e)
        k = e.k
        fs!!.exp2nextreg(e)
        return k
    }


    fun forbody(base: Int, line: Int, nvars: Int, isnum: Boolean) {
        /* forbody -> DO block */
        val bl: BlockCnt = BlockCnt()
        val fs: FuncState = this.fs
        val prep: Int
        val endfor: Int
        this.adjustlocalvars(3) /* control variables */
        this.checknext(net.blueva.luak.compiler.LexState.Companion.TK_DO)
        prep = if (isnum) fs.codeAsBx(
            Lua.OP_FORPREP,
            base,
            net.blueva.luak.compiler.LexState.Companion.NO_JUMP
        ) else fs.jump()
        fs.enterblock(bl, false) /* scope for declared variables */
        this.adjustlocalvars(nvars)
        fs.reserveregs(nvars)
        this.block()
        fs.leaveblock() /* end of scope for declared variables */
        fs.patchtohere(prep)
        if (isnum)  /* numeric for? */
            endfor = fs.codeAsBx(Lua.OP_FORLOOP, base, net.blueva.luak.compiler.LexState.Companion.NO_JUMP)
        else {  /* generic for */
            fs.codeABC(Lua.OP_TFORCALL, base, 0, nvars)
            fs.fixline(line)
            endfor = fs.codeAsBx(Lua.OP_TFORLOOP, base + 2, net.blueva.luak.compiler.LexState.Companion.NO_JUMP)
        }
        fs.patchlist(endfor, prep + 1)
        fs.fixline(line)
    }


    fun fornum(varname: LuaString?, line: Int) {
        /* fornum -> NAME = exp1,exp1[,exp1] forbody */
        val fs: FuncState = this.fs
        val base: Int = fs.freereg
        this.new_localvarliteral(net.blueva.luak.compiler.LexState.Companion.RESERVED_LOCAL_VAR_FOR_INDEX)
        this.new_localvarliteral(net.blueva.luak.compiler.LexState.Companion.RESERVED_LOCAL_VAR_FOR_LIMIT)
        this.new_localvarliteral(net.blueva.luak.compiler.LexState.Companion.RESERVED_LOCAL_VAR_FOR_STEP)
        this.new_localvar(varname)
        this.checknext('='.code)
        this.exp1() /* initial value */
        this.checknext(','.code)
        this.exp1() /* limit */
        if (this.testnext(','.code)) this.exp1() /* optional step */
        else { /* default step = 1 */
            fs.codeK(fs.freereg, fs.numberK(LuaInteger.valueOf(1)))
            fs.reserveregs(1)
        }
        this.forbody(base, line, 1, true)
    }


    fun forlist(indexname: LuaString?) {
        /* forlist -> NAME {,NAME} IN explist1 forbody */
        val fs: FuncState = this.fs
        val e: expdesc = net.blueva.luak.compiler.LexState.expdesc()
        var nvars = 4 /* gen, state, control, plus at least one declared var */
        val line: Int
        val base: Int = fs.freereg
        /* create control variables */
        this.new_localvarliteral(net.blueva.luak.compiler.LexState.Companion.RESERVED_LOCAL_VAR_FOR_GENERATOR)
        this.new_localvarliteral(net.blueva.luak.compiler.LexState.Companion.RESERVED_LOCAL_VAR_FOR_STATE)
        this.new_localvarliteral(net.blueva.luak.compiler.LexState.Companion.RESERVED_LOCAL_VAR_FOR_CONTROL)
        /* create declared variables */
        this.new_localvar(indexname)
        while (this.testnext(','.code)) {
            this.new_localvar(this.str_checkname())
            ++nvars
        }
        this.checknext(net.blueva.luak.compiler.LexState.Companion.TK_IN)
        line = this.linenumber
        this.adjust_assign(3, this.explist(e), e)
        fs.checkstack(3) /* extra space to call generator */
        this.forbody(base, line, nvars - 3, false)
    }


    fun forstat(line: Int) {
        /* forstat -> FOR (fornum | forlist) END */
        val fs: FuncState = this.fs
        val varname: LuaString?
        val bl: BlockCnt = BlockCnt()
        fs.enterblock(bl, true) /* scope for loop and control variables */
        this.next() /* skip `for' */
        varname = this.str_checkname() /* first variable name */
        when (this.t.token) {
            '=' -> this.fornum(varname, line)
            ',', net.blueva.luak.compiler.LexState.Companion.TK_IN -> this.forlist(varname)
            else -> this.syntaxerror(
                net.blueva.luak.compiler.LexState.Companion.LUA_QL("=")
                    .toString() + " or " + net.blueva.luak.compiler.LexState.Companion.LUA_QL("in") + " expected"
            )
        }
        this.check_match(
            net.blueva.luak.compiler.LexState.Companion.TK_END,
            net.blueva.luak.compiler.LexState.Companion.TK_FOR,
            line
        )
        fs.leaveblock() /* loop scope (`break' jumps to this point) */
    }


    fun test_then_block(escapelist: IntPtr?) {
        /* test_then_block -> [IF | ELSEIF] cond THEN block */
        val v: expdesc = net.blueva.luak.compiler.LexState.expdesc()
        val bl: BlockCnt = BlockCnt()
        val jf: Int /* instruction to skip 'then' code (if condition is false) */
        this.next() /* skip IF or ELSEIF */
        expr(v) /* read expression */
        this.checknext(net.blueva.luak.compiler.LexState.Companion.TK_THEN)
        if (t.token == net.blueva.luak.compiler.LexState.Companion.TK_GOTO || t.token == net.blueva.luak.compiler.LexState.Companion.TK_BREAK) {
            fs!!.goiffalse(v) /* will jump to label if condition is true */
            fs!!.enterblock(bl, false) /* must enter block before 'goto' */
            gotostat(v.t.i) /* handle goto/break */
            skipnoopstat() /* skip other no-op statements */
            if (block_follow(false)) { /* 'goto' is the entire block? */
                fs!!.leaveblock()
                return  /* and that is it */
            } else  /* must skip over 'then' part if condition is false */
                jf = fs!!.jump()
        } else { /* regular case (not goto/break) */
            fs!!.goiftrue(v) /* skip over block if condition is false */
            fs!!.enterblock(bl, false)
            jf = v.f.i
        }
        statlist() /* `then' part */
        fs!!.leaveblock()
        if (t.token == net.blueva.luak.compiler.LexState.Companion.TK_ELSE || t.token == net.blueva.luak.compiler.LexState.Companion.TK_ELSEIF) fs!!.concat(
            escapelist,
            fs!!.jump()
        ) /* must jump over it */
        fs!!.patchtohere(jf)
    }


    fun ifstat(line: Int) {
        val escapelist: IntPtr =
            IntPtr(net.blueva.luak.compiler.LexState.Companion.NO_JUMP) /* exit list for finished parts */
        test_then_block(escapelist) /* IF cond THEN block */
        while (t.token == net.blueva.luak.compiler.LexState.Companion.TK_ELSEIF) test_then_block(escapelist) /* ELSEIF cond THEN block */
        if (testnext(net.blueva.luak.compiler.LexState.Companion.TK_ELSE)) block() /* `else' part */
        check_match(
            net.blueva.luak.compiler.LexState.Companion.TK_END,
            net.blueva.luak.compiler.LexState.Companion.TK_IF,
            line
        )
        fs!!.patchtohere(escapelist.i) /* patch escape list to 'if' end */
    }

    fun localfunc() {
        val b: expdesc = net.blueva.luak.compiler.LexState.expdesc()
        val fs: FuncState = this.fs
        this.new_localvar(this.str_checkname())
        this.adjustlocalvars(1)
        this.body(b, false, this.linenumber)
        /* debug information will only see the variable after this point! */
        fs.getlocvar(fs.nactvar - 1).startpc = fs.pc
    }


    fun localstat() {
        /* stat -> LOCAL NAME {`,' NAME} [`=' explist1] */
        var nvars = 0
        val nexps: Int
        val e: expdesc = net.blueva.luak.compiler.LexState.expdesc()
        do {
            this.new_localvar(this.str_checkname())
            ++nvars
        } while (this.testnext(','.code))
        if (this.testnext('='.code)) nexps = this.explist(e)
        else {
            e.k = net.blueva.luak.compiler.LexState.Companion.VVOID
            nexps = 0
        }
        this.adjust_assign(nvars, nexps, e)
        this.adjustlocalvars(nvars)
    }


    internal fun funcname(v: expdesc): Boolean {
        /* funcname -> NAME {field} [`:' NAME] */
        var ismethod = false
        this.singlevar(v)
        while (this.t.token == '.'.code) this.fieldsel(v)
        if (this.t.token == ':'.code) {
            ismethod = true
            this.fieldsel(v)
        }
        return ismethod
    }


    fun funcstat(line: Int) {
        /* funcstat -> FUNCTION funcname body */
        val needself: Boolean
        val v: expdesc = net.blueva.luak.compiler.LexState.expdesc()
        val b: expdesc = net.blueva.luak.compiler.LexState.expdesc()
        this.next() /* skip FUNCTION */
        needself = this.funcname(v)
        this.body(b, needself, line)
        fs!!.storevar(v, b)
        fs!!.fixline(line) /* definition `happens' in the first line */
    }


    fun exprstat() {
        /* stat -> func | assignment */
        val fs: FuncState = this.fs
        val v: LHS_assign = net.blueva.luak.compiler.LexState.LHS_assign()
        this.suffixedexp(v.v)
        if (t.token == '='.code || t.token == ','.code) { /* stat -> assignment ? */
            v.prev = null
            assignment(v, 1)
        } else {  /* stat -> func */
            check_condition(v.v.k == net.blueva.luak.compiler.LexState.Companion.VCALL, "syntax error")
            SETARG_C(fs.getcodePtr(v.v), 1) /* call statement uses no results */
        }
    }

    fun retstat() {
        /* stat -> RETURN explist */
        val fs: FuncState = this.fs
        val e: expdesc = net.blueva.luak.compiler.LexState.expdesc()
        val first: Int
        var nret: Int /* registers with returned values */
        if (block_follow(true) || this.t.token == ';'.code) {
            nret = 0
            first = nret /* return no values */
        } else {
            nret = this.explist(e) /* optional return values */
            if (hasmultret(e.k)) {
                fs.setmultret(e)
                if (e.k == net.blueva.luak.compiler.LexState.Companion.VCALL && nret == 1) { /* tail call? */
                    SET_OPCODE(fs.getcodePtr(e), Lua.OP_TAILCALL)
                    _assert(Lua.GETARG_A(fs.getcode(e)) == fs.nactvar)
                }
                first = fs.nactvar
                nret = Lua.LUA_MULTRET /* return all values */
            } else {
                if (nret == 1)  /* only one single value? */
                    first = fs.exp2anyreg(e)
                else {
                    fs.exp2nextreg(e) /* values must go to the `stack' */
                    first = fs.nactvar /* return all `active' values */
                    _assert(nret == fs.freereg - first)
                }
            }
        }
        fs.ret(first, nret)
        testnext(';'.code) /* skip optional semicolon */
    }

    fun statement() {
        val line = this.linenumber /* may be needed for error messages */
        enterlevel()
        when (this.t.token) {
            ';' -> {
                /* stat -> ';' (empty statement) */
                next() /* skip ';' */
            }

            net.blueva.luak.compiler.LexState.Companion.TK_IF -> {
                /* stat -> ifstat */
                this.ifstat(line)
            }

            net.blueva.luak.compiler.LexState.Companion.TK_WHILE -> {
                /* stat -> whilestat */
                this.whilestat(line)
            }

            net.blueva.luak.compiler.LexState.Companion.TK_DO -> {
                /* stat -> DO block END */
                this.next() /* skip DO */
                this.block()
                this.check_match(
                    net.blueva.luak.compiler.LexState.Companion.TK_END,
                    net.blueva.luak.compiler.LexState.Companion.TK_DO,
                    line
                )
            }

            net.blueva.luak.compiler.LexState.Companion.TK_FOR -> {
                /* stat -> forstat */
                this.forstat(line)
            }

            net.blueva.luak.compiler.LexState.Companion.TK_REPEAT -> {
                /* stat -> repeatstat */
                this.repeatstat(line)
            }

            net.blueva.luak.compiler.LexState.Companion.TK_FUNCTION -> {
                this.funcstat(line) /* stat -> funcstat */
            }

            net.blueva.luak.compiler.LexState.Companion.TK_LOCAL -> {
                /* stat -> localstat */
                this.next() /* skip LOCAL */
                if (this.testnext(net.blueva.luak.compiler.LexState.Companion.TK_FUNCTION))  /* local function? */
                    this.localfunc()
                else this.localstat()
            }

            net.blueva.luak.compiler.LexState.Companion.TK_DBCOLON -> {
                /* stat -> label */
                next() /* skip double colon */
                labelstat(str_checkname(), line)
            }

            net.blueva.luak.compiler.LexState.Companion.TK_RETURN -> {
                /* stat -> retstat */
                next() /* skip RETURN */
                this.retstat()
            }

            net.blueva.luak.compiler.LexState.Companion.TK_BREAK, net.blueva.luak.compiler.LexState.Companion.TK_GOTO -> {
                /* stat -> breakstat */
                this.gotostat(fs!!.jump())
            }

            else -> {
                this.exprstat()
            }
        }
        _assert(
            fs!!.f!!.maxstacksize >= fs!!.freereg
                    && fs!!.freereg >= fs!!.nactvar
        )
        fs!!.freereg = fs!!.nactvar /* free registers */
        leavelevel()
    }

    fun statlist() {
        /* statlist -> { stat [`;'] } */
        while (!block_follow(true)) {
            if (t.token == net.blueva.luak.compiler.LexState.Companion.TK_RETURN) {
                statement()
                return  /* 'return' must be last statement */
            }
            statement()
        }
    }

    /*
	** compiles the main function, which is a regular vararg function with an
	** upvalue named LUA_ENV
	*/
    internal fun mainfunc(funcstate: FuncState) {
        val bl: BlockCnt = BlockCnt()
        open_func(funcstate, bl)
        fs!!.f!!.is_vararg = 1 /* main function is always vararg */
        val v: expdesc = net.blueva.luak.compiler.LexState.expdesc()
        v.init(net.blueva.luak.compiler.LexState.Companion.VLOCAL, 0) /* create and... */
        fs!!.newupvalue(envn, v) /* ...set environment upvalue */
        next() /* read first token */
        statlist() /* parse main body */
        check(net.blueva.luak.compiler.LexState.Companion.TK_EOS)
        close_func()
    } /* }====================================================================== */

    companion object {
        protected val RESERVED_LOCAL_VAR_FOR_CONTROL: String = "(for control)"
        protected val RESERVED_LOCAL_VAR_FOR_STATE: String = "(for state)"
        protected val RESERVED_LOCAL_VAR_FOR_GENERATOR: String = "(for generator)"
        protected val RESERVED_LOCAL_VAR_FOR_STEP: String = "(for step)"
        protected val RESERVED_LOCAL_VAR_FOR_LIMIT: String = "(for limit)"
        protected val RESERVED_LOCAL_VAR_FOR_INDEX: String = "(for index)"

        // keywords array
        protected val RESERVED_LOCAL_VAR_KEYWORDS: Array<String?> = arrayOf<String>(
            net.blueva.luak.compiler.LexState.Companion.RESERVED_LOCAL_VAR_FOR_CONTROL,
            net.blueva.luak.compiler.LexState.Companion.RESERVED_LOCAL_VAR_FOR_GENERATOR,
            net.blueva.luak.compiler.LexState.Companion.RESERVED_LOCAL_VAR_FOR_INDEX,
            net.blueva.luak.compiler.LexState.Companion.RESERVED_LOCAL_VAR_FOR_LIMIT,
            net.blueva.luak.compiler.LexState.Companion.RESERVED_LOCAL_VAR_FOR_STATE,
            net.blueva.luak.compiler.LexState.Companion.RESERVED_LOCAL_VAR_FOR_STEP
        )
        private val RESERVED_LOCAL_VAR_KEYWORDS_TABLE: Hashtable = Hashtable()

        init {
            for (i in net.blueva.luak.compiler.LexState.Companion.RESERVED_LOCAL_VAR_KEYWORDS.indices) net.blueva.luak.compiler.LexState.Companion.RESERVED_LOCAL_VAR_KEYWORDS_TABLE.put(
                net.blueva.luak.compiler.LexState.Companion.RESERVED_LOCAL_VAR_KEYWORDS[i],
                Boolean.TRUE
            )
        }

        private val EOZ = (-1)
        private val MAX_INT: Int = Integer.MAX_VALUE - 2
        private const val UCHAR_MAX = 255 // TODO, convert to unicode CHAR_MAX?
        private const val LUAI_MAXCCALLS = 200

        private fun LUA_QS(s: String?): String {
            return "'" + s + "'"
        }

        private fun LUA_QL(o: Object?): String {
            return net.blueva.luak.compiler.LexState.Companion.LUA_QS(String.valueOf(o))
        }

        private const val LUA_COMPAT_LSTR = 1 // 1 for compatibility, 2 for old behavior
        private const val LUA_COMPAT_VARARG = true

        fun isReservedKeyword(varName: String?): Boolean {
            return net.blueva.luak.compiler.LexState.Companion.RESERVED_LOCAL_VAR_KEYWORDS_TABLE.containsKey(varName)
        }

        /*
	** Marks the end of a patch list. It is an invalid value both as an absolute
	** address, and as a list link (would link an element to itself).
	*/
        val NO_JUMP: Int = (-1)

        /*
	** grep "ORDER OPR" if you change these enums
	*/
        const val OPR_ADD: Int = 0
        const val OPR_SUB: Int = 1
        const val OPR_MUL: Int = 2
        const val OPR_DIV: Int = 3
        const val OPR_MOD: Int = 4
        const val OPR_POW: Int = 5
        const val OPR_CONCAT: Int = 6
        const val OPR_NE: Int = 7
        const val OPR_EQ: Int = 8
        const val OPR_LT: Int = 9
        const val OPR_LE: Int = 10
        const val OPR_GT: Int = 11
        const val OPR_GE: Int = 12
        const val OPR_AND: Int = 13
        const val OPR_OR: Int = 14
        const val OPR_NOBINOPR: Int = 15

        const val OPR_MINUS: Int = 0
        const val OPR_NOT: Int = 1
        const val OPR_LEN: Int = 2
        const val OPR_NOUNOPR: Int = 3

        /* exp kind */
        const val VVOID: Int = 0 /* no value */
        const val VNIL: Int = 1
        const val VTRUE: Int = 2
        const val VFALSE: Int = 3
        const val VK: Int = 4 /* info = index of constant in `k' */
        const val VKNUM: Int = 5 /* nval = numerical value */
        const val VNONRELOC: Int = 6 /* info = result register */
        const val VLOCAL: Int = 7 /* info = local register */
        const val VUPVAL: Int = 8 /* info = index of upvalue in `upvalues' */
        const val VINDEXED: Int = 9 /* info = table register, aux = index register (or `k') */
        const val VJMP: Int = 10 /* info = instruction pc */
        const val VRELOCABLE: Int = 11 /* info = instruction pc */
        const val VCALL: Int = 12 /* info = instruction pc */
        const val VVARARG: Int = 13 /* info = instruction pc */

        /* ORDER RESERVED */
        val luaX_tokens: Array<String?>? = arrayOf<String?>(
            "and", "break", "do", "else", "elseif",
            "end", "false", "for", "function", "goto", "if",
            "in", "local", "nil", "not", "or", "repeat",
            "return", "then", "true", "until", "while",
            "..", "...", "==", ">=", "<=", "~=",
            "::", "<eos>", "<number>", "<name>", "<string>", "<eof>",
        )

        const val  /* terminal symbols denoted by reserved words */TK_AND: Int = 257
        const val TK_BREAK: Int = 258
        const val TK_DO: Int = 259
        const val TK_ELSE: Int = 260
        const val TK_ELSEIF: Int = 261
        const val TK_END: Int = 262
        const val TK_FALSE: Int = 263
        const val TK_FOR: Int = 264
        const val TK_FUNCTION: Int = 265
        const val TK_GOTO: Int = 266
        const val TK_IF: Int = 267
        const val TK_IN: Int = 268
        const val TK_LOCAL: Int = 269
        const val TK_NIL: Int = 270
        const val TK_NOT: Int = 271
        const val TK_OR: Int = 272
        const val TK_REPEAT: Int = 273
        const val TK_RETURN: Int = 274
        const val TK_THEN: Int = 275
        const val TK_TRUE: Int = 276
        const val TK_UNTIL: Int = 277
        const val TK_WHILE: Int = 278

        /* other terminal symbols */
        const val TK_CONCAT: Int = 279
        const val TK_DOTS: Int = 280
        const val TK_EQ: Int = 281
        const val TK_GE: Int = 282
        const val TK_LE: Int = 283
        const val TK_NE: Int = 284
        const val TK_DBCOLON: Int = 285
        const val TK_EOS: Int = 286
        const val TK_NUMBER: Int = 287
        const val TK_NAME: Int = 288
        const val TK_STRING: Int = 289

        val FIRST_RESERVED: Int = net.blueva.luak.compiler.LexState.Companion.TK_AND
        val NUM_RESERVED: Int =
            net.blueva.luak.compiler.LexState.Companion.TK_WHILE + 1 - net.blueva.luak.compiler.LexState.Companion.FIRST_RESERVED

        val RESERVED: Hashtable = Hashtable()

        init {
            for (i in 0..<net.blueva.luak.compiler.LexState.Companion.NUM_RESERVED) {
                val ts: LuaString? =
                    LuaValue.valueOf(net.blueva.luak.compiler.LexState.Companion.luaX_tokens!![i]) as LuaString?
                net.blueva.luak.compiler.LexState.Companion.RESERVED.put(
                    ts,
                    Integer(net.blueva.luak.compiler.LexState.Companion.FIRST_RESERVED + i)
                )
            }
        }

        private fun iscntrl(token: Int): Boolean {
            return token < ' '.code
        }

        // =============================================================
        // from lcode.h
        // =============================================================
        // =============================================================
        // from lparser.c
        // =============================================================
        fun vkisvar(k: Int): Boolean {
            return (net.blueva.luak.compiler.LexState.Companion.VLOCAL <= (k) && (k) <= net.blueva.luak.compiler.LexState.Companion.VINDEXED)
        }

        fun vkisinreg(k: Int): Boolean {
            return ((k) == net.blueva.luak.compiler.LexState.Companion.VNONRELOC || (k) == net.blueva.luak.compiler.LexState.Companion.VLOCAL)
        }

        /*
	** converts an integer to a "floating point byte", represented as
	** (eeeeexxx), where the real value is (1xxx) * 2^(eeeee - 1) if
	** eeeee != 0 and (xxx) otherwise.
	*/
        fun luaO_int2fb(x: Int): Int {
            var x = x
            var e = 0 /* expoent */
            while (x >= 16) {
                x = (x + 1) shr 1
                e++
            }
            if (x < 8) return x
            else return ((e + 1) shl 3) or (x - 8)
        }


        internal var priority: Array<Priority?> =
            arrayOf<Priority?>( /* ORDER OPR */net.blueva.luak.compiler.LexState.Priority(6, 6),
                net.blueva.luak.compiler.LexState.Priority(6, 6),
                net.blueva.luak.compiler.LexState.Priority(7, 7),
                net.blueva.luak.compiler.LexState.Priority(7, 7),
                net.blueva.luak.compiler.LexState.Priority(7, 7),  /* `+' `-' `/' `%' */
                net.blueva.luak.compiler.LexState.Priority(10, 9),
                net.blueva.luak.compiler.LexState.Priority(5, 4),  /* power and concat (right associative) */
                net.blueva.luak.compiler.LexState.Priority(3, 3),
                net.blueva.luak.compiler.LexState.Priority(3, 3),  /* equality and inequality */
                net.blueva.luak.compiler.LexState.Priority(3, 3),
                net.blueva.luak.compiler.LexState.Priority(3, 3),
                net.blueva.luak.compiler.LexState.Priority(3, 3),
                net.blueva.luak.compiler.LexState.Priority(3, 3),  /* order */
                net.blueva.luak.compiler.LexState.Priority(2, 2),
                net.blueva.luak.compiler.LexState.Priority(1, 1) /* logical (and/or) */
            )

        const val UNARY_PRIORITY: Int = 8 /* priority for unary operators */
    }
}
