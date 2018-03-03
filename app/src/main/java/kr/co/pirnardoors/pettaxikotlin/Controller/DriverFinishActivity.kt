package kr.co.pirnardoors.pettaxikotlin.Controller

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog

import kr.co.pirnardoors.pettaxikotlin.R

class DriverFinishActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_finish)
        val mBuilder = AlertDialog.Builder(this@DriverFinishActivity)
        val mView = layoutInflater.inflate(R.layout.layout_driver_finish, null)
        mBuilder.setView(mView)
        val dialog =mBuilder.create()
        dialog.show()
    }
}
