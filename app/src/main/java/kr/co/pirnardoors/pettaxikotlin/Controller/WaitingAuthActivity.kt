package kr.co.pirnardoors.pettaxikotlin.Controller

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_waiting_auth.*
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.DRIVER_ID
import kr.co.pirnardoors.pettaxikotlin.Utilities.DRIVER_LICENSE_AUTHORIZED
import kr.co.pirnardoors.pettaxikotlin.Utilities.PREF_NAME

class WaitingAuthActivity : AppCompatActivity() {

    val auth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting_auth)
        val sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()
        button3.setOnClickListener {
            auth.signOut()
            editor.putBoolean(DRIVER_LICENSE_AUTHORIZED, false)
            editor.apply()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return@setOnClickListener
        }

    }
}
