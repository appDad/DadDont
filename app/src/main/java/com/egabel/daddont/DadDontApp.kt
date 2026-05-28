package com.egabel.daddont

import android.app.Application
import com.egabel.daddont.data.db.DadDontDatabase

class DadDontApp : Application() {
    val database: DadDontDatabase by lazy { DadDontDatabase.getInstance(this) }
}
