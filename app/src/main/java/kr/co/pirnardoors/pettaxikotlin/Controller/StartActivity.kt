package kr.co.pirnardoors.pettaxikotlin.Controller

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_start.*
import kr.co.pirnardoors.pettaxikotlin.R
import java.sql.Driver

class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        customerBtn.setOnClickListener {
            val intent = Intent(this, CustomerLoginActivity::class.java)
            startActivity(intent)
        }
        driverBtn.setOnClickListener {
            val intent = Intent(this, DriverLoginActivity::class.java)
            startActivity(intent)
        }
    }
}
