package kr.co.pirnardoors.pettaxikotlin.Controller

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_waiting_auth.*
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.DRIVER_LICENSE_AUTHORIZED
import kr.co.pirnardoors.pettaxikotlin.Utilities.DRIVER_LOGON
import kr.co.pirnardoors.pettaxikotlin.Utilities.PREF_NAME

class WaitingAuthActivity : AppCompatActivity() {

    val auth = FirebaseAuth.getInstance()
    val driverDB = FirebaseDatabase.getInstance().getReference("Driver")
    var userId = FirebaseAuth.getInstance().currentUser!!.uid
    var driverAuthorized = false
    var authChecked = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting_auth)
        val sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()


        driverDB.addValueEventListener(object :ValueEventListener{
            override fun onCancelled(p0: DatabaseError?) {
                if(p0 != null) p0.message
            }

            override fun onDataChange(dataSnapshot: DataSnapshot?) {
                if(dataSnapshot != null) {
                    driverAuthorized = dataSnapshot.child(userId).child("Auth").getValue().toString().toBoolean()
                    if(driverAuthorized == true && authChecked == false) {
                        authChecked = true
                        editor.putBoolean(DRIVER_LICENSE_AUTHORIZED, true)
                        editor.apply()
                        val intent = Intent(this@WaitingAuthActivity, ViewRequestActivity::class.java)
                        startActivity(intent)
                        finish()
                        return

                    }
                }
            }

        })
//        if(driverAuthorized == true) {
//            val intent = Intent(this@WaitingAuthActivity, ViewRequestActivity::class.java)
//            startActivity(intent)
//            finish()
//            return
//        }

        verifyBtn.setOnClickListener {
            auth.signOut()
            editor.putBoolean(DRIVER_LOGON,false)
            editor.putBoolean(DRIVER_LICENSE_AUTHORIZED, false)
            editor.apply()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return@setOnClickListener
        }

    }
}
