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

import net.blueva.luak.lib.MathLib

/**
 * Extension of [LuaNumber] which can hold a Java int as its value.
 * 
 * 
 * These instance are not instantiated directly by clients, but indirectly
 * via the static functions [LuaValue.valueOf] or [LuaValue.valueOf]
 * functions.  This ensures that policies regarding pooling of instances are
 * encapsulated.
 * 
 * 
 * There are no API's specific to LuaInteger that are useful beyond what is already
 * exposed in [LuaValue].
 * 
 * @see LuaValue
 * 
 * @see LuaNumber
 * 
 * @see LuaDouble
 * 
 * @see LuaValue.valueOf
 * @see LuaValue.valueOf
 */
class LuaInteger
/**
 * Package protected constructor.
 * @see LuaValue.valueOf
 */ internal constructor(
    /** The value being held by this instance.  */
    val v: Int
) : LuaNumber() {
    fun isint(): Boolean {
        return true
    }

    fun isinttype(): Boolean {
        return true
    }

    fun islong(): Boolean {
        return true
    }

    fun tobyte(): Byte {
        return v.toByte()
    }

    fun tochar(): Char {
        return v.toChar()
    }

    fun todouble(): Double {
        return v.toDouble()
    }

    fun tofloat(): Float {
        return v.toFloat()
    }

    fun toint(): Int {
        return v
    }

    fun tolong(): Long {
        return v.toLong()
    }

    fun toshort(): Short {
        return v.toShort()
    }

    fun optdouble(defval: Double): Double {
        return v.toDouble()
    }

    fun optint(defval: Int): Int {
        return v
    }

    fun optinteger(defval: LuaInteger?): LuaInteger {
        return this
    }

    fun optlong(defval: Long): Long {
        return v.toLong()
    }

    fun tojstring(): String {
        return Integer.toString(v)
    }

    fun strvalue(): LuaString {
        return LuaString.valueOf(Integer.toString(v))
    }

    fun optstring(defval: LuaString?): LuaString {
        return LuaString.valueOf(Integer.toString(v))
    }

    fun tostring(): LuaValue {
        return LuaString.valueOf(Integer.toString(v))
    }

    fun optjstring(defval: String?): String {
        return Integer.toString(v)
    }

    fun checkinteger(): LuaInteger {
        return this
    }

    fun isstring(): Boolean {
        return true
    }

    fun hashCode(): Int {
        return v
    }

    // unary operators
    fun neg(): LuaValue? {
        return net.blueva.luak.LuaInteger.Companion.valueOf(-v.toLong())
    }

    // object equality, used for key comparison
    fun equals(o: Object): Boolean {
        return if (o is LuaInteger) o.v == v else false
    }

    // equality w/ metatable processing
    fun eq(`val`: LuaValue): LuaValue {
        return if (`val`.raweq(v)) TRUE else FALSE
    }

    fun eq_b(`val`: LuaValue): Boolean {
        return `val`.raweq(v)
    }

    // equality w/o metatable processing
    fun raweq(`val`: LuaValue): Boolean {
        return `val`.raweq(v)
    }

    fun raweq(`val`: Double): Boolean {
        return v.toDouble() == `val`
    }

    fun raweq(`val`: Int): Boolean {
        return v == `val`
    }

    // arithmetic operators
    fun add(rhs: LuaValue): LuaValue {
        return rhs.add(v)
    }

    fun add(lhs: Double): LuaValue {
        return LuaDouble.valueOf(lhs + v)
    }

    fun add(lhs: Int): LuaValue? {
        return net.blueva.luak.LuaInteger.Companion.valueOf(lhs + v.toLong())
    }

    fun sub(rhs: LuaValue): LuaValue {
        return rhs.subFrom(v)
    }

    fun sub(rhs: Double): LuaValue {
        return LuaDouble.valueOf(v - rhs)
    }

    fun sub(rhs: Int): LuaValue {
        return LuaDouble.valueOf(v - rhs)
    }

    fun subFrom(lhs: Double): LuaValue {
        return LuaDouble.valueOf(lhs - v)
    }

    fun subFrom(lhs: Int): LuaValue? {
        return net.blueva.luak.LuaInteger.Companion.valueOf(lhs - v.toLong())
    }

    fun mul(rhs: LuaValue): LuaValue {
        return rhs.mul(v)
    }

    fun mul(lhs: Double): LuaValue {
        return LuaDouble.valueOf(lhs * v)
    }

    fun mul(lhs: Int): LuaValue? {
        return net.blueva.luak.LuaInteger.Companion.valueOf(lhs * v.toLong())
    }

    fun pow(rhs: LuaValue): LuaValue {
        return rhs.powWith(v)
    }

    fun pow(rhs: Double): LuaValue {
        return MathLib.dpow(v, rhs)
    }

    fun pow(rhs: Int): LuaValue {
        return MathLib.dpow(v, rhs)
    }

    fun powWith(lhs: Double): LuaValue {
        return MathLib.dpow(lhs, v)
    }

    fun powWith(lhs: Int): LuaValue {
        return MathLib.dpow(lhs, v)
    }

    fun div(rhs: LuaValue): LuaValue {
        return rhs.divInto(v)
    }

    fun div(rhs: Double): LuaValue {
        return LuaDouble.ddiv(v, rhs)
    }

    fun div(rhs: Int): LuaValue {
        return LuaDouble.ddiv(v, rhs)
    }

    fun divInto(lhs: Double): LuaValue {
        return LuaDouble.ddiv(lhs, v)
    }

    fun mod(rhs: LuaValue): LuaValue {
        return rhs.modFrom(v)
    }

    fun mod(rhs: Double): LuaValue {
        return LuaDouble.dmod(v, rhs)
    }

    fun mod(rhs: Int): LuaValue {
        return LuaDouble.dmod(v, rhs)
    }

    fun modFrom(lhs: Double): LuaValue {
        return LuaDouble.dmod(lhs, v)
    }

    // relational operators
    fun lt(rhs: LuaValue): LuaValue {
        return if (rhs is LuaNumber) (if (rhs.gt_b(v)) TRUE else FALSE) else super.lt(rhs)
    }

    fun lt(rhs: Double): LuaValue {
        return if (v < rhs) TRUE else FALSE
    }

    fun lt(rhs: Int): LuaValue {
        return if (v < rhs) TRUE else FALSE
    }

    fun lt_b(rhs: LuaValue): Boolean {
        return if (rhs is LuaNumber) rhs.gt_b(v) else super.lt_b(rhs)
    }

    fun lt_b(rhs: Int): Boolean {
        return v < rhs
    }

    fun lt_b(rhs: Double): Boolean {
        return v < rhs
    }

    fun lteq(rhs: LuaValue): LuaValue {
        return if (rhs is LuaNumber) (if (rhs.gteq_b(v)) TRUE else FALSE) else super.lteq(rhs)
    }

    fun lteq(rhs: Double): LuaValue {
        return if (v <= rhs) TRUE else FALSE
    }

    fun lteq(rhs: Int): LuaValue {
        return if (v <= rhs) TRUE else FALSE
    }

    fun lteq_b(rhs: LuaValue): Boolean {
        return if (rhs is LuaNumber) rhs.gteq_b(v) else super.lteq_b(rhs)
    }

    fun lteq_b(rhs: Int): Boolean {
        return v <= rhs
    }

    fun lteq_b(rhs: Double): Boolean {
        return v <= rhs
    }

    fun gt(rhs: LuaValue): LuaValue {
        return if (rhs is LuaNumber) (if (rhs.lt_b(v)) TRUE else FALSE) else super.gt(rhs)
    }

    fun gt(rhs: Double): LuaValue {
        return if (v > rhs) TRUE else FALSE
    }

    fun gt(rhs: Int): LuaValue {
        return if (v > rhs) TRUE else FALSE
    }

    fun gt_b(rhs: LuaValue): Boolean {
        return if (rhs is LuaNumber) rhs.lt_b(v) else super.gt_b(rhs)
    }

    fun gt_b(rhs: Int): Boolean {
        return v > rhs
    }

    fun gt_b(rhs: Double): Boolean {
        return v > rhs
    }

    fun gteq(rhs: LuaValue): LuaValue {
        return if (rhs is LuaNumber) (if (rhs.lteq_b(v)) TRUE else FALSE) else super.gteq(rhs)
    }

    fun gteq(rhs: Double): LuaValue {
        return if (v >= rhs) TRUE else FALSE
    }

    fun gteq(rhs: Int): LuaValue {
        return if (v >= rhs) TRUE else FALSE
    }

    fun gteq_b(rhs: LuaValue): Boolean {
        return if (rhs is LuaNumber) rhs.lteq_b(v) else super.gteq_b(rhs)
    }

    fun gteq_b(rhs: Int): Boolean {
        return v >= rhs
    }

    fun gteq_b(rhs: Double): Boolean {
        return v >= rhs
    }

    // string comparison
    fun strcmp(rhs: LuaString?): Int {
        typerror("attempt to compare number with string")
        return 0
    }

    fun checkint(): Int {
        return v
    }

    fun checklong(): Long {
        return v.toLong()
    }

    fun checkdouble(): Double {
        return v.toDouble()
    }

    fun checkjstring(): String {
        return String.valueOf(v)
    }

    fun checkstring(): LuaString? {
        return valueOf(String.valueOf(v))
    }

    companion object {
        private val intValues = arrayOfNulls<LuaInteger>(512)

        init {
            for (i in 0..511) net.blueva.luak.LuaInteger.Companion.intValues[i] = net.blueva.luak.LuaInteger(i - 256)
        }

        fun valueOf(i: Int): LuaInteger? {
            return if (i <= 255 && i >= -256) net.blueva.luak.LuaInteger.Companion.intValues[i + 256] else net.blueva.luak.LuaInteger(
                i
            )
        } // TODO consider moving this to LuaValue

        /** Return a LuaNumber that represents the value provided
         * @param l long value to represent.
         * @return LuaNumber that is eithe LuaInteger or LuaDouble representing l
         * @see LuaValue.valueOf
         * @see LuaValue.valueOf
         */
        fun valueOf(l: Long): LuaNumber? {
            val i = l.toInt()
            return if (l == i.toLong()) (if (i <= 255 && i >= -256) net.blueva.luak.LuaInteger.Companion.intValues[i + 256] else net.blueva.luak.LuaInteger(
                i
            ) as LuaNumber) else LuaDouble.valueOf(l) as LuaNumber?
        }

        fun hashCode(x: Int): Int {
            return x
        }
    }
}
