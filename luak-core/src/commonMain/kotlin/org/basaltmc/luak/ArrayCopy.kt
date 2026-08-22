/******************************************************************************
 *  _                _
 * | |   _   _  __ _| | __
 * | |  | | | |/ _` | |/ /
 * | |__| |_| | (_| |   <
 * |_____\__,_|\__,_|_|\_\
 *
 *  Luak
 *  https://github.com/BasaltProject/Luak
 *
 *  Copyright (c) 2026 Basalt Project
 *
 *  SPDX-License-Identifier: MIT
 ******************************************************************************/
package org.basaltmc.luak

internal fun <T> arrayCopy(
    source: Array<out T>?,
    sourceIndex: Int,
    destination: Array<in T>?,
    destinationIndex: Int,
    length: Int,
) {
    source!!.copyInto(destination!!, destinationIndex, sourceIndex, sourceIndex + length)
}

internal fun arrayCopy(source: ByteArray?, sourceIndex: Int, destination: ByteArray?, destinationIndex: Int, length: Int) {
    source!!.copyInto(destination!!, destinationIndex, sourceIndex, sourceIndex + length)
}

internal fun arrayCopy(source: IntArray?, sourceIndex: Int, destination: IntArray?, destinationIndex: Int, length: Int) {
    source!!.copyInto(destination!!, destinationIndex, sourceIndex, sourceIndex + length)
}

internal fun arrayCopy(source: CharArray?, sourceIndex: Int, destination: CharArray?, destinationIndex: Int, length: Int) {
    source!!.copyInto(destination!!, destinationIndex, sourceIndex, sourceIndex + length)
}
