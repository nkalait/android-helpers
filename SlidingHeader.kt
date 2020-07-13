package [some package]

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import java.lang.Exception
import java.lang.NullPointerException
import kotlin.math.abs

/**
 HOW TO USE:
 Scenario, to use in Fragment.

 Step 1. override the variables constraintSet, maxHeaderTranslateUp, maxHeaderTranslateDown,
 and headerView with your desired values

 Step 2: override the function initSlidingHeader(...), first thing is to call super.initConstraintSet(...)
 inside it

 class YourFragmentClass : Fragment(), SlidingHeader {
     override val constraintSet: ConstraintSet = ConstraintSet()
     override var maxHeaderTranslateUp: Float = 0f
     override var maxHeaderTranslateDown: Float = 0f
     override var headerView: View? = [SOME VALUE]
    .
    .
    .
     override fun initSlidingHeader(hv: View, cl: ConstraintLayout, rv: RecyclerView, sl: RecyclerView.OnScrollListener?) {
         /** must be called before everything else */
         super.initConstraintSet(hv, cl, rv, sl)
         headerView?.let {
             it.post {
                 maxHeaderTranslateDown = it.translationY
                 val headerTopMargin = it.marginTop
                 val headerHeight = it.measuredHeight.toFloat()
                 val headerZValue = it.elevation + it.translationZ
                 maxHeaderTranslateUp = 0 - (headerTopMargin + headerHeight + headerZValue)
             }
         }
     }
     .
     .
     .
     // then in some function or somewhere in your code, your call the initSlidingHeader function you overrided
     fun someFunction(){
        initSlidingHeader(yourHeaderView, yourConstraintLayout, yourRecyclerView, null)
     }
 }
 */

/**
 * @property SlidingHeader
 * Used to make a header that slides up and down as the user scrolls in a [androidx.recyclerview.widget.RecyclerView]
 * Only works with a [View] inside a [ConstraintLayout]
 */
interface SlidingHeader {
    /**
     * @property headerView The view that will slide up and down
     * @property constraintSet ConstraintLayout constraint set
     * @property maxHeaderTranslateDown How far the [headerView] can slide down relative to its original position when the user scrolls down
     * @property maxHeaderTranslateUp How far the [headerView] can slide up relative to its original position when the user scrolls up
     */
    val constraintSet: ConstraintSet
    var headerView: View?
    var maxHeaderTranslateDown: Float
    var maxHeaderTranslateUp: Float

    /**
     * @property initSlidingHeader
     *
     * The super [initSlidingHeader] method must be called before everything else
     *
     * @param hv The "Header View", this is the view that scroll out of view when user scrolls up
     * @param cl The ConstraintLayout that serves as the "root View", is this view that contains the [hv], it MUST be of type [ConstraintLayout]
     * @param rv The RecyclerView on which we shall listen for scroll events
     * @param sl If provided, it is associated onScroll listener that will provide scroll events. If not provided then the default will be used
     */
    fun initSlidingHeader(hv: View, cl: ConstraintLayout, rv: RecyclerView, sl: RecyclerView.OnScrollListener?) {
        if(testViewIsConstraintLayout(cl)) {
            headerView = hv
            hv.post {
                setScrollListener(rv, sl)
                constraintSet.clone(cl)
            }
        }
    }

    /**
     * @property testViewIsConstraintLayout
     * Return true if [headerView]'s parent is of type [ConstraintLayout]
     */
    private fun testViewIsConstraintLayout(v: View): Boolean {
        if(v !is ConstraintLayout){
            throw Exception("Parent of view must be of type ConstraintLayout, current parent is ${v::class.simpleName}")
        }
        return true
    }

    /**
     * @property translateHeaderUp
     * Translate the [headerView] up by distance relative to [speed]
     *
     * @param speed The speed at which to translate [headerView] by
     */
    private fun translateHeaderUp(speed: Float = 1f) {
        headerView?.let {
            val viewParent = it.parent
            if (testViewIsConstraintLayout(viewParent as View)) {
                val translateAmount: Float = it.translationY - speed
                translateHeader(
                    viewParent = viewParent as ConstraintLayout,
                    translateAmount = translateAmount,
                    up = true
                )
            }
        } ?: throw Exception(NullPointerException("headerView is null, did you call super.initConstraintSet()?"))

    }

    /**
     * @property translateHeaderDown
     * Translate the [headerView] down by distance relative to [speed]
     *
     * @param speed The speed at which to translate [headerView] by
     */
    private fun translateHeaderDown(speed: Float = 1f) {
        headerView?.let {
            val viewParent = it.parent
            if (testViewIsConstraintLayout(viewParent as View)) {
                val translateAmount: Float = it.translationY + speed
                translateHeader(
                    viewParent = viewParent as ConstraintLayout,
                    translateAmount = translateAmount,
                    up = false
                )
            }
        } ?: throw Exception(NullPointerException("headerView is null, did you call super?"))

    }

    /**
     * @property translateHeader
     * Translate the [headerView] down or up by distance relative [translateAmount] relative to the [headerView]'s original position
     *
     * @param viewParent The view that contains [headerView]
     * @param translateAmount The distance to move [headerView]
     * @param up If true then translate upward, downward if false
     */
    private fun translateHeader(viewParent: ConstraintLayout, translateAmount: Float, up: Boolean) {
        headerView?.let {
            if ((testViewIsConstraintLayout(viewParent))) {
                if (up) {
                    if (translateAmount <= maxHeaderTranslateUp) {
                        applyTranslation(
                            viewId = it.id,
                            viewParent = viewParent,
                            translateAmount = maxHeaderTranslateUp
                        )
                        return
                    }
                } else {
                    if (translateAmount >= maxHeaderTranslateDown) {
                        applyTranslation(
                            viewId = it.id,
                            viewParent = viewParent,
                            translateAmount = maxHeaderTranslateDown
                        )
                        return
                    }
                }
                applyTranslation(
                    viewId = it.id,
                    viewParent = viewParent,
                    translateAmount = translateAmount
                )
            }
        } ?: throw Exception(NullPointerException("headerView is null, did you call super.initConstraintSet()?"))
    }

    /**
     * @property applyTranslation
     * Apply translation to the constraint layout
     */
    private fun applyTranslation(viewId: Int, viewParent: ConstraintLayout, translateAmount: Float) {
        val constraintSetTemp = ConstraintSet()
        constraintSetTemp.clone(constraintSet)
        constraintSetTemp.setTranslationY(viewId, translateAmount)
        constraintSetTemp.applyTo(viewParent)
    }

    /**
     * @property setScrollListener
     * Sets the on scroll listener to the associated [RecyclerView]
     *
     * @param rv The RecyclerView to set the [RecyclerView.OnScrollListener] to
     * @param sl The onScrollListener to attach to [rv], if null then the default is attached
     */
    private fun setScrollListener(rv: RecyclerView, sl: RecyclerView.OnScrollListener?) {
        sl?.let {
            rv.addOnScrollListener((it))
        } ?: rv.addOnScrollListener( object : RecyclerView.OnScrollListener() {
            /**
             * Callback method to be invoked when RecyclerView's scroll state changes.
             *
             * @param recyclerView The RecyclerView whose scroll state has changed.
             * @param newState     The updated scroll state. One of [.SCROLL_STATE_IDLE],
             * [.SCROLL_STATE_DRAGGING] or [.SCROLL_STATE_SETTLING].
             */
            override fun onScrollStateChanged(
                recyclerView: RecyclerView,
                newState: Int
            ) {
            }

            /**
             * Callback method to be invoked when the RecyclerView has been scrolled. This will be
             * called after the scroll has completed.
             *
             *
             * This callback will also be called if visible item range changes after a layout
             * calculation. In that case, dx and dy will be 0.
             *
             * @param recyclerView The RecyclerView which scrolled.
             * @param dx The amount of horizontal scroll.
             * @param dy The amount of vertical scroll.
             */
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val speed = abs(dy).toFloat()
                when {
                    dy < 0 -> translateHeaderDown(speed)
                    dy > 0 -> translateHeaderUp(speed)
                }
            }
        })
    }
}
