package com.geeklabs.remindme.activities

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import com.geeklabs.remindme.R
import com.geeklabs.remindme.adapters.ReminderAdapter
import com.geeklabs.remindme.database.SharedPreferencesHandler
import com.geeklabs.remindme.models.Reminder
import com.geeklabs.remindme.services.AlarmReceiver
import com.geeklabs.remindme.services.ReminderService
import com.geeklabs.remindme.utils.Util
import com.google.android.gms.ads.MobileAds
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), ReminderAdapter.OnItemClickListener {

    private lateinit var reminderPreferences: SharedPreferencesHandler
    private lateinit var adapter: ReminderAdapter
    private var reminderList = mutableListOf<Reminder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initAdds()
        reminderPreferences = SharedPreferencesHandler(this) // Use SharedPreferencesHandler instead of ReminderPreferences
        adapter = ReminderAdapter(this)
        recycler_view_reminder.adapter = adapter

        getAllRemindersFromDB()

        addReminderButton.setOnClickListener {
            val reminderIntent = Intent(this, AddReminderActivity::class.java)
            startActivity(reminderIntent)
        }

        val from = intent.getStringExtra("from")
        if (from == "Notification") {
            val reminderId = intent.getLongExtra("reminderId", 0)
            val reminderById = reminderPreferences.getReminderById(reminderId)
            reminderById?.let {
                showReminderAlert(it)
            }
        }

        searchET.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                filterData(newText)
                return false
            }
        })
    }

    private fun initAdds() {
        MobileAds.initialize(this) { }
    }

    private fun filterData(query: String) {
        val finalList = if (query.isEmpty()) reminderList else reminderList.filter {
            it.title.toLowerCase(Locale.getDefault())
                .contains(query.toLowerCase(Locale.getDefault())) ||
                    it.description.toLowerCase(Locale.getDefault()).contains(
                        query.toLowerCase(
                            Locale.getDefault()
                        )
                    )
        }
        if (finalList.isNotEmpty()) {
            updateList(finalList.toMutableList())
        }
    }

    private fun updateList(finalList: MutableList<Reminder>) {
        adapter.reminderList = finalList
        adapter.notifyDataSetChanged()

        if (reminderList.isNotEmpty()) {
            recycler_view_reminder.visibility = View.VISIBLE
            noData.visibility = View.GONE
        } else {
            recycler_view_reminder.visibility = View.GONE
            noData.visibility = View.VISIBLE
        }
    }

    private fun getAllRemindersFromDB() {
        reminderList = reminderPreferences.getAllReminders().toMutableList()
        updateList(reminderList)
    }

    override fun onResume() {
        super.onResume()
        getAllRemindersFromDB()
    }

    private fun showReminderAlert(reminder: Reminder) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(reminder.title)
        builder.setMessage(reminder.description)
        builder.setPositiveButton("STOP ALARM") { dialog, _ ->
            Util.showToastMessage(this, "Your alarm has been stopped")
            dialog.dismiss()
            stopAlarm()
            stopReminderService()
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun stopAlarm() {
        val intent = Intent(this, AlarmReceiver::class.java)
        val sender = PendingIntent.getBroadcast(this, 0, intent, 0)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(sender)
    }

    private fun stopReminderService() {
        val reminderService = Intent(this, ReminderService::class.java)
        stopService(reminderService)
    }

    override fun onItemClick(
        reminder: Reminder,
        view: View,
        position: Int
    ) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener {
            if (it.title == getString(R.string.update)) {
                startActivity(
                    Intent(this, AddReminderActivity::class.java)
                        .putExtra("reminder", reminder)
                )
            } else if (it.title == getString(R.string.delete)) {
                reminderPreferences.deleteReminderById(reminder.id)
                getAllRemindersFromDB()
            }
            return@setOnMenuItemClickListener true
        }
        popupMenu.show()
    }

}
