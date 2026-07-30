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

class NameScope {
    val namedVariables: MutableMap<String?, Variable?> = HashMap<String?, Variable?>()

    val outerScope: NameScope?

    var functionNestingCount: Int

    /** Construct default names scope  */
    constructor() {
        this.outerScope = null
        this.functionNestingCount = 0
    }

    /** Construct name scope within another scope */
    constructor(outerScope: NameScope?) {
        this.outerScope = outerScope
        this.functionNestingCount = if (outerScope != null) outerScope.functionNestingCount else 0
    }

    /** Look up a name.  If it is a global name, then throw IllegalArgumentException.  */
    @Throws(IllegalArgumentException::class)
    fun find(name: String?): Variable? {
        validateIsNotKeyword(name)
        var n: NameScope? = this
        while (n != null) {
            if (n.namedVariables.containsKey(name)) return n.namedVariables.get(name)
            n = n.outerScope
        }
        val value = Variable(name)
        this.namedVariables.put(name, value)
        return value
    }

    /** Define a name in this scope.  If it is a global name, then throw IllegalArgumentException.  */
    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    fun define(name: String?): Variable {
        validateIsNotKeyword(name)
        val value = Variable(name, this)
        this.namedVariables.put(name, value)
        return value
    }

    private fun validateIsNotKeyword(name: String?) {
        require(!LUA_KEYWORDS.contains(name)) { "name is a keyword: '" + name + "'" }
    }

    companion object {
        private val LUA_KEYWORDS: MutableSet<String?> = HashSet<String?>()

        init {
            val k: Array<String?> = arrayOf(
                "and", "break", "do", "else", "elseif", "end",
                "false", "for", "function", "if", "in", "local",
                "nil", "not", "or", "repeat", "return",
                "then", "true", "until", "while"
            )
            for (i in k.indices) LUA_KEYWORDS.add(k[i])
        }
    }
}
