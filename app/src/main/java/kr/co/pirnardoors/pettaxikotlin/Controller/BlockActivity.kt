package kr.co.pirnardoors.pettaxikotlin.Controller

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_block.*
import kr.co.pirnardoors.pettaxikotlin.R

class BlockActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block)

        nextBtn.setOnClickListener {
            val intent = Intent(this@BlockActivity, DriverAuthorizingActivity::class.java)
            startActivity(intent)
            finish()
            return@setOnClickListener
        }
    }
}
