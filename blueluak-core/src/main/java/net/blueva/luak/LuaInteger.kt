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
    override fun isint(): Boolean {
        return true
    }

    override fun isinttype(): Boolean {
        return true
    }

    override fun islong(): Boolean {
        return true
    }

    override fun tobyte(): Byte {
        return v.toByte()
    }

    override fun tochar(): Char {
        return v.toChar()
    }

    override fun todouble(): Double {
        return v.toDouble()
    }

    override fun tofloat(): Float {
        return v.toFloat()
    }

    override fun toint(): Int {
        return v
    }

    override fun tolong(): Long {
        return v.toLong()
    }

    override fun toshort(): Short {
        return v.toShort()
    }

    override fun optdouble(defval: Double): Double {
        return v.toDouble()
    }

    override fun optint(defval: Int): Int {
        return v
    }

    override fun optinteger(defval: LuaInteger?): LuaInteger {
        return this
    }

    override fun optlong(defval: Long): Long {
        return v.toLong()
    }

    override fun tojstring(): String {
        return Integer.toString(v)
    }

    override fun strvalue(): LuaString {
        return LuaString.valueOf(Integer.toString(v))
    }

    override fun optstring(defval: LuaString?): LuaString {
        return LuaString.valueOf(Integer.toString(v))
    }

    override fun tostring(): LuaValue {
        return LuaString.valueOf(Integer.toString(v))
    }

    override fun optjstring(defval: String?): String {
        return Integer.toString(v)
    }

    override fun checkinteger(): LuaInteger {
        return this
    }

    override fun isstring(): Boolean {
        return true
    }

    override fun hashCode(): Int {
        return v
    }

    // unary operators
    override fun neg(): LuaValue {
        return (net.blueva.luak.LuaInteger.Companion.valueOf(-v.toLong()))!!
    }

    // object equality, used for key comparison
    override fun equals(o: Any?): Boolean {
        return if (o is LuaInteger) o.v == v else false
    }

    // equality w/ metatable processing
    override fun eq(`val`: LuaValue?): LuaValue {
        val `val` = `val`!!
        return (if (`val`.raweq(v)) TRUE else FALSE)!!
    }

    override fun eq_b(`val`: LuaValue?): Boolean {
        val `val` = `val`!!
        return `val`.raweq(v)
    }

    // equality w/o metatable processing
    override fun raweq(`val`: LuaValue?): Boolean {
        val `val` = `val`!!
        return `val`.raweq(v)
    }

    override fun raweq(`val`: Double): Boolean {
        val `val` = `val`!!
        return v.toDouble() == `val`
    }

    override fun raweq(`val`: Int): Boolean {
        val `val` = `val`!!
        return v == `val`
    }

    // arithmetic operators
    override fun add(rhs: LuaValue): LuaValue {
        return rhs.add(v)
    }

    override fun add(lhs: Double): LuaValue {
        return (LuaDouble.valueOf(lhs + v))!!
    }

    override fun add(lhs: Int): LuaValue {
        return (net.blueva.luak.LuaInteger.Companion.valueOf(lhs + v.toLong()))!!
    }

    override fun sub(rhs: LuaValue): LuaValue {
        return rhs.subFrom(v)
    }

    override fun sub(rhs: Double): LuaValue {
        return (LuaDouble.valueOf(v - rhs))!!
    }

    override fun sub(rhs: Int): LuaValue {
        return (LuaDouble.valueOf((v - rhs).toDouble()))!!
    }

    override fun subFrom(lhs: Double): LuaValue {
        return (LuaDouble.valueOf(lhs - v))!!
    }

    override fun subFrom(lhs: Int): LuaValue {
        return (net.blueva.luak.LuaInteger.Companion.valueOf(lhs - v.toLong()))!!
    }

    override fun mul(rhs: LuaValue): LuaValue {
        return rhs.mul(v)
    }

    override fun mul(lhs: Double): LuaValue {
        return (LuaDouble.valueOf(lhs * v))!!
    }

    override fun mul(lhs: Int): LuaValue {
        return (net.blueva.luak.LuaInteger.Companion.valueOf(lhs * v.toLong()))!!
    }

    override fun pow(rhs: LuaValue): LuaValue {
        return rhs.powWith(v)
    }

    override fun pow(rhs: Double): LuaValue {
        return MathLib.dpow((v).toDouble(), rhs)
    }

    override fun pow(rhs: Int): LuaValue {
        return MathLib.dpow((v).toDouble(), (rhs).toDouble())
    }

    override fun powWith(lhs: Double): LuaValue {
        return MathLib.dpow(lhs, (v).toDouble())
    }

    override fun powWith(lhs: Int): LuaValue {
        return MathLib.dpow((lhs).toDouble(), (v).toDouble())
    }

    override fun div(rhs: LuaValue): LuaValue {
        return rhs.divInto((v).toDouble())
    }

    override fun div(rhs: Double): LuaValue {
        return (LuaDouble.ddiv((v).toDouble(), rhs))!!
    }

    override fun div(rhs: Int): LuaValue {
        return (LuaDouble.ddiv((v).toDouble(), (rhs).toDouble()))!!
    }

    override fun divInto(lhs: Double): LuaValue {
        return (LuaDouble.ddiv(lhs, (v).toDouble()))!!
    }

    override fun mod(rhs: LuaValue): LuaValue {
        return rhs.modFrom((v).toDouble())
    }

    override fun mod(rhs: Double): LuaValue {
        return (LuaDouble.dmod((v).toDouble(), rhs))!!
    }

    override fun mod(rhs: Int): LuaValue {
        return (LuaDouble.dmod((v).toDouble(), (rhs).toDouble()))!!
    }

    override fun modFrom(lhs: Double): LuaValue {
        return (LuaDouble.dmod(lhs, (v).toDouble()))!!
    }

    // relational operators
    override fun lt(rhs: LuaValue): LuaValue {
        return (if (rhs is LuaNumber) (if (rhs.gt_b(v)) TRUE else FALSE) else super.lt(rhs))!!
    }

    override fun lt(rhs: Double): LuaValue {
        return (if (v < rhs) TRUE else FALSE)!!
    }

    override fun lt(rhs: Int): LuaValue {
        return (if (v < rhs) TRUE else FALSE)!!
    }

    override fun lt_b(rhs: LuaValue): Boolean {
        return if (rhs is LuaNumber) rhs.gt_b(v) else super.lt_b(rhs)
    }

    override fun lt_b(rhs: Int): Boolean {
        return v < rhs
    }

    override fun lt_b(rhs: Double): Boolean {
        return v < rhs
    }

    override fun lteq(rhs: LuaValue): LuaValue {
        return (if (rhs is LuaNumber) (if (rhs.gteq_b(v)) TRUE else FALSE) else super.lteq(rhs))!!
    }

    override fun lteq(rhs: Double): LuaValue {
        return (if (v <= rhs) TRUE else FALSE)!!
    }

    override fun lteq(rhs: Int): LuaValue {
        return (if (v <= rhs) TRUE else FALSE)!!
    }

    override fun lteq_b(rhs: LuaValue): Boolean {
        return if (rhs is LuaNumber) rhs.gteq_b(v) else super.lteq_b(rhs)
    }

    override fun lteq_b(rhs: Int): Boolean {
        return v <= rhs
    }

    override fun lteq_b(rhs: Double): Boolean {
        return v <= rhs
    }

    override fun gt(rhs: LuaValue): LuaValue {
        return (if (rhs is LuaNumber) (if (rhs.lt_b(v)) TRUE else FALSE) else super.gt(rhs))!!
    }

    override fun gt(rhs: Double): LuaValue {
        return (if (v > rhs) TRUE else FALSE)!!
    }

    override fun gt(rhs: Int): LuaValue {
        return (if (v > rhs) TRUE else FALSE)!!
    }

    override fun gt_b(rhs: LuaValue): Boolean {
        return if (rhs is LuaNumber) rhs.lt_b(v) else super.gt_b(rhs)
    }

    override fun gt_b(rhs: Int): Boolean {
        return v > rhs
    }

    override fun gt_b(rhs: Double): Boolean {
        return v > rhs
    }

    override fun gteq(rhs: LuaValue): LuaValue {
        return (if (rhs is LuaNumber) (if (rhs.lteq_b(v)) TRUE else FALSE) else super.gteq(rhs))!!
    }

    override fun gteq(rhs: Double): LuaValue {
        return (if (v >= rhs) TRUE else FALSE)!!
    }

    override fun gteq(rhs: Int): LuaValue {
        return (if (v >= rhs) TRUE else FALSE)!!
    }

    override fun gteq_b(rhs: LuaValue): Boolean {
        return if (rhs is LuaNumber) rhs.lteq_b(v) else super.gteq_b(rhs)
    }

    override fun gteq_b(rhs: Int): Boolean {
        return v >= rhs
    }

    override fun gteq_b(rhs: Double): Boolean {
        return v >= rhs
    }

    // string comparison
    override fun strcmp(rhs: LuaString?): Int {
        typerror("attempt to compare number with string")
        return 0
    }

    override fun checkint(): Int {
        return v
    }

    override fun checklong(): Long {
        return v.toLong()
    }

    override fun checkdouble(): Double {
        return v.toDouble()
    }

    override fun checkjstring(): String {
        return (v).toString()
    }

    override fun checkstring(): LuaString? {
        return valueOf((v).toString())
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
            ) as LuaNumber) else LuaDouble.valueOf((l).toDouble()) as LuaNumber?
        }

        fun hashCode(x: Int): Int {
            return x
        }
    }
}
