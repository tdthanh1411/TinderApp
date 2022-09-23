package com.example.twiliovideo.sdk

import android.os.Handler
import android.os.HandlerThread
import com.twilio.video.Room
import com.twilio.video.StatsListener

class StatsScheduler(private val roomManager: RoomManager, private val room: Room) {
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private val statsListener: StatsListener = StatsListener { statsReports ->
        roomManager.sendStatsUpdate(statsReports)
    }
    private val isRunning: Boolean
        get() = handlerThread?.isAlive ?: false

    fun start() {
        if (isRunning) {
            stop()
        }
        val handlerThread = HandlerThread("StatsSchedulerThread")
        this.handlerThread = handlerThread
        handlerThread.start()
        val handler = Handler(handlerThread.looper)
        this.handler = handler
        val statsRunner: Runnable = object : Runnable {
            override fun run() {
                room.getStats(statsListener)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(statsRunner)
    }

    fun stop() {
        if (isRunning) {
            handlerThread?.let { handlerThread ->
                handlerThread.quit()
                this.handlerThread = null
                handler = null
            }
        }
    }
}
