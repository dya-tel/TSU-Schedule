package ru.dyatel.tsuschedule.events

import java.util.ArrayList
import java.util.EnumMap

object EventBus {

    private val listeners = EnumMap<Event, MutableList<EventListener>>(Event::class.java)

    fun subscribe(listener: EventListener, vararg types: Event) {
        val typeArray = if (types.isEmpty()) Event.values() else types
        typeArray.forEach { listeners.getOrPut(it, { ArrayList() }) += listener }
    }

    fun unsubscribe(listener: EventListener) = listeners.values.forEach { it.remove(listener) }

    fun broadcast(event: Event, payload: Any? = null) = listeners[event]?.forEach { it.handleEvent(event, payload) }

}