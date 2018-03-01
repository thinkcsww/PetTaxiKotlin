package kr.co.pirnardoors.pettaxikotlin.Controller

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.ALARM_BROADCAST

/**
 * Created by std on 2018-03-01.
 */
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val notificationManager = context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, CustomerMapActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(context, ALARM_BROADCAST, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationCompat.Builder(context)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setContentTitle("CatCarDog 알림")
                    .setContentText("1시간 후 예약시간입니다.")
                    .setSmallIcon(R.drawable.ic_subdirectory_arrow_left_black_24dp)
                    .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
                    .setPriority(NotificationManager.IMPORTANCE_HIGH)
        } else {
            NotificationCompat.Builder(context)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setContentTitle("CatCarDog 알림")
                    .setContentText("1시간 후 예약시간입니다.")
                    .setSmallIcon(R.drawable.ic_subdirectory_arrow_left_black_24dp)
                    .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_MAX)
        }
        notificationManager.notify(ALARM_BROADCAST, builder.build())
    }
}