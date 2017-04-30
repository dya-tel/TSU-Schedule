package ru.dyatel.tsuschedule.fragments

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.jetbrains.anko.find
import ru.dyatel.tsuschedule.R
import ru.dyatel.tsuschedule.ScheduleApplication
import ru.dyatel.tsuschedule.data.LessonFetchTask
import ru.dyatel.tsuschedule.events.Event
import ru.dyatel.tsuschedule.events.EventBus
import ru.dyatel.tsuschedule.events.EventListener
import ru.dyatel.tsuschedule.layout.WeekFragmentPagerAdapter
import ru.dyatel.tsuschedule.parsing.currentWeekParity

class MainFragment : Fragment(), EventListener {

    private lateinit var weekAdapter: WeekFragmentPagerAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        weekAdapter = WeekFragmentPagerAdapter(childFragmentManager, context)

        EventBus.subscribe(this, Event.DATA_UPDATE_FAILED, Event.DATA_UPDATED)
    }

    override fun onDestroy() {
        EventBus.unsubscribe(this)
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.main_screen, container, false)

        val pager = root.find<ViewPager>(R.id.pager)
        pager.adapter = weekAdapter
        pager.currentItem = currentWeekParity.index

        val tabLayout = root.find<TabLayout>(R.id.tab_layout)
        tabLayout.setupWithViewPager(pager)

        val lessonDao = (activity.application as ScheduleApplication).databaseManager.lessonDao
        swipeRefresh = root.find<SwipeRefreshLayout>(R.id.swipe_refresh)
        swipeRefresh.setOnRefreshListener { LessonFetchTask(context, lessonDao).execute() }
        swipeRefresh.setOnChildScrollUpCallback { _, _ ->
            val week = weekAdapter.getFragment(pager.currentItem)
            week.view?.canScrollVertically(-1) ?: false
        }

        return root
    }

    override fun handleEvent(type: Event, payload: Any?) = activity.runOnUiThread { swipeRefresh.isRefreshing = false }

}
