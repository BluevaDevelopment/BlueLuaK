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

import net.blueva.luak.Lua
import net.blueva.luak.LuaValue

abstract
class Exp : SyntaxElement() {
    abstract fun accept(visitor: Visitor?)

    open fun isvarexp(): Boolean {
        return false
    }

    open fun isfunccall(): Boolean {
        return false
    }

    open fun isvarargexp(): Boolean {
        return false
    }

    abstract class PrimaryExp : Exp() {
        override fun isvarexp(): Boolean {
            return false
        }

        override fun isfunccall(): Boolean {
            return false
        }
    }

    abstract class VarExp : PrimaryExp() {
        override fun isvarexp(): Boolean {
            return true
        }

        open fun markHasAssignment() {
        }
    }

    class NameExp(name: String?) : VarExp() {
        val name: Name

        init {
            this.name = Name(name)
        }

        override fun markHasAssignment() {
            name.variable!!.hasassignments = true
        }

        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }
    }

    class ParensExp(val exp: Exp?) : PrimaryExp() {
        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }
    }

    class FieldExp(val lhs: PrimaryExp?, name: String?) : VarExp() {
        val name: Name

        init {
            this.name = Name(name)
        }

        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }
    }

    class IndexExp(val lhs: PrimaryExp?, val exp: Exp?) : VarExp() {
        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }
    }

    open class FuncCall(val lhs: PrimaryExp?, val args: FuncArgs?) : PrimaryExp() {
        override fun isfunccall(): Boolean {
            return true
        }

        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }

        override fun isvarargexp(): Boolean {
            return true
        }
    }

    class MethodCall(lhs: PrimaryExp?, val name: String, args: FuncArgs?) : FuncCall(lhs, args) {
        override fun isfunccall(): Boolean {
            return true
        }

        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }
    }

    class Constant(val value: LuaValue?) : Exp() {
        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }
    }

    class VarargsExp : Exp() {
        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }

        override fun isvarargexp(): Boolean {
            return true
        }
    }

    class UnopExp(val op: Int, val rhs: Exp?) : Exp() {
        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }
    }

    class BinopExp(val lhs: Exp?, val op: Int, val rhs: Exp?) : Exp() {
        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }
    }

    class AnonFuncDef(val body: FuncBody?) : Exp() {
        override fun accept(visitor: Visitor?) {
            visitor?.visit(this)
        }
    }

    companion object {
                fun constant(value: LuaValue?): Exp {
            return Constant(value)
        }

                fun numberconstant(token: String?): Exp {
            return Constant(LuaValue.valueOf(token).tonumber())
        }

                fun varargs(): Exp {
            return VarargsExp()
        }

                fun tableconstructor(tc: TableConstructor?): Exp? {
            return tc
        }

                fun unaryexp(op: Int, rhs: Exp?): Exp? {
            if (rhs is BinopExp) {
                val b = rhs
                if (precedence(op) > precedence(b.op)) return binaryexp(unaryexp(op, b.lhs), b.op, b.rhs)
            }
            return UnopExp(op, rhs)
        }

                fun binaryexp(lhs: Exp?, op: Int, rhs: Exp?): Exp? {
            if (lhs is UnopExp) {
                val u = lhs
                if (precedence(op) > precedence(u.op)) return unaryexp(u.op, binaryexp(u.rhs, op, rhs))
            }
            // TODO: cumulate string concatenations together
            // TODO: constant folding
            if (lhs is BinopExp) {
                val b = lhs
                if ((precedence(op) > precedence(b.op)) ||
                    ((precedence(op) == precedence(b.op)) && isrightassoc(op))
                ) return binaryexp(b.lhs, b.op, binaryexp(b.rhs, op, rhs))
            }
            if (rhs is BinopExp) {
                val b = rhs
                if ((precedence(op) > precedence(b.op)) ||
                    ((precedence(op) == precedence(b.op)) && !isrightassoc(op))
                ) return binaryexp(binaryexp(lhs, op, b.lhs), b.op, b.rhs)
            }
            return BinopExp(lhs, op, rhs)
        }

                fun isrightassoc(op: Int): Boolean {
            when (op) {
                Lua.OP_CONCAT, Lua.OP_POW -> return true
                else -> return false
            }
        }

                fun precedence(op: Int): Int {
            when (op) {
                Lua.OP_OR -> return 0
                Lua.OP_AND -> return 1
                Lua.OP_LT, Lua.OP_GT, Lua.OP_LE, Lua.OP_GE, Lua.OP_NEQ, Lua.OP_EQ -> return 2
                Lua.OP_CONCAT -> return 3
                Lua.OP_ADD, Lua.OP_SUB -> return 4
                Lua.OP_MUL, Lua.OP_DIV, Lua.OP_MOD -> return 5
                Lua.OP_NOT, Lua.OP_UNM, Lua.OP_LEN -> return 6
                Lua.OP_POW -> return 7
                else -> throw IllegalStateException("precedence of bad op " + op)
            }
        }

                fun anonymousfunction(funcbody: FuncBody?): Exp {
            return AnonFuncDef(funcbody)
        }

        /** foo  */
                fun nameprefix(name: String?): NameExp {
            return NameExp(name)
        }

        /** ( foo.bar )  */
                fun parensprefix(exp: Exp?): ParensExp {
            return ParensExp(exp)
        }

        /** foo[exp]  */
                fun indexop(lhs: PrimaryExp?, exp: Exp?): IndexExp {
            return IndexExp(lhs, exp)
        }

        /** foo.bar  */
                fun fieldop(lhs: PrimaryExp?, name: String?): FieldExp {
            return FieldExp(lhs, name)
        }

        /** foo(2,3)  */
                fun functionop(lhs: PrimaryExp?, args: FuncArgs?): FuncCall {
            return FuncCall(lhs, args)
        }

        /** foo:bar(4,5)  */
                fun methodop(lhs: PrimaryExp?, name: String, args: FuncArgs?): MethodCall {
            return MethodCall(lhs, name, args)
        }
    }
}
