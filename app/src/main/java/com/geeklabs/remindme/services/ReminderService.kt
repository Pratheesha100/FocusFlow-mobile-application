package com.geeklabs.remindme.services


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.geeklabs.remindme.R
import com.geeklabs.remindme.activities.MainActivity
import com.geeklabs.remindme.database.SharedPreferencesHandler
import com.geeklabs.remindme.models.Reminder
import java.util.*

class ReminderService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("ReminderService", "onCreate called")
        // Enable to play a custom ringtone
        /*mediaPlayer = MediaPlayer.create(this, R.raw.alarm_ringtone)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()*/
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d("ReminderService", "onBind called")
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ReminderService", "onStartCommand called")
        val reminderId = intent?.getLongExtra("reminderId", 0)
        val reminderPreferences = SharedPreferencesHandler(this)
        val reminder = reminderPreferences.getReminderById(reminderId ?: 0)
        if (reminder != null) {
            showAlarmNotification(reminder)

            val speakText = reminder.title + " " + reminder.description
            tts = TextToSpeech(this,
                TextToSpeech.OnInitListener {
                    if (it != TextToSpeech.ERROR) {
                        tts?.language = Locale.US
                        tts?.speak(speakText, TextToSpeech.QUEUE_ADD, null, null)
                    } else {
                        Log.d("ReminderService", "Error: $it")
                    }
                })
            Log.d("ReminderService", speakText)
        } else {
            Log.d("ReminderService", "Reminder not found")
        }
        return START_STICKY
    }

    private fun showAlarmNotification(reminder: Reminder) {
        Log.d("ReminderService", "showAlarmNotification called")

        createNotificationChannel(reminder.id.toInt())
        // Build notification
        val builder = NotificationCompat.Builder(this, reminder.id.toString())
            .setSmallIcon(R.drawable.app_logo) // Set icon for notification
            .setContentTitle(reminder.title) // Set title of notification
            .setContentText(reminder.description) // Set notification message
            .setAutoCancel(true) // Makes auto cancel of notification
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Set priority of notification

        val notificationIntent = Intent(applicationContext, MainActivity::class.java)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        // Notification message will get at NotificationView
        notificationIntent.putExtra("reminderId", reminder.id)
        notificationIntent.putExtra("from", "Notification")

        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.setContentIntent(pendingIntent)
        val notification = builder.build()

        // Add as notification
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(reminder.id.toInt(), notification)
    }

    private fun createNotificationChannel(id: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                id.toString(),
                "Reminder Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        Log.d("ReminderService", "onDestroy called")
        super.onDestroy()

        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }

        tts?.stop()
        tts?.shutdown()
    }
}
