package one.xcorp.widget.swipepicker

import java.math.RoundingMode

internal class ScaleHelper {

    /**
     * Find the closest value to the specified values.
     *
     * @param left Value in left side.
     * @param right Value in right side.
     * @param value The value for which to search for the closest.
     * @return Closest value left or right. The left value in priority.
     */
    fun closestValue(left: Float, right: Float, value: Float) =
            if (Math.abs(value - left) <= Math.abs(right - value)) left else right

    /**
     * Automatically completes the scale to the value and determines
     * the closest value on the scale.
     *
     * @param boundary The end value of the scale from which
     * you want to complete the scale with the specified step.
     * @param step Step with which the scale is completed. If step is equal 0 then return boundary.
     * @param value The value for which to search for the closest on the scale.
     * @return Closest value on the virtual scale. The closest values to the boundary in priority.
     */
    fun closestInBoundary(boundary: Float, step: Float, value: Float): Float {
        if (step == 0f) return boundary

        // rounding to the closest to the boundary
        val distance = ((value - boundary) / step)
                .toBigDecimal().setScale(0, RoundingMode.HALF_DOWN).toInt()
        val offset = distance.toBigDecimal() * step.toBigDecimal()

        return (boundary.toBigDecimal() + offset).toFloat()
    }

    /**
     * Move on the scale from the value to the specified division.
     *
     * @param scale The scale in which you must move.
     * Сan be {@code null} if you only need to take into account the step.
     * @param step Step with which you need to move.
     * If is 0 then movement outside the scale is forbidden.
     * @param value The value from which need moved.
     * @param division The number of divisions that have moved.
     * @return The calculated value for specified division.
     */
    fun moveTo(scale: List<Float>?, step: Float, value: Float, division: Int): Float {
        if (division == 0) return value
        // the scale is not specified, calculate the value based on the step
        if (scale == null) return moveByStep(value, division, step)
        // movement outside the scale without crossing it
        if ((value < scale.first() && division < 0) || (value > scale.last() && division > 0)) {
            return moveOutside(scale, value, division, step)
        }
        // movement outside the scale with a possible intersection of scale
        if (value !in scale.first()..scale.last()) {
            return moveInside(scale, value, division, step)
        }
        // Finding the index of the value on the scale. If the value is not found
        // returns the index of the nearest value taking into account the direction of the gesture.
        var index: Int = scale.binarySearch(value)
        if (index < 0) {
            val offset = if (division < 0) 1 else 2
            index = -(index + offset)
        }
        // the value index lies on the scale, we move along it
        return moveByScale(scale, index, division, step)
    }

    private fun moveByStep(value: Float, division: Int, step: Float) =
            (value.toBigDecimal() + division.toBigDecimal() * step.toBigDecimal()).toFloat()

    private fun moveOutside(scale: List<Float>, value: Float, division: Int, step: Float): Float {
        // outward movement is impossible
        if (step == 0f) return value

        val closestValue: Float
        val offset: Int

        // Attract to the value on the scale
        if (division < 0) { // direction right to left
            closestValue = closestInBoundary(scale.first(), step, value)
            offset = if (closestValue < value) 1 else 0
        } else { // direction left to right
            closestValue = closestInBoundary(scale.last(), step, value)
            offset = if (closestValue > value) -1 else 0
        }
        // calculate the value based on the step from closest value
        return moveByStep(closestValue, division + offset, step)
    }

    private fun moveInside(scale: List<Float>, value: Float, division: Int, step: Float): Float {
        val boundaryIndex: Int
        val offset: Int

        if (division > 0) {  // direction left to right
            boundaryIndex = 0
            offset = -1
        } else {  // direction right to left
            boundaryIndex = scale.lastIndex
            offset = 1
        }
        // if step 0 means we are attracted to the boundary of the scale and move along it
        if (step == 0f) return moveByScale(scale, boundaryIndex, division + offset, step)
        val distance = ((value - scale[boundaryIndex]) / step)
        // the number of divisions up to the scale of values remaining after the move
        val remainder = division + distance
                .toBigDecimal().setScale(0, RoundingMode.UP).toInt()
        // move from the scale outwards or along it, depending on the sign of the remainder
        return moveByScale(scale, boundaryIndex, remainder, step)
    }

    private fun moveByScale(scale: List<Float>, index: Int, division: Int, step: Float): Float {
        val destination = index + division

        return when {
        // move on the scale outwards to the left
            destination < 0 ->
                moveByStep(scale.first(), destination, step)
        // move on the scale outwards to the right
            destination > scale.lastIndex ->
                moveByStep(scale.last(), (destination - scale.lastIndex), step)
        // move on the scale
            else -> scale[destination]
        }
    }
}