package net.blueva.luak.parser

import net.blueva.luak.Lua
import net.blueva.luak.LuaValue
import net.blueva.luak.ast.Block
import net.blueva.luak.ast.Chunk
import net.blueva.luak.ast.Exp
import net.blueva.luak.ast.FuncArgs
import net.blueva.luak.ast.FuncBody
import net.blueva.luak.ast.FuncName
import net.blueva.luak.ast.Name
import net.blueva.luak.ast.ParList
import net.blueva.luak.ast.Stat
import net.blueva.luak.ast.Str
import net.blueva.luak.ast.SyntaxElement
import net.blueva.luak.ast.TableConstructor
import net.blueva.luak.ast.TableField
import net.blueva.luak.parser.antlr.LuaParser
import org.antlr.v4.kotlinruntime.ParserRuleContext

internal class LuaAstBuilder {
    fun chunk(ctx: LuaParser.ChunkContext): Chunk =
        located(Chunk(block(ctx.block())), ctx)

    private fun block(ctx: LuaParser.BlockContext): Block {
        val result = Block()
        ctx.stat().mapNotNullTo(result.stats, ::stat)
        ctx.retstat()?.let { result.add(retstat(it)) }
        return located(result, ctx)
    }

    private fun stat(ctx: LuaParser.StatContext): Stat? {
        ctx.SEMI()?.let { return null }
        ctx.label()?.let { return located(Stat.labelstat(it.NAME().text), ctx) }
        ctx.BREAK()?.let { return located(Stat.breakstat(), ctx) }
        ctx.GOTO()?.let { return located(Stat.gotostat(ctx.NAME()!!.text), ctx) }
        ctx.WHILE()?.let {
            return located(Stat.whiledo(exp(ctx.exp(0)!!), block(ctx.block(0)!!)), ctx)
        }
        ctx.REPEAT()?.let {
            return located(Stat.repeatuntil(block(ctx.block(0)!!), exp(ctx.exp(0)!!)), ctx)
        }
        ctx.IF()?.let {
            val expressions = ctx.exp().map(::exp)
            val blocks = ctx.block().map(::block)
            val elseifCount = ctx.ELSEIF().size
            return located(
                Stat.ifthenelse(
                    expressions.first(),
                    blocks.first(),
                    expressions.drop(1).take(elseifCount).toMutableList(),
                    blocks.drop(1).take(elseifCount).toMutableList(),
                    if (ctx.ELSE() != null) blocks.last() else null,
                ),
                ctx,
            )
        }
        ctx.FOR()?.let {
            if (ctx.ASSIGN() != null) {
                val expressions = ctx.exp().map(::exp)
                return located(
                    Stat.fornumeric(
                        ctx.NAME()!!.text,
                        expressions[0],
                        expressions[1],
                        expressions.getOrNull(2),
                        block(ctx.block(0)!!),
                    ),
                    ctx,
                )
            }
            return located(
                Stat.forgeneric(
                    namelist(ctx.namelist()!!),
                    explist(ctx.explist()!!),
                    block(ctx.block(0)!!),
                ),
                ctx,
            )
        }
        ctx.FUNCTION()?.let {
            if (ctx.LOCAL() != null) {
                return located(
                    Stat.localfunctiondef(ctx.NAME()!!.text, funcbody(ctx.funcbody()!!)),
                    ctx,
                )
            }
            return located(
                Stat.functiondef(funcname(ctx.funcname()!!), funcbody(ctx.funcbody()!!)),
                ctx,
            )
        }
        ctx.LOCAL()?.let {
            return located(
                Stat.localassignment(
                    namelist(ctx.namelist()!!),
                    ctx.explist()?.let(::explist),
                ),
                ctx,
            )
        }
        ctx.varlist()?.let {
            return located(
                Stat.assignment(
                    it.variable().map(::variable).toMutableList(),
                    explist(ctx.explist()!!),
                ),
                ctx,
            )
        }
        ctx.functioncall()?.let {
            return located(Stat.functioncall(functioncall(it)), ctx)
        }
        ctx.DO()?.let {
            return located(block(ctx.block(0)!!), ctx)
        }
        throw ParseException("Unsupported statement at ${ctx.start?.line}:${ctx.start?.charPositionInLine}")
    }

    private fun retstat(ctx: LuaParser.RetstatContext): Stat =
        located(Stat.returnstat(ctx.explist()?.let(::explist)), ctx)

    private fun funcname(ctx: LuaParser.FuncnameContext): FuncName {
        val names = ctx.NAME()
        val result = FuncName(names.first().text)
        val dotCount = ctx.DOT().size
        names.drop(1).take(dotCount).forEach { result.adddot(it.text) }
        if (ctx.COLON() != null) result.method = names.last().text
        return located(result, ctx)
    }

    private fun namelist(ctx: LuaParser.NamelistContext): MutableList<Name?> =
        ctx.NAME().map { Name(it.text) }.toMutableList()

    private fun explist(ctx: LuaParser.ExplistContext): MutableList<Exp?> =
        ctx.exp().map(::exp).toMutableList()

    private fun exp(ctx: LuaParser.ExpContext): Exp = orExp(ctx.orExp())

    private fun orExp(ctx: LuaParser.OrExpContext): Exp =
        foldBinary(ctx.andExp().map(::andExp), List(ctx.OR().size) { Lua.OP_OR })

    private fun andExp(ctx: LuaParser.AndExpContext): Exp =
        foldBinary(ctx.compareExp().map(::compareExp), List(ctx.AND().size) { Lua.OP_AND })

    private fun compareExp(ctx: LuaParser.CompareExpContext): Exp {
        val values = ctx.concatExp().map(::concatExp)
        val operators = (1 until ctx.childCount step 2).map { binaryOperator(ctx.getChild(it)!!.text) }
        return foldBinary(values, operators)
    }

    private fun concatExp(ctx: LuaParser.ConcatExpContext): Exp {
        val lhs = addExp(ctx.addExp())
        val rhs = ctx.concatExp()?.let(::concatExp) ?: return lhs
        return Exp.binaryexp(lhs, Lua.OP_CONCAT, rhs)!!
    }

    private fun addExp(ctx: LuaParser.AddExpContext): Exp {
        val values = ctx.multiplyExp().map(::multiplyExp)
        val operators = (1 until ctx.childCount step 2).map { binaryOperator(ctx.getChild(it)!!.text) }
        return foldBinary(values, operators)
    }

    private fun multiplyExp(ctx: LuaParser.MultiplyExpContext): Exp {
        val values = ctx.unaryExp().map(::unaryExp)
        val operators = (1 until ctx.childCount step 2).map { binaryOperator(ctx.getChild(it)!!.text) }
        return foldBinary(values, operators)
    }

    private fun unaryExp(ctx: LuaParser.UnaryExpContext): Exp {
        ctx.powerExp()?.let { return powerExp(it) }
        return Exp.unaryexp(unaryOperator(ctx.getChild(0)!!.text), unaryExp(ctx.unaryExp()!!))!!
    }

    private fun powerExp(ctx: LuaParser.PowerExpContext): Exp {
        val lhs = simpleexp(ctx.simpleexp())
        val rhs = ctx.unaryExp()?.let(::unaryExp) ?: return lhs
        return Exp.binaryexp(lhs, Lua.OP_POW, rhs)!!
    }

    private fun simpleexp(ctx: LuaParser.SimpleexpContext): Exp {
        ctx.NIL()?.let { return located(Exp.constant(LuaValue.NIL), ctx) }
        ctx.FALSE()?.let { return located(Exp.constant(LuaValue.FALSE), ctx) }
        ctx.TRUE()?.let { return located(Exp.constant(LuaValue.TRUE), ctx) }
        ctx.NUMBER()?.let { return located(Exp.numberconstant(it.text), ctx) }
        ctx.string()?.let { return located(Exp.constant(string(it)), ctx) }
        ctx.ELLIPSIS()?.let { return located(Exp.varargs(), ctx) }
        ctx.functiondef()?.let {
            return located(Exp.anonymousfunction(funcbody(it.funcbody())), ctx)
        }
        ctx.prefixexp()?.let { return prefixexp(it) }
        ctx.tableconstructor()?.let { return tableconstructor(it) }
        throw ParseException("Unsupported expression at ${ctx.start?.line}:${ctx.start?.charPositionInLine}")
    }

    private fun prefixexp(ctx: LuaParser.PrefixexpContext): Exp.PrimaryExp {
        var result = initialPrimary(ctx.NAME()?.text, ctx.exp())
        ctx.postfix().forEach { result = postfix(result, it) }
        return located(result, ctx)
    }

    private fun functioncall(ctx: LuaParser.FunctioncallContext): Exp.FuncCall {
        var result = initialPrimary(ctx.NAME()?.text, ctx.exp())
        ctx.postfix().forEach { result = postfix(result, it) }
        result = callpostfix(result, ctx.callpostfix())
        return result as? Exp.FuncCall
            ?: throw ParseException("expected function call")
    }

    private fun variable(ctx: LuaParser.VariableContext): Exp.VarExp {
        if (ctx.NAME().size == 1 && ctx.postfix().isEmpty() && ctx.exp().isEmpty()) {
            return located(Exp.nameprefix(ctx.NAME(0)!!.text), ctx)
        }
        var result = initialPrimary(ctx.NAME(0)?.text, ctx.exp().firstOrNull())
        ctx.postfix().forEach { result = postfix(result, it) }
        result = if (ctx.LBRACK() != null) {
            Exp.indexop(result, exp(ctx.exp().last()))
        } else {
            Exp.fieldop(result, ctx.NAME().last().text)
        }
        return located(
            result as? Exp.VarExp ?: throw ParseException("expected variable"),
            ctx,
        )
    }

    private fun initialPrimary(name: String?, expression: LuaParser.ExpContext?): Exp.PrimaryExp =
        if (name != null) Exp.nameprefix(name) else Exp.parensprefix(exp(expression!!))

    private fun postfix(lhs: Exp.PrimaryExp, ctx: LuaParser.PostfixContext): Exp.PrimaryExp =
        when {
            ctx.LBRACK() != null -> Exp.indexop(lhs, exp(ctx.exp()!!))
            ctx.DOT() != null -> Exp.fieldop(lhs, ctx.NAME()!!.text)
            ctx.COLON() != null -> Exp.methodop(lhs, ctx.NAME()!!.text, args(ctx.args()!!))
            else -> Exp.functionop(lhs, args(ctx.args()!!))
        }

    private fun callpostfix(
        lhs: Exp.PrimaryExp,
        ctx: LuaParser.CallpostfixContext,
    ): Exp.PrimaryExp =
        if (ctx.COLON() != null) {
            Exp.methodop(lhs, ctx.NAME()!!.text, args(ctx.args()))
        } else {
            Exp.functionop(lhs, args(ctx.args()))
        }

    private fun args(ctx: LuaParser.ArgsContext): FuncArgs {
        val result = when {
            ctx.LPAREN() != null -> FuncArgs.explist(ctx.explist()?.let(::explist))
            ctx.tableconstructor() != null -> FuncArgs.tableconstructor(tableconstructor(ctx.tableconstructor()!!))
            else -> FuncArgs.string(string(ctx.string()!!))
        }
        return located(result, ctx)
    }

    private fun funcbody(ctx: LuaParser.FuncbodyContext): FuncBody =
        located(FuncBody(ctx.parlist()?.let(::parlist), block(ctx.block())), ctx)

    private fun parlist(ctx: LuaParser.ParlistContext): ParList =
        located(
            ParList(
                ctx.namelist()?.let(::namelist),
                ctx.ELLIPSIS() != null,
            ),
            ctx,
        )

    private fun tableconstructor(ctx: LuaParser.TableconstructorContext): TableConstructor {
        val result = TableConstructor()
        result.fields = ctx.fieldlist()?.field()?.map(::field)?.toMutableList()
        return located(result, ctx)
    }

    private fun field(ctx: LuaParser.FieldContext): TableField {
        val expressions = ctx.exp()
        val result = when {
            ctx.LBRACK() != null -> TableField.keyedField(exp(expressions[0]), exp(expressions[1]))
            ctx.NAME() != null -> TableField.namedField(ctx.NAME()!!.text, exp(expressions[0]))
            else -> TableField.listField(exp(expressions[0]))
        }
        return located(result, ctx)
    }

    private fun string(ctx: LuaParser.StringContext) =
        when {
            ctx.NORMAL_STRING() != null -> Str.quoteString(ctx.text)
            ctx.CHAR_STRING() != null -> Str.charString(ctx.text)
            else -> Str.longString(ctx.text)
        }

    private fun foldBinary(values: List<Exp>, operators: List<Int>): Exp {
        var result = values.first()
        operators.forEachIndexed { index, operator ->
            result = Exp.binaryexp(result, operator, values[index + 1])!!
        }
        return result
    }

    private fun binaryOperator(operator: String): Int =
        when (operator) {
            "+" -> Lua.OP_ADD
            "-" -> Lua.OP_SUB
            "*" -> Lua.OP_MUL
            "/" -> Lua.OP_DIV
            "%" -> Lua.OP_MOD
            "^" -> Lua.OP_POW
            ".." -> Lua.OP_CONCAT
            "<" -> Lua.OP_LT
            "<=" -> Lua.OP_LE
            ">" -> Lua.OP_GT
            ">=" -> Lua.OP_GE
            "==" -> Lua.OP_EQ
            "~=" -> Lua.OP_NEQ
            "and" -> Lua.OP_AND
            "or" -> Lua.OP_OR
            else -> throw ParseException("Unknown binary operator: $operator")
        }

    private fun unaryOperator(operator: String): Int =
        when (operator) {
            "-" -> Lua.OP_UNM
            "not" -> Lua.OP_NOT
            "#" -> Lua.OP_LEN
            else -> throw ParseException("Unknown unary operator: $operator")
        }

    private fun <T : SyntaxElement> located(element: T, ctx: ParserRuleContext): T {
        val start = ctx.start
        val stop = ctx.stop
        element.beginLine = start?.line ?: 0
        element.beginColumn = ((start?.charPositionInLine ?: 0) + 1).toShort()
        element.endLine = stop?.line ?: element.beginLine
        element.endColumn = ((stop?.charPositionInLine ?: 0) + (stop?.text?.length ?: 0)).toShort()
        return element
    }
}
