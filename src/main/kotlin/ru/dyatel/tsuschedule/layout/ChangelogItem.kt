package ru.dyatel.tsuschedule.layout

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import org.jetbrains.anko.find
import org.jetbrains.anko.margin
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.textView
import org.jetbrains.anko.verticalLayout
import ru.dyatel.tsuschedule.ADAPTER_CHANGELOG_ITEM_ID
import ru.dyatel.tsuschedule.R
import ru.dyatel.tsuschedule.model.VersionChangelog

class ChangelogItem(val changelog: VersionChangelog) : AbstractItem<ChangelogItem, ChangelogItem.ViewHolder>() {

    private companion object {
        val versionViewId = View.generateViewId()
        val changelogViewId = View.generateViewId()
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<ChangelogItem>(view) {
        private val versionView = view.find<TextView>(versionViewId)
        private val changelogView = view.find<TextView>(changelogViewId)

        override fun bindView(item: ChangelogItem, payloads: List<Any>) {
            var versionText = item.changelog.version
            if (item.changelog.prerelease) {
                val context = itemView.context
                versionText += " (${context.getString(R.string.dialog_changelog_prerelease_tag)})"
            }

            versionView.text = versionText
            changelogView.text = item.changelog.changelog
        }

        override fun unbindView(item: ChangelogItem?) {
            versionView.text = null
            changelogView.text = null
        }
    }

    override fun createView(ctx: Context, parent: ViewGroup?): View {
        return ctx.verticalLayout {
            lparams(width = matchParent) {
                margin = DIM_LARGE
            }

            textView {
                id = versionViewId
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }

            textView {
                id = changelogViewId
            }
        }
    }

    override fun getType() = ADAPTER_CHANGELOG_ITEM_ID

    override fun getViewHolder(view: View) = ViewHolder(view)

    override fun getLayoutRes() = throw UnsupportedOperationException()

}