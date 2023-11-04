package org.ktlib.db.ktorm

import org.ktlib.entities.EntityMarker
import org.ktorm.entity.Entity
import kotlin.reflect.KProperty

interface EntityKtorm<T : Entity<T>> : Entity<T>, org.ktlib.entities.Entity, EntityMarker {
    override fun lazyEntityValues(): MutableMap<String, Any?> {
        val key = "__lazyEntityValues__"
        if (!properties.containsKey(key)) {
            set(key, mutableMapOf<String, Any?>())
        }
        @Suppress("UNCHECKED_CAST")
        return this[key] as MutableMap<String, Any?>
    }

    override fun setProperty(property: KProperty<Any?>, value: Any?) {
        this[property.name] = value
    }

    override fun copyEntity(): org.ktlib.entities.Entity {
        return copy() as org.ktlib.entities.Entity
    }
}
