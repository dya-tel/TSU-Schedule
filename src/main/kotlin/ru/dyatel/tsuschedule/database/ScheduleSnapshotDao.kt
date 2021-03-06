package ru.dyatel.tsuschedule.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import hirondelle.date4j.DateTime
import org.jetbrains.anko.db.AUTOINCREMENT
import org.jetbrains.anko.db.DEFAULT
import org.jetbrains.anko.db.INTEGER
import org.jetbrains.anko.db.LongParser
import org.jetbrains.anko.db.MapRowParser
import org.jetbrains.anko.db.PRIMARY_KEY
import org.jetbrains.anko.db.SqlOrderDirection
import org.jetbrains.anko.db.TEXT
import org.jetbrains.anko.db.createTable
import org.jetbrains.anko.db.dropTable
import org.jetbrains.anko.db.select
import org.jetbrains.anko.db.update
import ru.dyatel.tsuschedule.model.GroupLesson
import ru.dyatel.tsuschedule.model.ScheduleSnapshot
import ru.dyatel.tsuschedule.utilities.schedulePreferences
import java.util.TimeZone

class ScheduleSnapshotDao(context: Context, databaseManager: DatabaseManager) : DatabasePart(databaseManager) {

    object Columns {
        const val ID = "id"
        const val GROUP = "group_index"
        const val TIMESTAMP = "timestamp"
        const val HASH = "hash"
        const val PINNED = "pinned"
        const val SELECTED = "selected"
    }

    companion object {
        const val TABLE = "schedule_snapshots"

        private val ROW_PARSER = object : MapRowParser<ScheduleSnapshot> {
            override fun parseRow(columns: Map<String, Any?>): ScheduleSnapshot {
                return ScheduleSnapshot(
                        columns[Columns.ID] as Long,
                        columns[Columns.GROUP] as String,
                        columns[Columns.TIMESTAMP] as Long,
                        (columns[Columns.PINNED] as Long).asFlag(),
                        (columns[Columns.SELECTED] as Long).asFlag())
            }
        }
    }

    private val preferences = context.schedulePreferences

    override fun createTables(db: SQLiteDatabase) {
        db.createTable(TABLE, true,
                Columns.ID to INTEGER + PRIMARY_KEY + AUTOINCREMENT,
                Columns.GROUP to TEXT,
                Columns.TIMESTAMP to INTEGER,
                Columns.HASH to INTEGER,
                Columns.PINNED to INTEGER + DEFAULT(false.toInt().toString()),
                Columns.SELECTED to INTEGER + DEFAULT(true.toInt().toString()))
    }

    override fun upgradeTables(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 7) {
            db.dropTable(TABLE, true)
            createTables(db)
            return
        }
    }

    fun request(group: String): List<ScheduleSnapshot> {
        return execute {
            select(TABLE)
                    .whereSimple("${Columns.GROUP} = ?", group)
                    .orderBy(Columns.ID)
                    .parseList(ROW_PARSER)
        }
    }

    fun save(group: String, lessons: Collection<GroupLesson>) {
        executeTransaction {
            val timezone = TimeZone.getDefault()
            val timestamp = DateTime.now(timezone).getMilliseconds(timezone)

            val hash = lessons.sumBy { it.hashCode() }

            update(TABLE, Columns.SELECTED to false.toInt())
                    .whereSimple("${Columns.GROUP} = ?", group)
                    .exec()

            val duplicate = select(TABLE)
                    .whereSimple("${Columns.GROUP} = ? AND ${Columns.HASH} = ?", group, hash.toString())
                    .parseOpt(ROW_PARSER)
                    ?.takeIf {
                        val old = databaseManager.rawGroupSchedule.request(it.id.toString())
                        lessons.size == old.size && lessons.all { old.contains(it) }
                    }

            val contentValues = ContentValues().apply {
                put(Columns.GROUP, group)
                put(Columns.TIMESTAMP, timestamp)
                put(Columns.HASH, hash)

                if (duplicate != null) {
                    put(Columns.PINNED, duplicate.pinned.toInt())
                }
            }
            val id = insert(TABLE, null, contentValues)

            if (duplicate != null) {
                databaseManager.rawGroupSchedule.transferSnapshot(duplicate.id, id)
                remove(duplicate.id)
            } else {
                databaseManager.rawGroupSchedule.save(id.toString(), lessons)
                removeSurplus(group)
            }

            if (duplicate == null || !duplicate.selected) {
                databaseManager.filteredGroupSchedule.save(group, lessons)
            }
        }
    }

    fun update(id: Long, pinned: Boolean, selected: Boolean) {
        executeTransaction {
            val old = if (selected) {
                select(TABLE)
                        .whereSimple("${Columns.ID} = ?", id.toString())
                        .parseSingle(ROW_PARSER)
            } else null

            update(TABLE, Columns.PINNED to pinned.toInt(), Columns.SELECTED to selected.toInt())
                    .whereSimple("${Columns.ID} = ?", id.toString())
                    .exec()

            if (selected) {
                old!!

                update(TABLE, Columns.SELECTED to false.toInt())
                        .whereSimple("${Columns.GROUP} = ? AND ${Columns.ID} != ?", old.group, id.toString())
                        .exec()

                if (!old.selected) {
                    val lessons = databaseManager.rawGroupSchedule.request(id.toString())
                    databaseManager.filteredGroupSchedule.save(old.group, lessons)
                }
            }
        }
    }

    fun remove(id: Long) {
        executeTransaction {
            val snapshot = select(TABLE)
                    .whereSimple("${Columns.ID} = ?", id.toString())
                    .parseSingle(ROW_PARSER)

            if (snapshot.selected) {
                databaseManager.filteredGroupSchedule.remove(snapshot.group)
            }

            databaseManager.rawGroupSchedule.remove(id.toString())
            delete(TABLE, "${Columns.ID} = ?", arrayOf(id.toString()))
        }
    }

    fun removeSurplus(group: String) {
        execute {
            select(TABLE, Columns.ID)
                    .whereSimple("${Columns.GROUP} = ? AND ${Columns.SELECTED} = 0 AND ${Columns.PINNED} = 0", group)
                    .orderBy(Columns.ID, SqlOrderDirection.DESC)
                    .limit(preferences.historySize, Int.MAX_VALUE) // -1 is not supported for some reason
                    .parseList(LongParser)
                    .forEach { remove(it) }
        }
    }

}
