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
 *  Copyright (c) 2026 Blueva Development
 *
 *  SPDX-License-Identifier: MIT
 ******************************************************************************/
package net.blueva.luak.io

actual typealias IOException = java.io.IOException
actual typealias EOFException = java.io.EOFException
actual typealias InputStream = java.io.InputStream
actual typealias OutputStream = java.io.OutputStream
actual abstract class Reader protected actual constructor() : java.io.Reader() {
    actual abstract override fun read(chars: CharArray, offset: Int, length: Int): Int
    actual override fun read(): Int = super.read()
    actual abstract override fun close()
}
actual typealias ByteArrayInputStream = java.io.ByteArrayInputStream
actual typealias ByteArrayOutputStream = java.io.ByteArrayOutputStream
actual typealias DataInputStream = java.io.DataInputStream
actual typealias DataOutputStream = java.io.DataOutputStream
actual typealias PrintStream = java.io.PrintStream

actual fun standardOutput(): PrintStream = System.out
actual fun standardError(): PrintStream = System.err
actual fun platformResource(name: String): InputStream? =
    object {}.javaClass.getResourceAsStream(name)
