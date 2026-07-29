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

class TableField(val index: Exp?, val name: String?, val rhs: Exp?) : SyntaxElement() {
    fun accept(visitor: Visitor) {
        visitor.visit(this)
    }

    companion object {
        fun keyedField(index: Exp?, rhs: Exp?): TableField {
            return TableField(index, null, rhs)
        }

        fun namedField(name: String?, rhs: Exp?): TableField {
            return TableField(null, name, rhs)
        }

        fun listField(rhs: Exp?): TableField {
            return TableField(null, null, rhs)
        }
    }
}
