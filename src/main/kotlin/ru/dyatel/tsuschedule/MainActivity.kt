package ru.dyatel.tsuschedule

import android.app.AlertDialog
import android.app.Dialog
import android.app.NotificationChannel
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.view.ViewCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.wealthfront.magellan.Navigator
import com.wealthfront.magellan.support.SingleActivity
import com.wealthfront.magellan.transitions.NoAnimationTransition
import hirondelle.date4j.DateTime
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.ctx
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.editText
import org.jetbrains.anko.find
import org.jetbrains.anko.frameLayout
import org.jetbrains.anko.leftPadding
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.notificationManager
import org.jetbrains.anko.rightPadding
import org.jetbrains.anko.singleLine
import ru.dyatel.tsuschedule.database.database
import ru.dyatel.tsuschedule.events.Event
import ru.dyatel.tsuschedule.events.EventBus
import ru.dyatel.tsuschedule.events.EventListener
import ru.dyatel.tsuschedule.layout.DIM_DIALOG_SIDE_PADDING
import ru.dyatel.tsuschedule.layout.DIM_ELEVATION_F
import ru.dyatel.tsuschedule.model.currentWeekParity
import ru.dyatel.tsuschedule.screens.ExamScheduleScreen
import ru.dyatel.tsuschedule.screens.FilterScreen
import ru.dyatel.tsuschedule.screens.HistoryScreen
import ru.dyatel.tsuschedule.screens.HomeScreen
import ru.dyatel.tsuschedule.screens.PreferenceScreen
import ru.dyatel.tsuschedule.screens.ScheduleScreen
import ru.dyatel.tsuschedule.updater.Updater
import ru.dyatel.tsuschedule.utilities.Validator
import ru.dyatel.tsuschedule.utilities.schedulePreferences
import java.util.TimeZone

private const val SCHEDULE_SCREEN_ID_START = 1000

class MainActivity : SingleActivity(), EventListener {

    private lateinit var toolbar: Toolbar
    private lateinit var drawer: Drawer

    private lateinit var parityItem: PrimaryDrawerItem

    private val preferences = schedulePreferences
    private val updater by lazy { Updater(this) }

    private var selectedGroup: String?
        get() {
            val id = drawer.currentSelection - SCHEDULE_SCREEN_ID_START
            return if (id >= 0) preferences.groups[id.toInt()] else null
        }
        set(value) {
            val id = preferences.groups.indexOf(value) + SCHEDULE_SCREEN_ID_START
            drawer.setSelection(id.toLong())
        }

    private val drawerListener = object : Drawer.OnDrawerListener {
        override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit
        override fun onDrawerClosed(drawerView: View) = Unit
        override fun onDrawerOpened(drawerView: View) = updateParityItemText()
    }

    private val historySizeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == getString(R.string.preference_history_size)) {
            preferences.groups.forEach {
                database.snapshots.removeSurplus(it)
            }
        }
    }

    override fun createNavigator() = Navigator
            .withRoot(HomeScreen())
            .transition(NoAnimationTransition())
            .build()!!

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleUpdateNotification(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = notificationManager

            val name = getString(R.string.notification_channel_updates_name)
            notificationManager.createNotificationChannel(
                    NotificationChannel(NOTIFICATION_CHANNEL_UPDATES, name, NotificationManagerCompat.IMPORTANCE_LOW)
            )
        }

        updater.handleMigration()

        toolbar = find(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawer = DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withOnDrawerListener(drawerListener)
                .withOnDrawerNavigationListener { onBackPressed(); true }
                .withSavedInstance(savedInstanceState)
                .build()

        generateDrawerButtons()

        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(historySizeListener)

        EventBus.subscribe(
                this,
                Event.SET_TOOLBAR_SHADOW_ENABLED, Event.SET_DRAWER_ENABLED, Event.ADD_GROUP
        )
        EventBus.broadcast(Event.SET_TOOLBAR_SHADOW_ENABLED, true)

        if (!handleUpdateNotification(intent) && preferences.autoupdate) {
            val now = DateTime.now(TimeZone.getDefault())

            val scheduled = preferences.lastUpdateCheck?.plusDays(3)
            if (scheduled == null || scheduled.lteq(now)) {
                launch { updater.fetchUpdate(true) }
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (savedInstanceState == null) {
            preferences.group?.let { selectedGroup = it }

            if (preferences.pendingChangelogDisplay) {
                updater.showChangelog()
            }
        }
    }

    override fun onDestroy() {
        defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(historySizeListener)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        menu.findItem(R.id.filters).icon = getActionBarIcon(CommunityMaterial.Icon.cmd_filter)
        menu.findItem(R.id.exams).icon = getActionBarIcon(CommunityMaterial.Icon.cmd_calendar)
        menu.findItem(R.id.history).icon = getActionBarIcon(CommunityMaterial.Icon.cmd_history)
        menu.findItem(R.id.delete_group).icon = getActionBarIcon(CommunityMaterial.Icon.cmd_delete)
        menu.findItem(R.id.search).apply { icon = getActionBarIcon(CommunityMaterial.Icon.cmd_magnify) }
                .actionView.let { it as SearchView }
                .apply {
                    queryHint = getString(R.string.menu_search) + "..."

                    setIconifiedByDefault(false)
                    isSubmitButtonEnabled = true

                    imeOptions = imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI

                    maxWidth = Int.MAX_VALUE
                }
        return super.onCreateOptionsMenu(menu)
    }

    private fun getActionBarIcon(icon: IIcon) =
            IconicsDrawable(ctx).actionBar().icon(icon).colorRes(R.color.text_title_color)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.filters -> getNavigator().goTo(FilterScreen(selectedGroup!!))
            R.id.exams -> getNavigator().goTo(ExamScheduleScreen(selectedGroup!!))
            R.id.history -> getNavigator().goTo(HistoryScreen(selectedGroup!!))
            R.id.delete_group -> showDeleteGroupDialog()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun generateDrawerButtons() {
        drawer.removeAllItems()

        parityItem = PrimaryDrawerItem()
                .withIcon(CommunityMaterial.Icon.cmd_calendar)
                .withSelectable(false)
        drawer.addItem(parityItem)
        updateParityItemText()

        drawer.addItem(DividerDrawerItem())

        drawer.addItem(PrimaryDrawerItem()
                .withIcon(CommunityMaterial.Icon.cmd_plus)
                .withName(R.string.button_add_group)
                .withSelectable(false)
                .withOnDrawerItemClickListener { _, _, _ -> showAddGroupDialog(); true })

        for ((id, group) in preferences.groups.withIndex()) {
            drawer.addItem(PrimaryDrawerItem()
                    .withIdentifier((id + SCHEDULE_SCREEN_ID_START).toLong())
                    .withIcon(CommunityMaterial.Icon.cmd_account_multiple)
                    .withName(group)
                    .withSetSelected(preferences.group == group)
                    .withOnDrawerItemClickListener { _, _, _ -> getNavigator().replace(ScheduleScreen(group)); false })
        }

        drawer.addItem(DividerDrawerItem())

        /*drawer.addItem(PrimaryDrawerItem()
                .withIcon(CommunityMaterial.Icon.cmd_account)
                .withName(R.string.screen_teachers)
                .withOnDrawerItemClickListener { _, _, _ -> getNavigator().replace(TeacherSearchScreen()); false })

        drawer.addItem(DividerDrawerItem())*/

        drawer.addItem(PrimaryDrawerItem()
                .withIcon(CommunityMaterial.Icon.cmd_settings)
                .withName(R.string.screen_settings)
                .withOnDrawerItemClickListener { _, _, _ -> getNavigator().goTo(PreferenceScreen()); false }
                .withSelectable(false))
    }

    private fun updateParityItemText() {
        val parityText = currentWeekParity.toText(ctx).capitalize()
        parityItem.withName(ctx.getString(R.string.label_week, parityText))
        drawer.updateItem(parityItem)
    }

    private fun handleUpdateNotification(intent: Intent): Boolean {
        val result = intent.getStringExtra(INTENT_TYPE) == INTENT_TYPE_UPDATE
        if (result) {
            notificationManager.cancel(NOTIFICATION_UPDATE)

            var preferenceScreen: PreferenceScreen? = null
            getNavigator().navigate {
                preferenceScreen = it.mapNotNull { it as? PreferenceScreen }.singleOrNull()
            }

            if (preferenceScreen == null) {
                getNavigator().goTo(PreferenceScreen())
            } else {
                getNavigator().goBackTo(preferenceScreen)
            }
        }
        return result
    }

    private fun showAddGroupDialog() {
        val view = ctx.frameLayout {
            editText {
                singleLine = true
                imeOptions = imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            }.lparams(width = matchParent) {
                leftPadding = DIM_DIALOG_SIDE_PADDING
                rightPadding = DIM_DIALOG_SIDE_PADDING
            }
        }
        val editor = view.getChildAt(0) as EditText

        AlertDialog.Builder(ctx)
                .setTitle(R.string.dialog_add_group_title)
                .setMessage(R.string.dialog_add_group_message)
                .setView(view)
                .setPositiveButton(R.string.dialog_ok) { _, _ -> }
                .setNegativeButton(R.string.dialog_cancel) { _, _ -> }
                .show()
                .apply {
                    getButton(Dialog.BUTTON_POSITIVE).setOnClickListener { _ ->
                        try {
                            val group = Validator.validateGroup(editor.text.toString())

                            if (group in preferences.groups) {
                                setMessage(getString(R.string.dialog_add_group_message_duplicate))
                                return@setOnClickListener
                            }

                            EventBus.broadcast(Event.ADD_GROUP, group)

                            dismiss()
                        } catch (e: BlankGroupIndexException) {
                            setMessage(getString(R.string.dialog_add_group_message_blank))
                        } catch (e: ShortGroupIndexException) {
                            setMessage(getString(R.string.dialog_add_group_message_short))
                        }
                    }
                }
    }

    private fun showDeleteGroupDialog() {
        val group = selectedGroup!!

        AlertDialog.Builder(ctx)
                .setTitle(R.string.dialog_remove_group_title)
                .setMessage(getString(R.string.dialog_remove_group_message, group))
                .setPositiveButton(R.string.dialog_ok) { _, _ ->
                    val navigator = getNavigator()

                    val groups = preferences.groups
                    val newGroup: String?
                    if (groups.size > 1) {
                        val index = groups.indexOf(group)
                        newGroup = if (index == groups.size - 1) groups[index - 1] else groups[index + 1]
                        navigator.replace(ScheduleScreen(newGroup))
                    } else {
                        preferences.group = null
                        newGroup = null
                        navigator.replace(HomeScreen())
                    }

                    database.snapshots.request(group).forEach {
                        database.snapshots.remove(it.id)
                    }
                    database.filters.remove(group)
                    database.exams.remove(group)
                    preferences.removeGroup(group)

                    generateDrawerButtons()
                    newGroup?.let { selectedGroup = it }
                }
                .setNegativeButton(R.string.dialog_cancel) { _, _ -> }
                .show()
    }

    override fun handleEvent(type: Event, payload: Any?) {
        when (type) {
            Event.SET_TOOLBAR_SHADOW_ENABLED -> {
                val enabled = payload as Boolean
                ViewCompat.setElevation(toolbar, if (enabled) DIM_ELEVATION_F else 0f)
            }
            Event.SET_DRAWER_ENABLED -> {
                val toggle = drawer.actionBarDrawerToggle
                val layout = drawer.drawerLayout
                val actionBar = supportActionBar!!

                val enabled = payload as Boolean
                if (enabled) {
                    actionBar.setDisplayHomeAsUpEnabled(false)
                    toggle.isDrawerIndicatorEnabled = true
                    layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                } else {
                    toggle.isDrawerIndicatorEnabled = false
                    actionBar.setDisplayHomeAsUpEnabled(true)
                    layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                }
            }
            Event.ADD_GROUP -> {
                val group = payload as String

                preferences.addGroup(group)

                generateDrawerButtons()
                selectedGroup = group
                drawer.closeDrawer()

                EventBus.broadcast(Event.INITIAL_DATA_FETCH, group)
            }
        }
    }

}
