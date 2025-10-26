package com.perflyst.twire.utils

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.ViewGroup
import androidx.constraintlayout.utils.widget.ImageFilterView
import androidx.transition.Transition
import androidx.transition.TransitionValues

class RoundTransition() : Transition() {
    init {
        addTarget(ImageFilterView::class.java)
    }


    override fun getTransitionProperties(): Array<String> {
        return arrayOf(PROPNAME_ROUND)
    }

    private fun captureValues(transitionValues: TransitionValues) {
        val view = transitionValues.view
        if (view is ImageFilterView) {
            transitionValues.values[PROPNAME_ROUND] = view.roundPercent
        }
    }

    override fun captureStartValues(transitionValues: TransitionValues) {
        captureValues(transitionValues)
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        captureValues(transitionValues)
    }

    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        if (startValues == null || endValues == null) {
            return null
        }

        val view = endValues.view
        if (view !is ImageFilterView) {
            return null
        }

        val startRound = startValues.values[PROPNAME_ROUND] as Float? ?: 0f
        val endRound = endValues.values[PROPNAME_ROUND] as Float? ?: 0f

        if (startRound != endRound) {
            return ObjectAnimator.ofFloat(
                view as ImageFilterView,
                "roundPercent",
                startRound,
                endRound
            )
        }

        return null
    }

    companion object {
        const val PROPNAME_ROUND = "com.perflyst.twire:RoundTransition:roundPercent"
    }
}
