package app.lawnchair.smartspace

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import app.lawnchair.LawnchairLauncher
import app.lawnchair.launcher
import app.lawnchair.launcherNullable
import app.lawnchair.ui.preferences.PreferenceActivity
import app.lawnchair.ui.preferences.navigation.Smartspace
import com.android.launcher3.CheckLongPressHelper
import com.android.launcher3.R
import com.android.launcher3.logging.StatsLogManager
import com.android.launcher3.views.OptionsPopupView

class SmartspaceViewContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val previewMode: Boolean = false,
) : FrameLayout(context, attrs) {

    private val longPressHelper = CheckLongPressHelper(this) { performLongClick() }
    private val smartspaceView: View

    init {
        val inflater = LayoutInflater.from(context)
        smartspaceView = inflater.inflate(R.layout.smartspace_enhanced, this, false) as BcSmartspaceView
        smartspaceView.previewMode = previewMode
        val ctx = LawnchairLauncher.instance?.launcherNullable
        val dp = ctx?.deviceProfile
        val leftPadding = dp?.widgetPadding?.left
        smartspaceView.setPadding(leftPadding ?: (left + 16), top, right, bottom)
        setOnLongClickListener {
            openOptions()
            true
        }
        addView(smartspaceView)
    }

    private fun openOptions() {
        if (previewMode) return

        val launcher = context.launcher
        val pos = Rect()
        launcher.dragLayer.getDescendantRectRelativeToSelf(smartspaceView, pos)
        OptionsPopupView.show<LawnchairLauncher>(launcher, RectF(pos), listOf(getCustomizeOption()), true)
    }

    private fun getCustomizeOption() = OptionsPopupView.OptionItem(
        context,
        R.string.action_customize,
        R.drawable.ic_setting,
        StatsLogManager.LauncherEvent.IGNORE,
    ) {
        context.startActivity(PreferenceActivity.createIntent(context, Smartspace))
        true
    }

    private fun checkScrollableRecursively(viewGroup: ViewGroup): Boolean {
        if (viewGroup is android.widget.AdapterView<*>) {
            return true
        } else {
            for (i in 0 until viewGroup.childCount) {
                val child = viewGroup.getChildAt(i)
                if (child is ViewGroup) {
                    if (checkScrollableRecursively(child)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val dragLayer = context.launcher.dragLayer
            if (checkScrollableRecursively(this)) {
                dragLayer.requestDisallowInterceptTouchEvent(true)
            }
        }
        longPressHelper.onTouchEvent(ev)
        return longPressHelper.hasPerformedLongPress()
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        longPressHelper.onTouchEvent(ev)
        return true
    }

    override fun cancelLongPress() {
        super.cancelLongPress()
        longPressHelper.cancelLongPress()
    }
}
