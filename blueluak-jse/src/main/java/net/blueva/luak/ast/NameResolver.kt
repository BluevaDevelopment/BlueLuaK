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

import net.blueva.luak.LuaValue
import net.blueva.luak.ast.Exp.NameExp
import net.blueva.luak.ast.Exp.VarExp
import net.blueva.luak.ast.Stat.*

/**
 * Visitor that resolves names to scopes.
 * Each Name is resolved to a NamedVarible, possibly in a NameScope
 * if it is a local, or in no named scope if it is a global.
 */
class NameResolver : Visitor() {
    private var scope: NameScope? = null

    private fun pushScope() {
        scope = NameScope(scope)
    }

    private fun popScope() {
        scope = scope!!.outerScope
    }

    override fun visit(scope: NameScope?) {
    }

    override fun visit(block: Block) {
        pushScope()
        block.scope = scope
        super.visit(block)
        popScope()
    }

    override fun visit(body: FuncBody) {
        pushScope()
        scope!!.functionNestingCount++
        body.scope = scope
        super.visit(body)
        popScope()
    }

    override fun visit(stat: LocalFuncDef) {
        defineLocalVar(stat.name)
        super.visit(stat)
    }

    override fun visit(stat: NumericFor) {
        pushScope()
        stat.scope = scope
        defineLocalVar(stat.name)
        super.visit(stat)
        popScope()
    }

    override fun visit(stat: GenericFor) {
        pushScope()
        stat.scope = scope
        stat.names?.let { defineLocalVars(it) }
        super.visit(stat)
        popScope()
    }

    override fun visit(exp: NameExp) {
        exp.name.variable = resolveNameReference(exp.name)
        super.visit(exp)
    }

    override fun visit(stat: FuncDef) {
        stat.name?.let {
            it.name.variable = resolveNameReference(it.name)
            it.name.variable!!.hasassignments = true
        }
        super.visit(stat)
    }

    override fun visit(stat: Assign) {
        super.visit(stat)
        val vars = stat.vars ?: return
        var i = 0
        val n = vars.size
        while (i < n) {
            val v = vars[i] as VarExp
            v.markHasAssignment()
            i++
        }
    }

    override fun visit(stat: LocalAssign) {
        visitExps(stat.values)
        stat.names?.let { defineLocalVars(it) }
        val names = stat.names ?: return
        val values = stat.values
        val n = names.size
        val m = values?.size ?: 0
        val isvarlist = m > 0 && m < n && (values!![m - 1] as Exp).isvarargexp()
        var i = 0
        while (i < n && i < (if (isvarlist) m - 1 else m)) {
            if (values!![i] is Exp.Constant) (names[i] as Name).variable!!.initialValue =
                (values[i] as Exp.Constant).value
            i++
        }
        if (!isvarlist) for (j in m..<n) (names[j] as Name).variable!!.initialValue = LuaValue.NIL
    }

    override fun visit(pars: ParList) {
        if (pars.names != null) defineLocalVars(pars.names)
        if (pars.isvararg) scope!!.define("arg")
        super.visit(pars)
    }

    protected fun defineLocalVars(names: MutableList<Name?>) {
        var i = 0
        val n = names.size
        while (i < n) {
            defineLocalVar(names[i] as Name)
            i++
        }
    }

    protected fun defineLocalVar(name: Name) {
        name.variable = scope!!.define(name.name)
    }

    protected fun resolveNameReference(name: Name): Variable {
        val v = scope!!.find(name.name)!!
        if (v.isLocal && scope!!.functionNestingCount != v.definingScope!!.functionNestingCount) v.isupvalue = true
        return v
    }
}
