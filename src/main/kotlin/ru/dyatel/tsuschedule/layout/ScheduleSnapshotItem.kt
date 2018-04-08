package ru.dyatel.tsuschedule.layout

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.fastadapter_extensions.swipe.ISwipeable
import com.mikepenz.iconics.IconicsDrawable
import hirondelle.date4j.DateTime
import org.jetbrains.anko.alignParentLeft
import org.jetbrains.anko.alignParentRight
import org.jetbrains.anko.backgroundColorResource
import org.jetbrains.anko.centerVertically
import org.jetbrains.anko.dip
import org.jetbrains.anko.find
import org.jetbrains.anko.frameLayout
import org.jetbrains.anko.imageView
import org.jetbrains.anko.leftOf
import org.jetbrains.anko.leftPadding
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.relativeLayout
import org.jetbrains.anko.rightPadding
import org.jetbrains.anko.sp
import org.jetbrains.anko.textColorResource
import org.jetbrains.anko.textResource
import org.jetbrains.anko.textView
import ru.dyatel.tsuschedule.ADAPTER_SCHEDULE_SNAPSHOT_ITEM_ID
import ru.dyatel.tsuschedule.R
import ru.dyatel.tsuschedule.model.ScheduleSnapshot
import ru.dyatel.tsuschedule.utilities.hideIf
import java.util.TimeZone

typealias ClickListener = (ScheduleSnapshotItem) -> Unit

class ScheduleSnapshotItem(
        snapshot: ScheduleSnapshot
) : AbstractItem<ScheduleSnapshotItem, ScheduleSnapshotItem.ViewHolder>(),
        ISwipeable<ScheduleSnapshotItem, ScheduleSnapshotItem> {

    private companion object {
        val contentContainerId = View.generateViewId()
        val timestampViewId = View.generateViewId()
        val pinViewId = View.generateViewId()

        val swipedContainerId = View.generateViewId()
        val cancelViewId = View.generateViewId()
    }

    val id = snapshot.id
    var pinned = snapshot.pinned

    private val datetimeText = DateTime.forInstant(snapshot.timestamp, TimeZone.getDefault())
            .format("DD-MM-YYYY, hh:mm:ss")

    private var swipeable = true

    var clickListener: ClickListener? = null
    var pinClickListener: ClickListener? = null
    var cancelClickListener: ClickListener? = null

    init {
        withSetSelected(snapshot.selected)
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<ScheduleSnapshotItem>(view) {
        private val contentContainer = view.find<ViewGroup>(contentContainerId)
        private val timestampView = view.find<TextView>(timestampViewId)
        private val pinView = view.find<ImageView>(pinViewId)

        private val swipedContainer = view.find<ViewGroup>(swipedContainerId)
        private val cancelView = view.find<View>(cancelViewId)

        private val pinIcon = IconicsDrawable(view.context)
                .icon(CommunityMaterial.Icon.cmd_pin)
                .sizeDp(24)
        private val unpinIcon = IconicsDrawable(view.context)
                .icon(CommunityMaterial.Icon.cmd_pin_off)
                .sizeDp(24)

        override fun bindView(item: ScheduleSnapshotItem, payloads: List<Any>) {
            contentContainer.hideIf { !item.swipeable }
            swipedContainer.hideIf { item.swipeable }

            timestampView.text = item.datetimeText

            // Setting alpha due to a bug in Android Iconics
            if (item.isSelected) {
                contentContainer.backgroundColorResource = R.color.snapshot_selected_background_color
                timestampView.textColorResource = R.color.snapshot_selected_text_color
                pinIcon.alpha(0xff).colorRes(R.color.snapshot_selected_text_color)
                unpinIcon.alpha(0xff).colorRes(R.color.snapshot_selected_text_color)
            } else {
                contentContainer.backgroundColorResource = R.color.snapshot_background_color
                timestampView.textColorResource = R.color.snapshot_text_color
                pinIcon.alpha(0xff).colorRes(R.color.snapshot_text_color)
                unpinIcon.alpha(0xff).colorRes(R.color.snapshot_text_color)
            }

            if (item.pinned) {
                pinView.setImageDrawable(unpinIcon)
            } else {
                pinView.setImageDrawable(pinIcon)
            }

            contentContainer.setOnClickListener { item.clickListener?.invoke(item) }
            pinView.setOnClickListener { item.pinClickListener?.invoke(item) }
            cancelView.setOnClickListener { item.cancelClickListener?.invoke(item) }
        }

        override fun unbindView(item: ScheduleSnapshotItem) {
            timestampView.text = null

            contentContainer.setOnClickListener(null)
            pinView.setOnClickListener(null)
            cancelView.setOnClickListener(null)
        }
    }

    override fun createView(ctx: Context, parent: ViewGroup?): View {
        return ctx.frameLayout {
            lparams(width = matchParent)

            relativeLayout {
                id = contentContainerId

                lparams(width = matchParent) {
                    padding = dip(8)
                    leftPadding = dip(16)
                    rightPadding = dip(16)
                }

                textView {
                    id = timestampViewId
                    textSize = sp(8).toFloat()
                }.lparams {
                    alignParentLeft()
                    leftOf(pinViewId)
                    centerVertically()
                }

                imageView {
                    id = pinViewId
                }.lparams {
                    alignParentRight()
                    centerVertically()
                }
            }

            relativeLayout {
                id = swipedContainerId
                backgroundColorResource = R.color.snapshot_remove_background_color

                lparams(width = matchParent) {
                    padding = dip(8)
                    leftPadding = dip(16)
                    rightPadding = dip(16)
                }

                textView {
                    textResource = R.string.result_removed
                    textSize = sp(8).toFloat()
                    textColorResource = R.color.snapshot_remove_text_color
                }.lparams {
                    alignParentLeft()
                    leftOf(cancelViewId)
                }

                textView {
                    id = cancelViewId
                    textResource = R.string.dialog_cancel
                    textSize = sp(8).toFloat()
                    textColorResource = R.color.snapshot_remove_text_color
                }.lparams {
                    alignParentRight()
                }
            }
        }
    }

    override fun getType() = ADAPTER_SCHEDULE_SNAPSHOT_ITEM_ID

    override fun getViewHolder(view: View) = ViewHolder(view)

    override fun getLayoutRes() = throw UnsupportedOperationException()

    override fun withIsSwipeable(swipeable: Boolean): ScheduleSnapshotItem {
        this.swipeable = swipeable
        return this
    }

    override fun isSwipeable() = swipeable && !isSelected && !pinned

}
