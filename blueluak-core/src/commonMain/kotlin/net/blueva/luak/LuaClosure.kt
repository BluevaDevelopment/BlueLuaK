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
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/** Drives a suspend computation that is not expected to actually suspend here
 * (no active, yield-propagating coroutine chain reaches this call) to
 * completion synchronously. If it tries to suspend anyway - e.g. Lua code
 * called from a library function like `table.sort`'s comparator calls
 * `coroutine.yield()` - that correctly surfaces as a boundary error, exactly
 * like real Lua's C-call boundary restriction. */
internal fun <T> runLuaSync(block: suspend () -> T): T {
    var outcome: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { outcome = it })
    val result = outcome ?: throw LuaError("attempt to yield across metamethod/C-call boundary")
    return result.getOrThrow()
}

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
 * <pre> `String script = "print( 'hello, world' )"; InputStream is = new ByteArrayInputStream(script.toByteArray()); Prototype p = LuaC.instance.compile(is, "script"); LuaValue globals = JvmPlatform.standardGlobals(); LuaClosure f = new LuaClosure(p, globals); f.call(); `</pre>
 * 
 * 
 * To construct it indirectly, the [Globals.load] method may be used:
 * <pre> `Globals globals = JvmPlatform.standardGlobals(); LuaFunction f = globals.load(new StringReader(script), "script"); LuaClosure c = f.checkclosure();  // This may fail if LuaJC is installed. c.call(); `</pre>
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

    lateinit var upValues: Array<UpValue?>

    val globals: Globals?

    /** Create a closure around a Prototype with a specific environment.
     * If the prototype has upvalues, the environment will be written into the first upvalue.
     * @param p the Prototype to construct this Closure for.
     * @param env the environment to associate with the closure.
     */
    init {
        this.p = p
        this.initupvalue1(env)
        globals = env as? Globals
    }

    override fun initupvalue1(env: LuaValue?) {
        if (p.upvalues == null || p.upvalues!!.size === 0) this.upValues =
            net.blueva.luak.LuaClosure.Companion.NOUPVALUES
        else {
            this.upValues = arrayOfNulls<UpValue>(p.upvalues!!.size)
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

    override fun tojstring(): String {
        return "function: " + p.toString()
    }

    private val newStack: Array<LuaValue>
        get() {
            val max: Int = p.maxstacksize
            val stack: Array<LuaValue> = Array(max) { NIL }
            return stack
        }

    // Note: execute() may return a TailcallVarargs; must resolve it via
    // evalSuspend() (not the plain, non-suspend arg1()/eval()) so a tail
    // call ending in coroutine.yield() still propagates suspension.
    private suspend fun call0(): LuaValue {
        val stack: Array<LuaValue> = this.newStack
        return (execute(stack, (NONE)!!)!!.evalSuspend().arg1())
    }

    private suspend fun call1(arg: LuaValue?): LuaValue {
        val stack: Array<LuaValue> = this.newStack
        when (p.numparams) {
            0 -> return (execute(stack, arg!!)!!.evalSuspend().arg1())
            else -> {
                stack[0] = arg!!
                return (execute(stack, (NONE)!!)!!.evalSuspend().arg1())
            }
        }
    }

    private suspend fun call2(arg1: LuaValue?, arg2: LuaValue?): LuaValue {
        val stack: Array<LuaValue> = this.newStack
        when (p.numparams) {
            1 -> {
                stack[0] = arg1!!
                return (execute(stack, arg2!!)!!.evalSuspend().arg1())
            }

            0 -> return (execute(stack, (if (p.is_vararg !== 0) varargsOf(arg1, arg2!!) else NONE)!!)!!.evalSuspend().arg1())
            else -> {
                stack[0] = arg1!!
                stack[1] = arg2!!
                return (execute(stack, (NONE)!!)!!.evalSuspend().arg1())
            }
        }
    }

    private suspend fun call3(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?): LuaValue {
        val stack: Array<LuaValue> = this.newStack
        when (p.numparams) {
            2 -> {
                stack[0] = arg1!!
                stack[1] = arg2!!
                return (execute(stack, arg3!!)!!.evalSuspend().arg1())
            }

            1 -> {
                stack[0] = arg1!!
                return (execute(stack, (if (p.is_vararg !== 0) varargsOf(arg2, arg3!!) else NONE)!!)!!.evalSuspend().arg1())
            }

            0 -> return (execute(stack, (if (p.is_vararg !== 0) varargsOf(arg1, arg2, arg3!!) else NONE)!!)!!.evalSuspend().arg1())
            else -> {
                stack[0] = arg1!!
                stack[1] = arg2!!
                stack[2] = arg3!!
                return (execute(stack, (NONE)!!)!!.evalSuspend().arg1())
            }
        }
    }

    private suspend fun onInvokeImpl(varargs: Varargs): Varargs? {
        val stack: Array<LuaValue> = this.newStack
        for (i in 0..<p.numparams) stack[i] = varargs.arg(i + 1)
        return execute(stack, (if (p.is_vararg !== 0) varargs.subargs(p.numparams + 1) else NONE)!!)
    }

    override fun call(): LuaValue = runLuaSync { call0() }
    override fun call(arg: LuaValue?): LuaValue = runLuaSync { call1(arg) }
    override fun call(arg1: LuaValue?, arg2: LuaValue?): LuaValue = runLuaSync { call2(arg1, arg2) }
    override fun call(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?): LuaValue =
        runLuaSync { call3(arg1, arg2, arg3) }

    override fun invoke(varargs: Varargs): Varargs = runLuaSync { onInvokeImpl(varargs)!!.evalSuspend() }
    override fun onInvoke(varargs: Varargs): Varargs? = runLuaSync { onInvokeImpl(varargs) }

    override suspend fun callSuspend(): LuaValue? = call0()
    override suspend fun callSuspend(arg: LuaValue?): LuaValue? = call1(arg)
    override suspend fun callSuspend(arg1: LuaValue?, arg2: LuaValue?): LuaValue? = call2(arg1, arg2)
    override suspend fun callSuspend(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?): LuaValue? =
        call3(arg1, arg2, arg3)

    override suspend fun invokeSuspend(args: Varargs): Varargs = onInvokeImpl(args)!!.evalSuspend()
    override suspend fun onInvokeSuspend(args: Varargs): Varargs? = onInvokeImpl(args)

    protected suspend fun execute(stack: Array<LuaValue>, varargs: Varargs): Varargs? {
        // loop through instructions
        var i: Int
        var a: Int
        var b: Int
        var c: Int
        var pc = 0
        var top = 0
        var o: LuaValue
        var v: Varargs = NONE!!
        val code: IntArray = p.code!!
        val k: Array<LuaValue?> = p.k!!


        // upvalues are only possible when closures create closures
        // TODO: use linked list.
        val openups: Array<UpValue?>? = if (p.p!!.size > 0) arrayOfNulls<UpValue>(stack.size) else null


        // allow for debug hooks
        if (globals != null && globals.debuglib != null) globals.debuglib!!.onCall(this, varargs, stack as Array<LuaValue?>)

        // process instructions
        try {
            while (true) {
                if (globals != null && globals.debuglib != null) globals.debuglib!!.onInstruction(pc, v, top)


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
                        stack[a] = k[i ushr 14]!!
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
                                        (if (op < Print.OPNAMES.size - 1) Print.OPNAMES[op] else "UNKNOWN_OP_" + op)
                            )
                        }
                        stack[a] = k[i ushr 6]!!
                        ++pc
                        continue
                    }

                    Lua.OP_LOADBOOL -> {
                        stack[a] = (if (i ushr 23 != 0) LuaValue.TRUE else LuaValue.FALSE)!!
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
                        stack[a] = upValues[i ushr 23]!!.getValue()!!
                        ++pc
                        continue
                    }

                    Lua.OP_GETTABUP -> {
                        c = (i shr 14) and 0x1ff
                        stack[a] = upValues[i ushr 23]!!.getValue()!!
                            .get(if (c > 0xff) k[c and 0x0ff]!! else stack[c])
                        ++pc
                        continue
                    }

                    Lua.OP_GETTABLE -> {
                        c = (i shr 14) and 0x1ff
                        stack[a] = stack[i ushr 23].get(if (c > 0xff) k[c and 0x0ff]!! else stack[c])
                        ++pc
                        continue
                    }

                    Lua.OP_SETTABUP -> {
                        b = i ushr 23
                        c = (i shr 14) and 0x1ff
                        upValues[a]!!.getValue()!!
                            .set(
                                if (b > 0xff) k[b and 0x0ff] else stack[b],
                                if (c > 0xff) k[c and 0x0ff] else stack[c]
                            )
                        ++pc
                        continue
                    }

                    Lua.OP_SETUPVAL -> {
                        upValues[i ushr 23]!!.setValue(stack[a])
                        ++pc
                        continue
                    }

                    Lua.OP_SETTABLE -> {
                        b = i ushr 23
                        c = (i shr 14) and 0x1ff
                        stack[a].set(
                            if (b > 0xff) k[b and 0x0ff] else stack[b],
                            if (c > 0xff) k[c and 0x0ff] else stack[c]
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
                        o = stack[i ushr 23]
                        stack[a + 1] = o
                        c = (i shr 14) and 0x1ff
                        stack[a] = o.get(if (c > 0xff) k[c and 0x0ff]!! else stack[c])
                        ++pc
                        continue
                    }

                    Lua.OP_ADD -> {
                        b = i ushr 23
                        c = (i shr 14) and 0x1ff
                        stack[a] = (if (b > 0xff) k[b and 0x0ff]!! else stack[b])
                            .add(if (c > 0xff) k[c and 0x0ff]!! else stack[c])
                        ++pc
                        continue
                    }

                    Lua.OP_SUB -> {
                        b = i ushr 23
                        c = (i shr 14) and 0x1ff
                        stack[a] = (if (b > 0xff) k[b and 0x0ff]!! else stack[b])
                            .sub(if (c > 0xff) k[c and 0x0ff]!! else stack[c])
                        ++pc
                        continue
                    }

                    Lua.OP_MUL -> {
                        b = i ushr 23
                        c = (i shr 14) and 0x1ff
                        stack[a] = (if (b > 0xff) k[b and 0x0ff]!! else stack[b])
                            .mul(if (c > 0xff) k[c and 0x0ff]!! else stack[c])
                        ++pc
                        continue
                    }

                    Lua.OP_DIV -> {
                        b = i ushr 23
                        c = (i shr 14) and 0x1ff
                        stack[a] = (if (b > 0xff) k[b and 0x0ff]!! else stack[b])
                            .div(if (c > 0xff) k[c and 0x0ff]!! else stack[c])
                        ++pc
                        continue
                    }

                    Lua.OP_MOD -> {
                        b = i ushr 23
                        c = (i shr 14) and 0x1ff
                        stack[a] = (if (b > 0xff) k[b and 0x0ff]!! else stack[b])
                            .mod(if (c > 0xff) k[c and 0x0ff]!! else stack[c])
                        ++pc
                        continue
                    }

                    Lua.OP_POW -> {
                        b = i ushr 23
                        c = (i shr 14) and 0x1ff
                        stack[a] = (if (b > 0xff) k[b and 0x0ff]!! else stack[b])
                            .pow(if (c > 0xff) k[c and 0x0ff]!! else stack[c])
                        ++pc
                        continue
                    }

                    Lua.OP_UNM -> {
                        stack[a] = stack[i ushr 23].neg()
                        ++pc
                        continue
                    }

                    Lua.OP_NOT -> {
                        stack[a] = stack[i ushr 23].not()!!
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
                        if (c > b + 1) {
                            val sb: Buffer = stack[c].buffer()!!
                            while (--c >= b) sb.concatTo(stack[c])
                            stack[a] = sb.value()!!
                        } else {
                            stack[a] = stack[c - 1].concat(stack[c])
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
                                if (openups[b] != null && openups[b]!!.index >= a) {
                                    openups[b]!!.close()
                                    openups[b] = null
                                }
                            }
                        }
                        ++pc
                        continue
                    }

                    Lua.OP_EQ -> {
                        b = i ushr 23
                        c = (i shr 14) and 0x1ff
                        if ((if (b > 0xff) k[b and 0x0ff]!! else stack[b])
                                .eq_b(if (c > 0xff) k[c and 0x0ff] else stack[c])!! !== (a != 0)) ++pc
                        ++pc
                        continue
                    }

                    Lua.OP_LT -> {
                        b = i ushr 23
                        c = (i shr 14) and 0x1ff
                        if ((if (b > 0xff) k[b and 0x0ff]!! else stack[b])
                                .lt_b(if (c > 0xff) k[c and 0x0ff]!! else stack[c]) !== (a != 0)) ++pc
                        ++pc
                        continue
                    }

                    Lua.OP_LE -> {
                        b = i ushr 23
                        c = (i shr 14) and 0x1ff
                        if ((if (b > 0xff) k[b and 0x0ff]!! else stack[b])
                                .lteq_b(if (c > 0xff) k[c and 0x0ff]!! else stack[c]) !== (a != 0)) ++pc
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
                        o = stack[i ushr 23]
                        if (o.toboolean() !== ((i and (0x1ff shl 14)) != 0)) ++pc
                        else stack[a] = o // TODO: should be sBx?

                        ++pc
                        continue
                    }

                    Lua.OP_CALL -> when (i and (Lua.MASK_B or Lua.MASK_C)) {
                        (1 shl Lua.POS_B) or (0 shl Lua.POS_C) -> {
                            v = stack[a].invokeSuspend((NONE)!!)
                            top = a + v.narg()
                            ++pc
                            continue
                        }

                        (2 shl Lua.POS_B) or (0 shl Lua.POS_C) -> {
                            v = stack[a].invokeSuspend(stack[a + 1])
                            top = a + v.narg()
                            ++pc
                            continue
                        }

                        (1 shl Lua.POS_B) or (1 shl Lua.POS_C) -> {
                            stack[a].callSuspend()
                            ++pc
                            continue
                        }

                        (2 shl Lua.POS_B) or (1 shl Lua.POS_C) -> {
                            stack[a].callSuspend(stack[a + 1])
                            ++pc
                            continue
                        }

                        (3 shl Lua.POS_B) or (1 shl Lua.POS_C) -> {
                            stack[a].callSuspend(stack[a + 1], stack[a + 2])
                            ++pc
                            continue
                        }

                        (4 shl Lua.POS_B) or (1 shl Lua.POS_C) -> {
                            stack[a].callSuspend(stack[a + 1], stack[a + 2], stack[a + 3])
                            ++pc
                            continue
                        }

                        (1 shl Lua.POS_B) or (2 shl Lua.POS_C) -> {
                            stack[a] = stack[a].callSuspend()!!
                            ++pc
                            continue
                        }

                        (2 shl Lua.POS_B) or (2 shl Lua.POS_C) -> {
                            stack[a] = stack[a].callSuspend(stack[a + 1])!!
                            ++pc
                            continue
                        }

                        (3 shl Lua.POS_B) or (2 shl Lua.POS_C) -> {
                            stack[a] = stack[a].callSuspend(stack[a + 1], stack[a + 2])!!
                            ++pc
                            continue
                        }

                        (4 shl Lua.POS_B) or (2 shl Lua.POS_C) -> {
                            stack[a] = stack[a].callSuspend(stack[a + 1], stack[a + 2], stack[a + 3])!!
                            ++pc
                            continue
                        }

                        else -> {
                            b = i ushr 23
                            c = (i shr 14) and 0x1ff
                            v = stack[a].invokeSuspend(
                                if (b > 0) varargsOf(stack, a + 1, b - 1) else  // exact arg count
                                    varargsOf(stack, a + 1, top - v.narg() - (a + 1), v)
                            ) // from prev top
                            if (c > 0) {
                                v.copyto(stack as Array<LuaValue?>, a, c - 1)
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
                        val limit: LuaValue? = stack[a + 1]
                        val step: LuaValue = stack[a + 2]
                        val idx: LuaValue = stack[a].add(step)
                        if (if (step.gt_b(0)) idx.lteq_b((limit)!!) else idx.gteq_b((limit)!!)) {
                            stack[a] = idx
                            stack[a + 3] = idx
                            pc += (i ushr 14) - 0x1ffff
                        }
                        ++pc
                        continue
                    }

                    Lua.OP_FORPREP -> {
                        val init: LuaValue = stack[a].checknumber("'for' initial value must be a number")!!
                        val limit: LuaValue? = stack[a + 1].checknumber("'for' limit must be a number")
                        val step: LuaValue? = stack[a + 2].checknumber("'for' step must be a number")
                        stack[a] = init.sub((step)!!)
                        stack[a + 1] = limit!!
                        stack[a + 2] = step
                        pc += (i ushr 14) - 0x1ffff
                        ++pc
                        continue
                    }

                    Lua.OP_TFORCALL -> {
                        v = stack[a].invokeSuspend((varargsOf(stack[a + 1], stack[a + 2]))!!)
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
                        c = (i shr 14) and 0x1ff
                        if (c == 0) c = code[++pc]
                        val offset: Int = (c - 1) * Lua.LFIELDS_PER_FLUSH
                        o = stack[a]
                        b = i ushr 23
                        if (b == 0) {
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
                        ++pc
                        continue
                    }

                    Lua.OP_CLOSURE -> {
                        val newp: Prototype = p.p!![i ushr 14]!!
                        val ncl: LuaClosure = net.blueva.luak.LuaClosure(newp, globals)
                        val uv: Array<Upvaldesc?> = newp.upvalues!!
                        var j = 0
                        val nup = uv.size
                        while (j < nup) {
                            if (uv[j]!!.instack)  /* upvalue refes to local variable? */
                                ncl.upValues[j] = findupval(stack, uv[j]!!.idx, openups!!)
                            else  /* get upvalue from enclosing function */
                                ncl.upValues[j] = upValues[(uv[j]!!.idx).toInt()]
                            ++j
                        }
                        stack[a] = ncl
                        ++pc
                        continue
                    }

                    Lua.OP_VARARG -> {
                        b = i ushr 23
                        if (b == 0) {
                            b = varargs.narg()
                            top = a + b
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
            if (le.traceback == null) {
                enrichArgError(le, p, pc)
                processErrorHooks(le, p, pc)
            }
            throw le
        } catch (e: Exception) {
            val le: LuaError = LuaError(e)
            processErrorHooks(le, p, pc)
            throw le
        } finally {
            if (openups != null) {
                var u = openups.size
                while (--u >= 0) {
                    if (openups[u] != null) openups[u]!!.close()
                }
            }
            if (globals != null && globals.debuglib != null) globals.debuglib!!.onReturn()
        }
    }

    /**
     * Run the error hook if there is one
     * @param msg the message to use in error hook processing.
     */
    fun errorHook(msg: String?, level: Int): String? {
        if (globals == null) return msg
        val r: LuaThread = globals.running
        if (r.errorfunc == null) return if (globals.debuglib != null) msg.toString() + "\n" + globals.debuglib!!.traceback(
            level
        ) else msg
        val e: LuaValue = r.errorfunc!!
        r.errorfunc = null
        try {
            return e.call(LuaValue.valueOf(msg))!!.tojstring()
        } catch (t: Throwable) {
            return "error in error handling"
        } finally {
            r.errorfunc = e
        }
    }

    /**
     * Enrich a raw "bad argument #N: detail" message (stamped by [Varargs]'
     * argument checkers, which don't know the calling function's name) with
     * that name, matching real Lua's "bad argument #N to 'name' (detail)".
     * Mirrors real Lua's `luaL_argerror`/`getobjname`: [pc] is still the
     * CALL/TAILCALL instruction that invoked the failing callee, since the
     * throw unwound before the loop's `++pc`.
     */
    private fun enrichArgError(le: LuaError, p: Prototype, pc: Int) {
        val m = le.message ?: return
        val match = Regex("^bad argument #(\\d+): ([\\s\\S]*)$").find(m) ?: return
        val code = p.code ?: return
        if (pc < 0 || pc >= code.size) return
        val instr = code[pc]
        val opcode = Lua.GET_OPCODE(instr)
        if (opcode != Lua.OP_CALL && opcode != Lua.OP_TAILCALL) return
        var argIndex = match.groupValues[1].toIntOrNull() ?: return
        val detail = match.groupValues[2]
        val a = Lua.GETARG_A(instr)
        val nw = net.blueva.luak.lib.DebugLib.getobjname(p, pc, a)
        if (nw != null && nw.namewhat == "method") {
            argIndex--
            if (argIndex == 0) {
                le.argMessageOverride = "calling '" + nw.name + "' on bad self (" + detail + ")"
                return
            }
        }
        val funcname = nw?.name ?: "?"
        le.argMessageOverride = "bad argument #" + argIndex + " to '" + funcname + "' (" + detail + ")"
    }

    private fun processErrorHooks(le: LuaError, p: Prototype, pc: Int) {
        var file: String? = "?"
        var line = -1
        run {
            var frame: CallFrame? = null
            if (globals != null && globals.debuglib != null) {
                frame = globals.debuglib!!.getCallFrame(le.level)
                if (frame != null) {
                    val src: String? = frame.shortsource()
                    file = if (src != null) src else "?"
                    line = frame.currentline()
                }
            }
            if (frame == null) {
                file = if (p.source != null) p.source!!.tojstring() else "?"
                line = if (p.lineinfo != null && pc >= 0 && pc < p.lineinfo!!.size) p.lineinfo!![pc] else -1
            }
        }
        le.fileline = file.toString() + ":" + line
        le.traceback = errorHook(le.message, le.level)
    }

    private fun findupval(stack: Array<LuaValue>, idx: Short, openups: Array<UpValue?>): UpValue? {
        val n = openups.size
        for (i in 0..<n) if (openups[i] != null && openups[i]!!.index == idx.toInt()) return openups[i]
        for (i in 0..<n) if (openups[i] == null) {
            val created = UpValue(stack as Array<LuaValue?>, (idx).toInt())
            openups[i] = created
            return created
        }
        error("No space for upvalue")
        return null
    }

    protected fun getUpvalue(i: Int): LuaValue {
        return (upValues[i]!!.getValue())!!
    }

    protected fun setUpvalue(i: Int, v: LuaValue?) {
        upValues[i]!!.setValue(v)
    }

    override fun name(): String {
        return "<" + p.shortsource() + ":" + p.linedefined + ">"
    }


    companion object {
        private val NOUPVALUES: Array<UpValue?> = arrayOfNulls<UpValue>(0)
    }
}
