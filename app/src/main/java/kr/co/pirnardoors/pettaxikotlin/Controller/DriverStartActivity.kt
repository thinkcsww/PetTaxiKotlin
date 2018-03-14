package kr.co.pirnardoors.pettaxikotlin.Controller

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_driver_start.*
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.DRIVER_LICENSE_AUTHORIZED
import kr.co.pirnardoors.pettaxikotlin.Utilities.DRIVER_LOGON
import kr.co.pirnardoors.pettaxikotlin.Utilities.PREF_NAME

class DriverStartActivity : AppCompatActivity() {
    val auth = FirebaseAuth.getInstance()
    var authStateListener = FirebaseAuth.AuthStateListener {  }
    val handler = Handler()
    var driverAuthorized = false
    var userId = FirebaseAuth.getInstance().currentUser?.uid
    var driverDB = FirebaseDatabase.getInstance().getReference("Driver")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_start)
        var sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()

        if(FirebaseAuth.getInstance().currentUser != null) {
            driverDB.child(userId).addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError?) {
                    if (p0 != null) p0.message
                }

                override fun onDataChange(p0: DataSnapshot?) {
                    if (p0 != null) {
                        driverAuthorized = p0.child("Auth").getValue(String::class.java)!!.toBoolean()
                        editor.putBoolean(DRIVER_LICENSE_AUTHORIZED, driverAuthorized)
                        editor.apply()
                    }
                }
            })
        }

        backBtn.setOnClickListener {
            super.onBackPressed()
        }

        authStateListener = FirebaseAuth.AuthStateListener {
            var user = FirebaseAuth.getInstance().currentUser

            if (user != null) {
                editor.putBoolean(DRIVER_LOGON, true)
                if(driverAuthorized == true) {
                    val intent = Intent(this, ViewRequestActivity::class.java)
                    startActivity(intent)
                    finish()
                    return@AuthStateListener
                } else {
                    val intent = Intent(this@DriverStartActivity, BlockActivity::class.java)
                    startActivity(intent)
                    finish()
                    return@AuthStateListener
                }
            }
        }

        loginBtn.setOnClickListener {
            var intent = Intent(this, DriverLoginActivity::class.java)
            startActivity(intent)
            return@setOnClickListener
        }
        registerBtn.setOnClickListener {
            var intent = Intent(this, DriverRegisterActivity::class.java)
            startActivity(intent)
            return@setOnClickListener
        }
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }
}
