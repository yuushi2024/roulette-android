package com.yuushi2024.roulette.logic

/**
 * Height of the wheel as a fraction of the available space.
 *
 * The soft keyboard takes a large part of a phone screen. Keeping the wheel at
 * its normal height would leave too little room for the input box, so the wheel
 * shrinks while the keyboard is open and the box stays usable.
 *
 * Android types are avoided on purpose so this can be unit tested on the JVM.
 */
object WheelLayout {

    /** Fraction of the screen used by the wheel when nothing else is open. */
    const val NORMAL_HEIGHT_PERCENT = 0.40f

    /** Fraction used while the soft keyboard is visible. */
    const val IME_HEIGHT_PERCENT = 0.16f

    /** Fraction of the available height the wheel should take. */
    fun wheelHeightPercent(imeVisible: Boolean): Float =
        if (imeVisible) IME_HEIGHT_PERCENT else NORMAL_HEIGHT_PERCENT
}
