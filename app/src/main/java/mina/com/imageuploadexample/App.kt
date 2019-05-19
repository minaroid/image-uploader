package mina.com.imageuploadexample

import android.app.Application
import android.util.Log
import com.miguelbcr.ui.rx_paparazzo2.RxPaparazzo
import timber.log.Timber

class App :Application() {

    override fun onCreate() {
        super.onCreate()
        RxPaparazzo.register(this)
        setupTimberTree()
    }

    private fun setupTimberTree() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Log.d("minaLog", "DebugVersion")
        } else {

        }
    }
}