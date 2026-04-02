package app.lawnchair.nexuslauncher

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.view.HapticFeedbackConstantsCompat
import app.lawnchair.LawnchairLauncher
import app.lawnchair.util.unsafeLazy
import com.android.launcher3.CheckLongPressHelper
import com.android.launcher3.Launcher
import com.android.launcher3.R
import com.android.launcher3.logging.StatsLogManager.EventEnum
import com.android.launcher3.qsb.QsbWidgetHostView
import com.android.launcher3.views.BaseDragLayer.TouchCompleteListener
import com.android.launcher3.views.OptionsPopupView
import com.android.launcher3.views.OptionsPopupView.OptionItem

sealed class SmartSpaceHostView(context: Context) :
    QsbWidgetHostView(context),
    OnLongClickListener,
    TouchCompleteListener {
    private val mLauncher: Launcher by unsafeLazy { Launcher.getLauncher(context) }

    @Suppress("LeakingThis")
    private val mLongPressHelper: CheckLongPressHelper = CheckLongPressHelper(this, this)

    private var mIsScrollable = false

    override fun getErrorView(): View {
        return SmartspaceQsb.getDateView(this)
    }

    override fun onLongClick(view: View): Boolean {
        if (!hasSettings(view.context)) {
            return false
        }
        performHapticFeedback(HapticFeedbackConstantsCompat.LONG_PRESS)
        val pos = Rect()
        mLauncher.dragLayer.getDescendantRectRelativeToSelf(this, pos)
        val centerPos = RectF()
        centerPos.right = pos.exactCenterX()
        centerPos.left = centerPos.right
        centerPos.top = 0f
        centerPos.bottom = pos.bottom.toFloat()
        centerPos.bottom = findBottomRecur(this, pos.top, pos).toFloat().coerceAtMost(centerPos.bottom)
        val item = OptionItem(
            view.context,
            R.string.smartspace_preferences,
            R.drawable.ic_smartspace_preferences,
            NexusLauncherEnum.SMARTSPACE_TAP_OR_LONGPRESS,
        ) { v: View -> openSettings(v) }
        OptionsPopupView.show<LawnchairLauncher>(mLauncher, centerPos, listOf(item), true)
        return true
    }

    private fun findBottomRecur(view: View, max: Int, tempRect: Rect): Int {
        var ret = max
        if (view.visibility != VISIBLE) {
            return ret
        }
        if (view is ViewGroup) {
            for (i in view.childCount - 1 downTo 0) {
                ret = findBottomRecur(view.getChildAt(i), ret, tempRect).coerceAtLeast(ret)
            }
        }
        if (!view.willNotDraw()) {
            mLauncher.dragLayer.getDescendantRectRelativeToSelf(view, tempRect)
            return ret.coerceAtLeast(tempRect.bottom)
        }
        return ret
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val dragLayer = mLauncher.dragLayer
            if (mIsScrollable) {
                dragLayer.requestDisallowInterceptTouchEvent(true)
            }
            dragLayer.setTouchCompleteListener(this)
        }
        mLongPressHelper.onTouchEvent(ev)
        return mLongPressHelper.hasPerformedLongPress()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        mLongPressHelper.onTouchEvent(ev)
        return true
    }

    override fun cancelLongPress() {
        super.cancelLongPress()
        mLongPressHelper.cancelLongPress()
    }

    override fun onTouchComplete() {
        if (!mLongPressHelper.hasPerformedLongPress()) {
            mLongPressHelper.cancelLongPress()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        mIsScrollable = isTaggedAsScrollable() || checkScrollableRecursively(this)
    }

    private fun isTaggedAsScrollable(): Boolean {
        for (i in 0 until childCount) {
            (getChildAt(i).getTag(android.R.id.widget_frame) as? Int)?.let { widgetFrameTag ->
                // The widget_frame tag is set to 0 when RemoteViews is created from
                // DrawInstructions (i.e. the widget renders its own content via a Canvas-like
                // API and is always considered scrollable). A non-zero value is a regular
                // RemoteViews layout resource ID, which may or may not be scrollable — that
                // case is handled by checkScrollableRecursively.
                return widgetFrameTag == 0
            }
        }
        return false
    }

    // Mirrors LauncherAppWidgetHostView.checkScrollableRecursively: checks for AdapterView
    // descendants (ListView, GridView, StackView, etc.) in the RemoteViews hierarchy.
    // Widgets backed by DrawInstructions are detected separately by isTaggedAsScrollable.
    private fun checkScrollableRecursively(viewGroup: ViewGroup): Boolean {
        if (viewGroup is AdapterView<*>) return true
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is ViewGroup && checkScrollableRecursively(child)) return true
        }
        return false
    }

    private fun openSettings(v: View): Boolean {
        v.context.startActivity(createSettingsIntent())
        return true
    }

    companion object {
        private const val SETTINGS_INTENT_ACTION = "com.google.android.apps.gsa.smartspace.SETTINGS"
        fun hasSettings(context: Context): Boolean {
            val info = context.packageManager
                .resolveActivity(createSettingsIntent(), 0)
            return info != null
        }

        fun createSettingsIntent(): Intent {
            return Intent(SETTINGS_INTENT_ACTION)
                .setPackage(SmartspaceQsb.WIDGET_PACKAGE_NAME)
                .setFlags(
                    Intent.FLAG_RECEIVER_FOREGROUND
                        or Intent.FLAG_ACTIVITY_NO_HISTORY
                        or Intent.FLAG_ACTIVITY_NEW_TASK
                        or Intent.FLAG_ACTIVITY_NEW_DOCUMENT,
                )
        }
    }
}

enum class NexusLauncherEnum(override val id: Int) : EventEnum {
    SMARTSPACE_TAP_OR_LONGPRESS(520),
}
