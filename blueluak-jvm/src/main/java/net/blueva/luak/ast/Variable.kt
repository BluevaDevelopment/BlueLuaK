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

/** Variable is created lua name scopes, and is a named, lua variable that
 * either refers to a lua local, global, or upvalue storage location.
 */
class Variable {
    /** The name as it appears in lua source code  */
    val name: String?

    /** The lua scope in which this variable is defined.  */
    val definingScope: NameScope?

    /** true if this variable is an upvalue  */
    var isupvalue: Boolean = false

    /** true if there are assignments made to this variable  */
    var hasassignments: Boolean = false

    /** When hasassignments == false, and the initial value is a constant, this is the initial value  */
    var initialValue: LuaValue? = null

    /** Global is named variable not associated with a defining scope  */
    constructor(name: String?) {
        this.name = name
        this.definingScope = null
    }

    constructor(name: String?, definingScope: NameScope?) {
        /** Local variable is defined in a particular scope.   */
        this.name = name
        this.definingScope = definingScope
    }

    val isLocal: Boolean
        get() = this.definingScope != null
    val isConstant: Boolean
        get() = !hasassignments && initialValue != null
}