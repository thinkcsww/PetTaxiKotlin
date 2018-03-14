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
import kotlinx.android.synthetic.main.activity_block.*
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.DRIVER_ID
import kr.co.pirnardoors.pettaxikotlin.Utilities.DRIVER_NICKNAME
import kr.co.pirnardoors.pettaxikotlin.Utilities.PREF_NAME

class BlockActivity : AppCompatActivity() {
    var userId = FirebaseAuth.getInstance().currentUser?.uid
    val driverDB = FirebaseDatabase.getInstance().getReference("Driver")
    var readyToTest = false
    val auth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block)
        var sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()
        var Id = sharedPreferences.getString(DRIVER_NICKNAME, "")
        welcomText.text = "${Id}님\n 반갑습니다!"

        okBtn.setOnClickListener {
            auth.signOut()
            val intent = Intent(this@BlockActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
            return@setOnClickListener
        }


            driverDB.child(userId).addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError?) {
                    if (p0 != null) p0.message
                }

                override fun onDataChange(p0: DataSnapshot?) {
                    if (p0 != null) {
                        readyToTest = p0.child("ReadyToTest").getValue(String::class.java)!!.toBoolean()
                        if(readyToTest == true) {
                            val intent = Intent(this@BlockActivity, WaitingAuthActivity::class.java)
                            startActivity(intent)
                            finish()
                            return
                        }
                    }
                }
            })








        nextBtn.setOnClickListener {
            val intent = Intent(this@BlockActivity, DriverAuthorizingActivity::class.java)
            startActivity(intent)
            finish()
            return@setOnClickListener
        }
    }
}
