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
package net.blueva.luak.ast

import net.blueva.luak.ast.Exp.*
import net.blueva.luak.ast.Stat.*

abstract class Visitor {
    fun visit(chunk: Chunk) {
        chunk.block?.accept(this)
    }

    open fun visit(block: Block) {
        visit(block.scope)
        if (block.stats != null) {
            var i = 0
            val n = block.stats.size
            while (i < n) {
                (block.stats[i] as Stat).accept(this)
                i++
            }
        }
    }

    open fun visit(stat: Assign) {
        visitVars(stat.vars)
        visitExps(stat.exps)
    }

    fun visit(breakstat: Stat.Break?) {
    }

    fun visit(stat: FuncCallStat) {
        stat.funccall?.accept(this)
    }

    open fun visit(stat: FuncDef) {
        stat.body?.accept(this)
    }

    open fun visit(stat: GenericFor) {
        visit(stat.scope)
        visitNames(stat.names)
        visitExps(stat.exps)
        stat.block?.accept(this)
    }

    fun visit(stat: IfThenElse) {
        stat.ifexp?.accept(this)
        stat.ifblock?.accept(this)
        if (stat.elseifblocks != null && stat.elseifexps != null) {
            var i = 0
            val n = stat.elseifblocks.size
            while (i < n) {
                (stat.elseifexps[i] as Exp).accept(this)
                (stat.elseifblocks[i] as Block).accept(this)
                i++
            }
        }
        if (stat.elseblock != null) visit(stat.elseblock)
    }

    open fun visit(stat: LocalAssign) {
        visitNames(stat.names)
        visitExps(stat.values)
    }

    open fun visit(stat: LocalFuncDef) {
        visit(stat.name)
        stat.body?.accept(this)
    }

    open fun visit(stat: NumericFor) {
        visit(stat.scope)
        visit(stat.name)
        stat.initial?.accept(this)
        stat.limit?.accept(this)
        stat.step?.accept(this)
        stat.block?.accept(this)
    }

    fun visit(stat: RepeatUntil) {
        stat.block?.accept(this)
        stat.exp?.accept(this)
    }

    fun visit(stat: Stat.Return) {
        visitExps(stat.values)
    }

    fun visit(stat: WhileDo) {
        stat.exp?.accept(this)
        stat.block?.accept(this)
    }

    open fun visit(body: FuncBody) {
        visit(body.scope)
        body.parlist?.accept(this)
        body.block?.accept(this)
    }

    fun visit(args: FuncArgs) {
        visitExps(args.exps)
    }

    fun visit(field: TableField) {
        if (field.name != null) visit(field.name)
        field.index?.accept(this)
        field.rhs?.accept(this)
    }

    open fun visit(exp: AnonFuncDef) {
        exp.body?.accept(this)
    }

    fun visit(exp: BinopExp) {
        exp.lhs?.accept(this)
        exp.rhs?.accept(this)
    }

    fun visit(exp: Exp.Constant?) {
    }

    fun visit(exp: FieldExp) {
        exp.lhs?.accept(this)
        visit(exp.name)
    }

    fun visit(exp: FuncCall) {
        exp.lhs?.accept(this)
        exp.args?.accept(this)
    }

    fun visit(exp: IndexExp) {
        exp.lhs?.accept(this)
        exp.exp?.accept(this)
    }

    fun visit(exp: Exp.MethodCall) {
        exp.lhs?.accept(this)
        visit(exp.name)
        exp.args?.accept(this)
    }

    open fun visit(exp: NameExp) {
        visit(exp.name)
    }

    fun visit(exp: ParensExp) {
        exp.exp?.accept(this)
    }

    fun visit(exp: UnopExp) {
        exp.rhs?.accept(this)
    }

    fun visit(exp: VarargsExp?) {
    }

    open fun visit(pars: ParList) {
        visitNames(pars.names)
    }

    fun visit(table: TableConstructor) {
        val fields = table.fields ?: return
        var i = 0
        val n = fields.size
        while (i < n) {
            (fields[i] as TableField).accept(this)
            i++
        }
    }

    fun visitVars(vars: MutableList<VarExp?>?) {
        if (vars != null) {
            var i = 0
            val n = vars.size
            while (i < n) {
                (vars[i] as VarExp).accept(this)
                i++
            }
        }
    }

    fun visitExps(exps: MutableList<Exp?>?) {
        if (exps != null) {
            var i = 0
            val n = exps.size
            while (i < n) {
                (exps[i] as Exp).accept(this)
                i++
            }
        }
    }

    fun visitNames(names: MutableList<Name?>?) {
        if (names != null) {
            var i = 0
            val n = names.size
            while (i < n) {
                visit(names[i])
                i++
            }
        }
    }

    fun visit(name: Name?) {
    }

    fun visit(name: String?) {
    }

    open fun visit(scope: NameScope?) {
    }

    fun visit(gotostat: Goto?) {
    }

    fun visit(label: Stat.Label?) {
    }
}
