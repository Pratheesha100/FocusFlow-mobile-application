package com.geeklabs.remindme.database


import android.content.Context
import android.content.SharedPreferences
import com.geeklabs.remindme.models.Reminder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.atomic.AtomicLong

class SharedPreferencesHandler(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("RemindMePrefs", Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()
    private val gson = Gson()

    companion object {
        private const val REMINDER_KEY = "ReminderList"
        private const val ID_COUNTER = "ReminderIdCounter"
    }

    private val idCounter: AtomicLong by lazy {
        AtomicLong(sharedPreferences.getLong(ID_COUNTER, 0))
    }

    private fun generateId(): Long {
        val newId = idCounter.incrementAndGet()
        editor.putLong(ID_COUNTER, newId).apply()
        return newId
    }

    fun saveReminder(reminder: Reminder): Boolean {
        val reminderList = getAllReminders().toMutableList()

        if (reminder.id == 0L) {
            reminder.id = generateId()
        }

        reminderList.add(reminder)
        return saveReminderList(reminderList)
    }


    fun getReminderById(id: Long): Reminder? {
        val reminderList = getAllReminders()
        return reminderList.find { it.id == id }
    }

    fun updateReminder(updatedReminder: Reminder): Boolean {
        val reminderList = getAllReminders().toMutableList()
        val index = reminderList.indexOfFirst { it.id == updatedReminder.id }

        if (index != -1) {
            reminderList[index] = updatedReminder
            return saveReminderList(reminderList)
        }
        return false
    }

    fun deleteReminderById(id: Long): Boolean {
        val reminderList = getAllReminders().toMutableList()
        val updatedList = reminderList.filter { it.id != id }
        return saveReminderList(updatedList)
    }

    fun getAllReminders(): List<Reminder> {
        val jsonString = sharedPreferences.getString(REMINDER_KEY, null)
        return if (jsonString != null) {
            val type = object : TypeToken<List<Reminder>>() {}.type
            gson.fromJson(jsonString, type)
        } else {
            emptyList()
        }
    }

    private fun saveReminderList(reminderList: List<Reminder>): Boolean {
        val jsonString = gson.toJson(reminderList)
        editor.putString(REMINDER_KEY, jsonString)
        return editor.commit()
    }

    fun clearAllReminders(): Boolean {
        editor.remove(REMINDER_KEY)
        editor.putLong(ID_COUNTER, 0L)
        return editor.commit()
    }
}
