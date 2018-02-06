package kr.co.pirnardoors.pettaxikotlin.Controller

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import kr.co.pirnardoors.pettaxikotlin.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startBtn.setOnClickListener {
            var intent = Intent(this, StartActivity::class.java)
            startActivity(intent)
        }

    }

}
