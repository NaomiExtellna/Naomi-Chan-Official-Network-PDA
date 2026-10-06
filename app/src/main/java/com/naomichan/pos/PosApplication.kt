package com.naomichan.pos

import android.app.Application
import com.naomichan.pos.data.PosRepository

class PosApplication : Application() {
  val repository by lazy { PosRepository() }
}
