package com.h4ckm310n.s5w2c

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object ConfigManager {
    private val defaultConfigs = mutableMapOf<String, Any>(
        "timeout" to "10",
        "port" to "3111",
        "forward_buff_size" to "32"
    )
    private val displayNames = mapOf(
        "timeout" to "Forward timeout (s)",
        "port" to "Listen port",
        "forward_buff_size" to "Buffer Size (kb)"
    )
    private val configs = mutableMapOf<String, Any>()
    private lateinit var dataStore: DataStore<Preferences>

    suspend fun initConfig(newDataStore: DataStore<Preferences>) {
        dataStore = newDataStore
        // Config data exists in local storage
        if (dataStore.data.map { it.contains(stringPreferencesKey("timeout")) }.first()) {
            val keys = dataStore.data.map { it.asMap().keys }.first()
            keys.forEach { key ->
                configs[key.name] = dataStore.data.map { it[key] }.first()!!
            }
            return
        }

        write(defaultConfigs)
        configs.putAll(defaultConfigs)
    }

    suspend fun update(newConfigs: MutableList<MutableState<SettingItem>>) {
        newConfigs.forEach { item ->
            configs[item.value.name] = item.value.value
        }
        write(configs)
    }

    fun getDefaultConfigs(): MutableList<MutableState<SettingItem>> {
        return getConfigs(defaultConfigs)
    }

    fun getConfigs(): MutableList<MutableState<SettingItem>> {
        return getConfigs(configs)
    }

    fun getConfig(key: String): Any {
        return configs[key]!!
    }

    fun getDisplayName(configName: String): String {
        return displayNames[configName]!!
    }

    private suspend fun write(newConfigs: Map<String, Any>) {
        dataStore.edit {
            newConfigs.forEach { (key, value) ->
                when (value) {
                    is Boolean -> it[booleanPreferencesKey(key)] = value
                    is Int -> it[intPreferencesKey(key)] = value
                    is String -> it[stringPreferencesKey(key)] = value
                }
            }
        }
    }

    private fun getConfigs(configs: MutableMap<String, Any>): MutableList<MutableState<SettingItem>> {
        val items = mutableStateListOf<MutableState<SettingItem>>()
        configs.forEach { (name, value) ->
            items.add(mutableStateOf(SettingItem(name, value)))
        }
        return items
    }
}