package kr.co.pirnardoors.pettaxikotlin.Controller

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_driver_login.*
import kr.co.pirnardoors.pettaxikotlin.R

class DriverLoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_login)

        registerBtn.setOnClickListener {
            var intent = Intent(this, DriverRegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
