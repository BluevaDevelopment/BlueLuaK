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

import net.blueva.luak.LuaString
import java.io.ByteArrayOutputStream
import java.io.UnsupportedEncodingException

object Str {
    @JvmStatic
    fun quoteString(image: String): LuaString {
        val s = image.substring(1, image.length - 1)
        val bytes = unquote(s)
        return LuaString.valueUsing(bytes)
    }

    @JvmStatic
    fun charString(image: String): LuaString {
        val s = image.substring(1, image.length - 1)
        val bytes = unquote(s)
        return LuaString.valueUsing(bytes)
    }

    @JvmStatic
    fun longString(image: String): LuaString {
        val i = image.indexOf('[', image.indexOf('[') + 1) + 1
        val s = image.substring(i, image.length - i)
        val b = iso88591bytes(s)
        return LuaString.valueUsing(b)
    }

    fun iso88591bytes(s: String): ByteArray {
        try {
            return s.toByteArray(charset("ISO8859-1"))
        } catch (e: UnsupportedEncodingException) {
            throw IllegalStateException("ISO8859-1 not supported")
        }
    }

    fun unquote(s: String): ByteArray {
        val baos = ByteArrayOutputStream()
        val c = s.toCharArray()
        val n = c.size
        var i = 0
        while (i < n) {
            if (c[i] == '\\' && i < n) {
                when (c[++i]) {
                    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                        var d = (c[i++].code - '0'.code)
                        var j = 0
                        while (i < n && j < 2 && c[i] >= '0' && c[i] <= '9') {
                            d = d * 10 + (c[i].code - '0'.code)
                            i++
                            j++
                        }
                        baos.write(d.toByte().toInt())
                        --i
                        i++
                        continue
                    }

                    'a' -> {
                        baos.write(7.toByte().toInt())
                        i++
                        continue
                    }

                    'b' -> {
                        baos.write('\b'.code.toByte().toInt())
                        i++
                        continue
                    }

                    'f' -> {
                        baos.write(0x0C)
                        i++
                        continue
                    }

                    'n' -> {
                        baos.write('\n'.code.toByte().toInt())
                        i++
                        continue
                    }

                    'r' -> {
                        baos.write('\r'.code.toByte().toInt())
                        i++
                        continue
                    }

                    't' -> {
                        baos.write('\t'.code.toByte().toInt())
                        i++
                        continue
                    }

                    'v' -> {
                        baos.write(11.toByte().toInt())
                        i++
                        continue
                    }

                    '"' -> {
                        baos.write('"'.code.toByte().toInt())
                        i++
                        continue
                    }

                    '\'' -> {
                        baos.write('\''.code.toByte().toInt())
                        i++
                        continue
                    }

                    '\\' -> {
                        baos.write('\\'.code.toByte().toInt())
                        i++
                        continue
                    }

                    else -> baos.write(c[i].code.toByte().toInt())
                }
            } else {
                baos.write(c[i].code.toByte().toInt())
            }
            i++
        }
        return baos.toByteArray()
    }
}
