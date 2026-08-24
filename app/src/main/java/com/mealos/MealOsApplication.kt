package com.mealos

import android.app.Application
import com.mealos.data.MealOsRepository
import com.mealos.timer.TimerNotifications

class MealOsApplication : Application() {
    val repository by lazy { MealOsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        TimerNotifications.createChannel(this)
    }
}
