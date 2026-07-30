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
 * Extension of [LuaNumber] which can hold a Java double as its value.
 * 
 * 
 * These instance are not instantiated directly by clients, but indirectly
 * via the static functions [LuaValue.valueOf] or [LuaValue.valueOf]
 * functions.  This ensures that values which can be represented as int
 * are wrapped in [LuaInteger] instead of [LuaDouble].
 * 
 * 
 * Almost all API's implemented in LuaDouble are defined and documented in [LuaValue].
 * 
 * 
 * However the constants [.NAN], [.POSINF], [.NEGINF],
 * [.JSTR_NAN], [.JSTR_POSINF], and [.JSTR_NEGINF] may be useful
 * when dealing with Nan or Infinite values.
 * 
 * 
 * LuaDouble also defines functions for handling the unique math rules of lua devision and modulo in
 * 
 *  * [.ddiv]
 *  * [.ddiv_d]
 *  * [.dmod]
 *  * [.dmod_d]
 * 
 * 
 * 
 * @see LuaValue
 * 
 * @see LuaNumber
 * 
 * @see LuaInteger
 * 
 * @see LuaValue.valueOf
 * @see LuaValue.valueOf
 */
class LuaDouble
/** Don't allow ints to be boxed by DoubleValues   */ private constructor(
    /** The value being held by this instance.  */
    val v: Double
) : LuaNumber() {
    override fun hashCode(): Int {
        val l: Long = (v + 1).toBits()
        return ((l shr 32).toInt()) + l.toInt()
    }

    override fun islong(): Boolean {
        return v == v.toLong().toDouble()
    }

    override fun tobyte(): Byte {
        return v.toLong().toByte()
    }

    override fun tochar(): Char {
        return Char(v.toLong().toUShort())
    }

    override fun todouble(): Double {
        return v
    }

    override fun tofloat(): Float {
        return v.toFloat()
    }

    override fun toint(): Int {
        return v.toLong().toInt()
    }

    override fun tolong(): Long {
        return v.toLong()
    }

    override fun toshort(): Short {
        return v.toLong().toShort()
    }

    override fun optdouble(defval: Double): Double {
        return v
    }

    override fun optint(defval: Int): Int {
        return v.toLong().toInt()
    }

    override fun optinteger(defval: LuaInteger?): LuaInteger {
        return LuaInteger.valueOf(v.toLong().toInt())
    }

    override fun optlong(defval: Long): Long {
        return v.toLong()
    }

    override fun checkinteger(): LuaInteger {
        return LuaInteger.valueOf(v.toLong().toInt())
    }

    // unary operators
    override fun neg(): LuaValue? {
        return net.blueva.luak.LuaDouble.Companion.valueOf(-v)
    }

    // object equality, used for key comparison
    fun equals(o: Object): Boolean {
        return if (o is LuaDouble) o.v == v else false
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

    override fun raweq(`val`: Double): Boolean {
        return v == `val`
    }

    override fun raweq(`val`: Int): Boolean {
        return v == `val`.toDouble()
    }

    // basic binary arithmetic
    override fun add(rhs: LuaValue): LuaValue {
        return rhs.add(v)
    }

    override fun add(lhs: Double): LuaValue? {
        return net.blueva.luak.LuaDouble.Companion.valueOf(lhs + v)
    }

    override fun sub(rhs: LuaValue): LuaValue {
        return rhs.subFrom(v)
    }

    override fun sub(rhs: Double): LuaValue? {
        return net.blueva.luak.LuaDouble.Companion.valueOf(v - rhs)
    }

    override fun sub(rhs: Int): LuaValue? {
        return net.blueva.luak.LuaDouble.Companion.valueOf(v - rhs)
    }

    override fun subFrom(lhs: Double): LuaValue? {
        return net.blueva.luak.LuaDouble.Companion.valueOf(lhs - v)
    }

    override fun mul(rhs: LuaValue): LuaValue {
        return rhs.mul(v)
    }

    override fun mul(lhs: Double): LuaValue? {
        return net.blueva.luak.LuaDouble.Companion.valueOf(lhs * v)
    }

    override fun mul(lhs: Int): LuaValue? {
        return net.blueva.luak.LuaDouble.Companion.valueOf(lhs * v)
    }

    override fun pow(rhs: LuaValue): LuaValue {
        return rhs.powWith(v)
    }

    override fun pow(rhs: Double): LuaValue {
        return MathLib.dpow(v, rhs)
    }

    override fun pow(rhs: Int): LuaValue {
        return MathLib.dpow(v, rhs)
    }

    override fun powWith(lhs: Double): LuaValue {
        return MathLib.dpow(lhs, v)
    }

    override fun powWith(lhs: Int): LuaValue {
        return MathLib.dpow(lhs, v)
    }

    override fun div(rhs: LuaValue): LuaValue {
        return rhs.divInto(v)
    }

    override fun div(rhs: Double): LuaValue? {
        return net.blueva.luak.LuaDouble.Companion.ddiv(v, rhs)
    }

    override fun div(rhs: Int): LuaValue? {
        return net.blueva.luak.LuaDouble.Companion.ddiv(v, rhs.toDouble())
    }

    override fun divInto(lhs: Double): LuaValue? {
        return net.blueva.luak.LuaDouble.Companion.ddiv(lhs, v)
    }

    override fun mod(rhs: LuaValue): LuaValue {
        return rhs.modFrom(v)
    }

    override fun mod(rhs: Double): LuaValue? {
        return net.blueva.luak.LuaDouble.Companion.dmod(v, rhs)
    }

    override fun mod(rhs: Int): LuaValue? {
        return net.blueva.luak.LuaDouble.Companion.dmod(v, rhs.toDouble())
    }

    override fun modFrom(lhs: Double): LuaValue? {
        return net.blueva.luak.LuaDouble.Companion.dmod(lhs, v)
    }


    // relational operators
    override fun lt(rhs: LuaValue): LuaValue {
        return if (rhs is LuaNumber) (if (rhs.gt_b(v)) TRUE else FALSE) else super.lt(rhs)
    }

    override fun lt(rhs: Double): LuaValue {
        return if (v < rhs) TRUE else FALSE
    }

    override fun lt(rhs: Int): LuaValue {
        return if (v < rhs) TRUE else FALSE
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
        return if (rhs is LuaNumber) (if (rhs.gteq_b(v)) TRUE else FALSE) else super.lteq(rhs)
    }

    override fun lteq(rhs: Double): LuaValue {
        return if (v <= rhs) TRUE else FALSE
    }

    override fun lteq(rhs: Int): LuaValue {
        return if (v <= rhs) TRUE else FALSE
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
        return if (rhs is LuaNumber) (if (rhs.lt_b(v)) TRUE else FALSE) else super.gt(rhs)
    }

    override fun gt(rhs: Double): LuaValue {
        return if (v > rhs) TRUE else FALSE
    }

    override fun gt(rhs: Int): LuaValue {
        return if (v > rhs) TRUE else FALSE
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
        return if (rhs is LuaNumber) (if (rhs.lteq_b(v)) TRUE else FALSE) else super.gteq(rhs)
    }

    override fun gteq(rhs: Double): LuaValue {
        return if (v >= rhs) TRUE else FALSE
    }

    override fun gteq(rhs: Int): LuaValue {
        return if (v >= rhs) TRUE else FALSE
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

    override fun tojstring(): String? {
        /*
		if ( v == 0.0 ) { // never occurs in J2me
			long bits = ( v ).toBits();
			return ( bits >> 63 == 0 ) ? "0" : "-0";
		}
		*/
        val l = v.toLong()
        if (l.toDouble() == v) return Long.toString(l)
        if ((v).isNaN()) return net.blueva.luak.LuaDouble.Companion.JSTR_NAN
        if ((v).isInfinite()) return (if (v < 0) net.blueva.luak.LuaDouble.Companion.JSTR_NEGINF else net.blueva.luak.LuaDouble.Companion.JSTR_POSINF)
        return Float.toString(v.toFloat())
    }

    override fun strvalue(): LuaString {
        return LuaString.valueOf(tojstring())
    }

    override fun optstring(defval: LuaString?): LuaString {
        return LuaString.valueOf(tojstring())
    }

    override fun tostring(): LuaValue {
        return LuaString.valueOf(tojstring())
    }

    override fun optjstring(defval: String?): String? {
        return tojstring()
    }

    override fun optnumber(defval: LuaNumber?): LuaNumber? {
        return this
    }

    override fun isnumber(): Boolean {
        return true
    }

    override fun isstring(): Boolean {
        return true
    }

    override fun tonumber(): LuaValue? {
        return this
    }

    override fun checkint(): Int {
        return v.toLong().toInt()
    }

    override fun checklong(): Long {
        return v.toLong()
    }

    override fun checknumber(): LuaNumber? {
        return this
    }

    override fun checkdouble(): Double {
        return v
    }

    override fun checkjstring(): String? {
        return tojstring()
    }

    override fun checkstring(): LuaString {
        return LuaString.valueOf(tojstring())
    }

    override fun isvalidkey(): Boolean {
        return !(v).isNaN()
    }

    companion object {
        /** Constant LuaDouble representing NaN (not a number)  */
        val NAN: LuaDouble = net.blueva.luak.LuaDouble(Double.NaN)

        /** Constant LuaDouble representing positive infinity  */
        val POSINF: LuaDouble = net.blueva.luak.LuaDouble(Double.POSITIVE_INFINITY)

        /** Constant LuaDouble representing negative infinity  */
        val NEGINF: LuaDouble = net.blueva.luak.LuaDouble(Double.NEGATIVE_INFINITY)

        /** Constant String representation for NaN (not a number), "nan"  */
        val JSTR_NAN: String = "nan"

        /** Constant String representation for positive infinity, "inf"  */
        val JSTR_POSINF: String = "inf"

        /** Constant String representation for negative infinity, "-inf"  */
        val JSTR_NEGINF: String = "-inf"

        fun valueOf(d: Double): LuaNumber? {
            val id = d.toInt()
            return if (d == id.toDouble()) LuaInteger.valueOf(id) as LuaNumber? else net.blueva.luak.LuaDouble(d) as LuaNumber
        }

        /** Divide two double numbers according to lua math, and return a [LuaValue] result.
         * @param lhs Left-hand-side of the division.
         * @param rhs Right-hand-side of the division.
         * @return [LuaValue] for the result of the division,
         * taking into account positive and negiative infinity, and Nan
         * @see .ddiv_d
         */
        fun ddiv(lhs: Double, rhs: Double): LuaValue? {
            return if (rhs != 0.0) net.blueva.luak.LuaDouble.Companion.valueOf(lhs / rhs) else if (lhs > 0) net.blueva.luak.LuaDouble.Companion.POSINF else if (lhs == 0.0) net.blueva.luak.LuaDouble.Companion.NAN else net.blueva.luak.LuaDouble.Companion.NEGINF
        }

        /** Divide two double numbers according to lua math, and return a double result.
         * @param lhs Left-hand-side of the division.
         * @param rhs Right-hand-side of the division.
         * @return Value of the division, taking into account positive and negative infinity, and Nan
         * @see .ddiv
         */
        fun ddiv_d(lhs: Double, rhs: Double): Double {
            return if (rhs != 0.0) lhs / rhs else if (lhs > 0) Double.POSITIVE_INFINITY else if (lhs == 0.0) Double.NaN else Double.NEGATIVE_INFINITY
        }

        /** Take modulo double numbers according to lua math, and return a [LuaValue] result.
         * @param lhs Left-hand-side of the modulo.
         * @param rhs Right-hand-side of the modulo.
         * @return [LuaValue] for the result of the modulo,
         * using lua's rules for modulo
         * @see .dmod_d
         */
        fun dmod(lhs: Double, rhs: Double): LuaValue? {
            if (rhs == 0.0 || lhs == Double.POSITIVE_INFINITY || lhs == Double.NEGATIVE_INFINITY) return net.blueva.luak.LuaDouble.Companion.NAN
            if (rhs == Double.POSITIVE_INFINITY) {
                return if (lhs < 0) net.blueva.luak.LuaDouble.Companion.POSINF else net.blueva.luak.LuaDouble.Companion.valueOf(
                    lhs
                )
            }
            if (rhs == Double.NEGATIVE_INFINITY) {
                return if (lhs > 0) net.blueva.luak.LuaDouble.Companion.NEGINF else net.blueva.luak.LuaDouble.Companion.valueOf(
                    lhs
                )
            }
            return net.blueva.luak.LuaDouble.Companion.valueOf(lhs - rhs * Math.floor(lhs / rhs))
        }

        /** Take modulo for double numbers according to lua math, and return a double result.
         * @param lhs Left-hand-side of the modulo.
         * @param rhs Right-hand-side of the modulo.
         * @return double value for the result of the modulo,
         * using lua's rules for modulo
         * @see .dmod
         */
        fun dmod_d(lhs: Double, rhs: Double): Double {
            if (rhs == 0.0 || lhs == Double.POSITIVE_INFINITY || lhs == Double.NEGATIVE_INFINITY) return Double.NaN
            if (rhs == Double.POSITIVE_INFINITY) {
                return if (lhs < 0) Double.POSITIVE_INFINITY else lhs
            }
            if (rhs == Double.NEGATIVE_INFINITY) {
                return if (lhs > 0) Double.NEGATIVE_INFINITY else lhs
            }
            return lhs - rhs * Math.floor(lhs / rhs)
        }
    }
}
