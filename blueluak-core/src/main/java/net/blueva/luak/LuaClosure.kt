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

import net.blueva.luak.lib.DebugLib.CallFrame

/**
 * Extension of [LuaFunction] which executes lua bytecode.
 * 
 * 
 * A [LuaClosure] is a combination of a [Prototype]
 * and a [LuaValue] to use as an environment for execution.
 * Normally the [LuaValue] is a [Globals] in which case the environment
 * will contain standard lua libraries.
 * 
 * 
 * 
 * There are three main ways [LuaClosure] instances are created:
 * 
 *  * Construct an instance using [.LuaClosure]
 *  * Construct it indirectly by loading a chunk via [Globals.load]
 *  * Execute the lua bytecode [Lua.OP_CLOSURE] as part of bytecode processing
 * 
 * 
 * 
 * To construct it directly, the [Prototype] is typically created via a compiler such as
 * [net.blueva.luak.compiler.LuaC]:
 * <pre> `String script = "print( 'hello, world' )"; InputStream is = new ByteArrayInputStream(script.getBytes()); Prototype p = LuaC.instance.compile(is, "script"); LuaValue globals = JsePlatform.standardGlobals(); LuaClosure f = new LuaClosure(p, globals); f.call(); `</pre>
 * 
 * 
 * To construct it indirectly, the [Globals.load] method may be used:
 * <pre> `Globals globals = JsePlatform.standardGlobals(); LuaFunction f = globals.load(new StringReader(script), "script"); LuaClosure c = f.checkclosure();  // This may fail if LuaJC is installed. c.call(); `</pre>
 * 
 * 
 * In this example, the "checkclosure()" may fail if direct lua-to-java-bytecode
 * compiling using LuaJC is installed, because no LuaClosure is created in that case
 * and the value returned is a [LuaFunction] but not a [LuaClosure].
 * 
 * 
 * Since a [LuaClosure] is a [LuaFunction] which is a [LuaValue],
 * all the value operations can be used directly such as:
 * 
 *  * [LuaValue.call]
 *  * [LuaValue.call]
 *  * [LuaValue.invoke]
 *  * [LuaValue.invoke]
 *  * [LuaValue.method]
 *  * [LuaValue.method]
 *  * [LuaValue.invokemethod]
 *  * [LuaValue.invokemethod]
 *  *  ...
 * 
 * @see LuaValue
 * 
 * @see LuaFunction
 * 
 * @see LuaValue.isclosure
 * @see LuaValue.checkclosure
 * @see LuaValue.optclosure
 * @see LoadState
 * 
 * @see Globals.compiler
 */
class LuaClosure(p: Prototype, env: LuaValue?) : LuaFunction() {
    val p: Prototype

    var upValues: Array<UpValue?>

    val globals: Globals

    /** Create a closure around a Prototype with a specific environment.
     * If the prototype has upvalues, the environment will be written into the first upvalue.
     * @param p the Prototype to construct this Closure for.
     * @param env the environment to associate with the closure.
     */
    init {
        this.p = p
        this.initupvalue1(env)
        globals = if (env is Globals) env as Globals? else null
    }

    override fun initupvalue1(env: LuaValue?) {        if (p.upvalues == null || p.upvalues.length === 0) this.upValues =
            net.blueva.luak.LuaClosure.Companion.NOUPVALUES
        else {
            this.upValues = arrayOfNulls<UpValue>(p.upvalues.length)
            this.upValues[0] = UpValue(arrayOf<LuaValue?>(env), 0)
        }
    }


    override fun isclosure(): Boolean {
        return true
    }

    override fun optclosure(defval: LuaClosure?): LuaClosure {
        return this
    }

    override fun checkclosure(): LuaClosure {
        return this
    }

    override fun tojstring(): String? {
        return "function: " + p.toString()
    }

    private val newStack: Array<LuaValue>
        get() {
            val max: Int = p.maxstacksize
            val stack: Array<LuaValue> = arrayOfNulls<LuaValue>(max)
            System.arraycopy(NILS, 0, stack, 0, max)
            return stack
        }

    override fun call(): LuaValue {
        override val stack: Array<LuaValue> = this.newStack        return execute(stack, NONE).arg1()
    }

    override fun call(arg: LuaValue): LuaValue {
        val stack: Array<LuaValue> = this.newStack
        when (p.numparams) {
            0 -> return execute(stack, arg).arg1()
            else -> {
                stack[0] = arg
                return execute(stack, NONE).arg1()
            }
        }
    }

    override fun call(arg1: LuaValue?, arg2: LuaValue): LuaValue {
        val stack: Array<LuaValue> = this.newStack
        when (p.numparams) {
            1 -> {
                stack[0] = arg1
                return execute(stack, arg2).arg1()
            }

            0 -> return execute(stack, if (p.is_vararg !== 0) varargsOf(arg1, arg2) else NONE).arg1()
            else -> {
                stack[0] = arg1
                stack[1] = arg2
                return execute(stack, NONE).arg1()
            }
        }
    }

    override fun call(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue): LuaValue {
        val stack: Array<LuaValue> = this.newStack
        when (p.numparams) {
            2 -> {
                stack[0] = arg1
                stack[1] = arg2
                return execute(stack, arg3).arg1()
            }

            1 -> {
                stack[0] = arg1
                return execute(stack, if (p.is_vararg !== 0) varargsOf(arg2, arg3) else NONE).arg1()
            }

            0 -> return execute(stack, if (p.is_vararg !== 0) varargsOf(arg1, arg2, arg3) else NONE).arg1()
            else -> {
                stack[0] = arg1
                stack[1] = arg2
                stack[2] = arg3
                return execute(stack, NONE).arg1()
            }
        }
    }

    override fun invoke(varargs: Varargs): Varargs {
        return onInvoke(varargs).eval()
    }

    override fun onInvoke(varargs: Varargs): Varargs? {
        override val stack: Array<LuaValue> = this.newStack        for (i in 0..<p.numparams) stack[i] = varargs.arg(i + 1)
        return execute(stack, if (p.is_vararg !== 0) varargs.subargs(p.numparams + 1) else NONE)
    }

    protected fun execute(stack: Array<LuaValue>, varargs: Varargs): Varargs? {
        // loop through instructions
        var i: Int
        var a: Int
        var b: Int
        var c: Int
        var pc = 0
        var top = 0
        var o: LuaValue
        var v: Varargs = NONE
        val code: IntArray = p.code
        val k: Array<LuaValue?> = p.k


        // upvalues are only possible when closures create closures
        // TODO: use linked list.
        val openups: Array<UpValue?>? = if (p.p.length > 0) arrayOfNulls<UpValue>(stack.size) else null


        // allow for debug hooks
        if (globals != null && globals.debuglib != null) globals.debuglib.onCall(this, varargs, stack)

        // process instructions
        try {
            while (true) {
                if (globals != null && globals.debuglib != null) globals.debuglib.onInstruction(pc, v, top)


                // pull out instruction
                i = code[pc]
                a = ((i shr 6) and 0xff)


                // process the op code
                when (i and 0x3f) {
                    Lua.OP_MOVE -> {
                        stack[a] = stack[i ushr 23]
                        ++pc
                        continue
                    }

                    Lua.OP_LOADK -> {
                        stack[a] = k[i ushr 14]
                        ++pc
                        continue
                    }

                    Lua.OP_LOADKX -> {
                        ++pc
                        i = code[pc]
                        if ((i and 0x3f) != Lua.OP_EXTRAARG) {
                            val op = i and 0x3f
                            throw LuaError(
                                "OP_EXTRAARG expected after OP_LOADKX, got " +
                                        (if (op < Print.OPNAMES.length - 1) Print.OPNAMES[op] else "UNKNOWN_OP_" + op)
                            )
                        }
                        stack[a] = k[i ushr 6]
                        ++pc
                        continue
                    }

                    Lua.OP_LOADBOOL -> {
                        stack[a] = if (i ushr 23 != 0) LuaValue.TRUE else LuaValue.FALSE
                        if ((i and (0x1ff shl 14)) != 0) ++pc /* skip next instruction (if C) */
                        ++pc
                        continue
                    }

                    Lua.OP_LOADNIL -> {
                        b = i ushr 23
                        while (b-- >= 0) {
                            stack[a++] = LuaValue.NIL
                        }
                        ++pc
                        continue
                    }

                    Lua.OP_GETUPVAL -> {
                        stack[a] = upValues[i ushr 23].getValue()
                        ++pc
                        continue
                    }

                    Lua.OP_GETTABUP -> {
                        stack[a] = upValues[i ushr 23].getValue()
                            .get(if ((((i shr 14) and 0x1ff).also { c = it }) > 0xff) k[c and 0x0ff] else stack[c])
                        ++pc
                        continue
                    }

                    Lua.OP_GETTABLE -> {
                        stack[a] = stack[i ushr 23].get(if ((((i shr 14) and 0x1ff).also {
                                c = it
                            }) > 0xff) k[c and 0x0ff] else stack[c])
                        ++pc
                        continue
                    }

                    Lua.OP_SETTABUP -> {
                        upValues[a].getValue()
                            .set(
                                (if (((i ushr 23).also { b = it }) > 0xff) k[b and 0x0ff] else stack[b]),
                                if ((((i shr 14) and 0x1ff).also { c = it }) > 0xff) k[c and 0x0ff] else stack[c]
                            )
                        ++pc
                        continue
                    }

                    Lua.OP_SETUPVAL -> {
                        upValues[i ushr 23].setValue(stack[a])
                        ++pc
                        continue
                    }

                    Lua.OP_SETTABLE -> {
                        stack[a].set(
                            (if (((i ushr 23).also { b = it }) > 0xff) k[b and 0x0ff] else stack[b]),
                            if ((((i shr 14) and 0x1ff).also { c = it }) > 0xff) k[c and 0x0ff] else stack[c]
                        )
                        ++pc
                        continue
                    }

                    Lua.OP_NEWTABLE -> {
                        stack[a] = LuaTable(i ushr 23, (i shr 14) and 0x1ff)
                        ++pc
                        continue
                    }

                    Lua.OP_SELF -> {
                        stack[a + 1] = (stack[i ushr 23].also { o = it })
                        stack[a] =
                            o.get(if ((((i shr 14) and 0x1ff).also { c = it }) > 0xff) k[c and 0x0ff] else stack[c])
                        ++pc
                        continue
                    }

                    Lua.OP_ADD -> {
                        stack[a] = (if (((i ushr 23).also {
                                b = it
                            }) > 0xff) k[b and 0x0ff] else stack[b]).add(if ((((i shr 14) and 0x1ff).also {
                                c = it
                            }) > 0xff) k[c and 0x0ff] else stack[c])
                        ++pc
                        continue
                    }

                    Lua.OP_SUB -> {
                        stack[a] = (if (((i ushr 23).also {
                                b = it
                            }) > 0xff) k[b and 0x0ff] else stack[b]).sub(if ((((i shr 14) and 0x1ff).also {
                                c = it
                            }) > 0xff) k[c and 0x0ff] else stack[c])
                        ++pc
                        continue
                    }

                    Lua.OP_MUL -> {
                        stack[a] = (if (((i ushr 23).also {
                                b = it
                            }) > 0xff) k[b and 0x0ff] else stack[b]).mul(if ((((i shr 14) and 0x1ff).also {
                                c = it
                            }) > 0xff) k[c and 0x0ff] else stack[c])
                        ++pc
                        continue
                    }

                    Lua.OP_DIV -> {
                        stack[a] = (if (((i ushr 23).also {
                                b = it
                            }) > 0xff) k[b and 0x0ff] else stack[b]).div(if ((((i shr 14) and 0x1ff).also {
                                c = it
                            }) > 0xff) k[c and 0x0ff] else stack[c])
                        ++pc
                        continue
                    }

                    Lua.OP_MOD -> {
                        stack[a] = (if (((i ushr 23).also {
                                b = it
                            }) > 0xff) k[b and 0x0ff] else stack[b]).mod(if ((((i shr 14) and 0x1ff).also {
                                c = it
                            }) > 0xff) k[c and 0x0ff] else stack[c])
                        ++pc
                        continue
                    }

                    Lua.OP_POW -> {
                        stack[a] = (if (((i ushr 23).also {
                                b = it
                            }) > 0xff) k[b and 0x0ff] else stack[b]).pow(if ((((i shr 14) and 0x1ff).also {
                                c = it
                            }) > 0xff) k[c and 0x0ff] else stack[c])
                        ++pc
                        continue
                    }

                    Lua.OP_UNM -> {
                        stack[a] = stack[i ushr 23].neg()
                        ++pc
                        continue
                    }

                    Lua.OP_NOT -> {
                        stack[a] = stack[i ushr 23].not()
                        ++pc
                        continue
                    }

                    Lua.OP_LEN -> {
                        stack[a] = stack[i ushr 23].len()
                        ++pc
                        continue
                    }

                    Lua.OP_CONCAT -> {
                        b = i ushr 23
                        c = (i shr 14) and 0x1ff
                        run {
                            if (c > b + 1) {
                                val sb: Buffer = stack[c].buffer()
                                while (--c >= b) sb.concatTo(stack[c])
                                stack[a] = sb.value()
                            } else {
                                stack[a] = stack[c - 1].concat(stack[c])
                            }
                        }
                        ++pc
                        continue
                    }

                    Lua.OP_JMP -> {
                        pc += (i ushr 14) - 0x1ffff
                        if (a > 0) {
                            --a
                            b = openups!!.size
                            while (--b >= 0) {
                                if (openups[b] != null && openups[b].index >= a) {
                                    openups[b].close()
                                    openups[b] = null
                                }
                            }
                        }
                        ++pc
                        continue
                    }

                    Lua.OP_EQ -> {
                        if ((if (((i ushr 23).also {
                                    b = it
                                }) > 0xff) k[b and 0x0ff] else stack[b]).eq_b(if ((((i shr 14) and 0x1ff).also {
                                    c = it
                                }) > 0xff) k[c and 0x0ff] else stack[c]) !== (a != 0)) ++pc
                        ++pc
                        continue
                    }

                    Lua.OP_LT -> {
                        if ((if (((i ushr 23).also {
                                    b = it
                                }) > 0xff) k[b and 0x0ff] else stack[b]).lt_b(if ((((i shr 14) and 0x1ff).also {
                                    c = it
                                }) > 0xff) k[c and 0x0ff] else stack[c]) !== (a != 0)) ++pc
                        ++pc
                        continue
                    }

                    Lua.OP_LE -> {
                        if ((if (((i ushr 23).also {
                                    b = it
                                }) > 0xff) k[b and 0x0ff] else stack[b]).lteq_b(if ((((i shr 14) and 0x1ff).also {
                                    c = it
                                }) > 0xff) k[c and 0x0ff] else stack[c]) !== (a != 0)) ++pc
                        ++pc
                        continue
                    }

                    Lua.OP_TEST -> {
                        if (stack[a].toboolean() !== ((i and (0x1ff shl 14)) != 0)) ++pc
                        ++pc
                        continue
                    }

                    Lua.OP_TESTSET -> {
                        /* note: doc appears to be reversed */
                        if ((stack[i ushr 23].also { o = it }).toboolean() !== ((i and (0x1ff shl 14)) != 0)) ++pc
                        else stack[a] = o // TODO: should be sBx?

                        ++pc
                        continue
                    }

                    Lua.OP_CALL -> when (i and (Lua.MASK_B or Lua.MASK_C)) {
                        (1 shl Lua.POS_B) or (0 shl Lua.POS_C) -> {
                            v = stack[a].invoke(NONE)
                            top = a + v.narg()
                            ++pc
                            continue
                        }

                        (2 shl Lua.POS_B) or (0 shl Lua.POS_C) -> {
                            v = stack[a].invoke(stack[a + 1])
                            top = a + v.narg()
                            ++pc
                            continue
                        }

                        (1 shl Lua.POS_B) or (1 shl Lua.POS_C) -> {
                            stack[a].call()
                            ++pc
                            continue
                        }

                        (2 shl Lua.POS_B) or (1 shl Lua.POS_C) -> {
                            stack[a].call(stack[a + 1])
                            ++pc
                            continue
                        }

                        (3 shl Lua.POS_B) or (1 shl Lua.POS_C) -> {
                            stack[a].call(stack[a + 1], stack[a + 2])
                            ++pc
                            continue
                        }

                        (4 shl Lua.POS_B) or (1 shl Lua.POS_C) -> {
                            stack[a].call(stack[a + 1], stack[a + 2], stack[a + 3])
                            ++pc
                            continue
                        }

                        (1 shl Lua.POS_B) or (2 shl Lua.POS_C) -> {
                            stack[a] = stack[a].call()
                            ++pc
                            continue
                        }

                        (2 shl Lua.POS_B) or (2 shl Lua.POS_C) -> {
                            stack[a] = stack[a].call(stack[a + 1])
                            ++pc
                            continue
                        }

                        (3 shl Lua.POS_B) or (2 shl Lua.POS_C) -> {
                            stack[a] = stack[a].call(stack[a + 1], stack[a + 2])
                            ++pc
                            continue
                        }

                        (4 shl Lua.POS_B) or (2 shl Lua.POS_C) -> {
                            stack[a] = stack[a].call(stack[a + 1], stack[a + 2], stack[a + 3])
                            ++pc
                            continue
                        }

                        else -> {
                            b = i ushr 23
                            c = (i shr 14) and 0x1ff
                            v = stack[a].invoke(
                                if (b > 0) varargsOf(stack, a + 1, b - 1) else  // exact arg count
                                    varargsOf(stack, a + 1, top - v.narg() - (a + 1), v)
                            ) // from prev top
                            if (c > 0) {
                                v.copyto(stack, a, c - 1)
                                v = NONE
                            } else {
                                top = a + v.narg()
                                v = v.dealias()
                            }
                            ++pc
                            continue
                        }
                    }

                    Lua.OP_TAILCALL -> when (i and Lua.MASK_B) {
                        (1 shl Lua.POS_B) -> return TailcallVarargs(stack[a], NONE)
                        (2 shl Lua.POS_B) -> return TailcallVarargs(stack[a], stack[a + 1])
                        (3 shl Lua.POS_B) -> return TailcallVarargs(stack[a], varargsOf(stack[a + 1], stack[a + 2]))
                        (4 shl Lua.POS_B) -> return TailcallVarargs(
                            stack[a],
                            varargsOf(stack[a + 1], stack[a + 2], stack[a + 3])
                        )

                        else -> {
                            b = i ushr 23
                            v = if (b > 0) varargsOf(stack, a + 1, b - 1) else  // exact arg count
                                varargsOf(stack, a + 1, top - v.narg() - (a + 1), v) // from prev top
                            return TailcallVarargs(stack[a], v)
                        }
                    }

                    Lua.OP_RETURN -> {
                        b = i ushr 23
                        when (b) {
                            0 -> return varargsOf(stack, a, top - v.narg() - a, v)
                            1 -> return NONE
                            2 -> return stack[a]
                            else -> return varargsOf(stack, a, b - 1)
                        }
                    }

                    Lua.OP_FORLOOP -> {
                        run {
                            val limit: LuaValue? = stack[a + 1]
                            val step: LuaValue = stack[a + 2]
                            val idx: LuaValue = stack[a].add(step)
                            if (if (step.gt_b(0)) idx.lteq_b(limit) else idx.gteq_b(limit)) {
                                stack[a] = idx
                                stack[a + 3] = idx
                                pc += (i ushr 14) - 0x1ffff
                            }
                        }
                        ++pc
                        continue
                    }

                    Lua.OP_FORPREP -> {
                        run {
                            val init: LuaValue = stack[a].checknumber("'for' initial value must be a number")
                            val limit: LuaValue? = stack[a + 1].checknumber("'for' limit must be a number")
                            val step: LuaValue? = stack[a + 2].checknumber("'for' step must be a number")
                            stack[a] = init.sub(step)
                            stack[a + 1] = limit
                            stack[a + 2] = step
                            pc += (i ushr 14) - 0x1ffff
                        }
                        ++pc
                        continue
                    }

                    Lua.OP_TFORCALL -> {
                        v = stack[a].invoke(varargsOf(stack[a + 1], stack[a + 2]))
                        c = (i shr 14) and 0x1ff
                        while (--c >= 0) stack[a + 3 + c] = v.arg(c + 1)
                        v = NONE
                        ++pc
                        continue
                    }

                    Lua.OP_TFORLOOP -> {
                        if (!stack[a + 1].isnil()) { /* continue loop? */
                            stack[a] = stack[a + 1] /* save control varible. */
                            pc += (i ushr 14) - 0x1ffff
                        }
                        ++pc
                        continue
                    }

                    Lua.OP_SETLIST -> {
                        run {
                            if ((((i shr 14) and 0x1ff).also { c = it }) == 0) c = code[++pc]
                            val offset: Int = (c - 1) * Lua.LFIELDS_PER_FLUSH
                            o = stack[a]
                            if (((i ushr 23).also { b = it }) == 0) {
                                b = top - a - 1
                                val m: Int = b - v.narg()
                                var j = 1
                                while (j <= m) {
                                    o.set(offset + j, stack[a + j])
                                    j++
                                }
                                while (j <= b) {
                                    o.set(offset + j, v.arg(j - m))
                                    j++
                                }
                            } else {
                                o.presize(offset + b)
                                var j = 1
                                while (j <= b) {
                                    o.set(offset + j, stack[a + j])
                                    j++
                                }
                            }
                        }
                        ++pc
                        continue
                    }

                    Lua.OP_CLOSURE -> {
                        run {
                            val newp: Prototype = p.p[i ushr 14]
                            val ncl: LuaClosure = net.blueva.luak.LuaClosure(newp, globals)
                            val uv: Array<Upvaldesc?> = newp.upvalues
                            var j = 0
                            val nup = uv.size
                            while (j < nup) {
                                if (uv[j].instack)  /* upvalue refes to local variable? */
                                    ncl.upValues[j] = findupval(stack, uv[j].idx, openups)
                                else  /* get upvalue from enclosing function */
                                    ncl.upValues[j] = upValues[uv[j].idx]
                                ++j
                            }
                            stack[a] = ncl
                        }
                        ++pc
                        continue
                    }

                    Lua.OP_VARARG -> {
                        b = i ushr 23
                        if (b == 0) {
                            top = a + (varargs.narg().also { b = it })
                            v = varargs
                        } else {
                            var j = 1
                            while (j < b) {
                                stack[a + j - 1] = varargs.arg(j)
                                ++j
                            }
                        }
                        ++pc
                        continue
                    }

                    Lua.OP_EXTRAARG -> throw IllegalArgumentException("Uexecutable opcode: OP_EXTRAARG")

                    else -> throw IllegalArgumentException("Illegal opcode: " + (i and 0x3f))
                }
                ++pc
            }
        } catch (le: LuaError) {
            if (le.traceback == null) processErrorHooks(le, p, pc)
            throw le
        } catch (e: Exception) {
            val le: LuaError = LuaError(e)
            processErrorHooks(le, p, pc)
            throw le
        } finally {
            if (openups != null) {
                var u = openups.size
                while (--u >= 0) {
                    if (openups[u] != null) openups[u].close()
                }
            }
            if (globals != null && globals.debuglib != null) globals.debuglib.onReturn()
        }
    }

    /**
     * Run the error hook if there is one
     * @param msg the message to use in error hook processing.
     */
    fun errorHook(msg: String?, level: Int): String? {
        if (globals == null) return msg
        val r: LuaThread = globals.running
        if (r.errorfunc == null) return if (globals.debuglib != null) msg.toString() + "\n" + globals.debuglib.traceback(
            level
        ) else msg
        val e: LuaValue = r.errorfunc
        r.errorfunc = null
        try {
            return e.call(LuaValue.valueOf(msg)).tojstring()
        } catch (t: Throwable) {
            return "error in error handling"
        } finally {
            r.errorfunc = e
        }
    }

    private fun processErrorHooks(le: LuaError, p: Prototype, pc: Int) {
        var file: String? = "?"
        var line = -1
        run {
            var frame: CallFrame? = null
            if (globals != null && globals.debuglib != null) {
                frame = globals.debuglib.getCallFrame(le.level)
                if (frame != null) {
                    val src: String? = frame.shortsource()
                    file = if (src != null) src else "?"
                    line = frame.currentline()
                }
            }
            if (frame == null) {
                file = if (p.source != null) p.source.tojstring() else "?"
                line = if (p.lineinfo != null && pc >= 0 && pc < p.lineinfo.length) p.lineinfo[pc] else -1
            }
        }
        le.fileline = file.toString() + ":" + line
        le.traceback = errorHook(le.getMessage(), le.level)
    }

    private fun findupval(stack: Array<LuaValue>?, idx: Short, openups: Array<UpValue?>): UpValue? {
        val n = openups.size
        for (i in 0..<n) if (openups[i] != null && openups[i].index === idx) return openups[i]
        for (i in 0..<n) if (openups[i] == null) return UpValue(stack, idx).also { openups[i] = it }
        error("No space for upvalue")
        return null
    }

    protected fun getUpvalue(i: Int): LuaValue {
        return upValues[i].getValue()
    }

    protected fun setUpvalue(i: Int, v: LuaValue?) {
        upValues[i].setValue(v)
    }

    override fun name(): String? {
        return "<" + p.shortsource() + ":" + p.linedefined + ">"
    }


    companion object {
        private val NOUPVALUES: Array<UpValue?> = arrayOfNulls<UpValue>(0)
    }
}
