package com.yuushi2024.roulette.logic

/**
 * Layout decisions for the keyboard-visible state.
 *
 * The soft keyboard can cover half of a phone screen. While it is open the wheel,
 * the result line and the start button are hidden so the input box can fill the
 * whole area above the keyboard, and the bottom inset of the keyboard is added as
 * padding so the box is never drawn behind it.
 *
 * Android types are avoided on purpose so this can be unit tested on the JVM.
 */
object WheelLayout {

    /** Fraction of the screen the wheel uses; must match activity_main.xml. */
    const val NORMAL_HEIGHT_PERCENT = 0.40f

    /** Whether the wheel, result and start button should be shown. */
    fun showWheelArea(imeVisible: Boolean): Boolean = !imeVisible

    /** Bottom padding that keeps the content above the keyboard or the nav bar. */
    fun bottomInset(imeVisible: Boolean, imeBottom: Int, systemBarBottom: Int): Int =
        if (imeVisible) maxOf(imeBottom, 0) else maxOf(systemBarBottom, 0)

    /**
     * Whether the keyboard is open, for platforms that do not report it directly.
     *
     * There the IME height is folded into the bottom inset while the system-bar
     * inset keeps reporting only the navigation bar, so a taller bottom inset means
     * the keyboard is up.
     */
    fun imeVisibleFromInsets(imeBottom: Int, systemBarBottom: Int): Boolean =
        imeBottom > systemBarBottom
}
