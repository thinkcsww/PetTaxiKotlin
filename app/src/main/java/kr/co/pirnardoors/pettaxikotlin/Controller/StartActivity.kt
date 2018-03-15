package kr.co.pirnardoors.pettaxikotlin.Controller

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_start.*
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.PREF_NAME


class StartActivity : AppCompatActivity() {
    lateinit var myRunnable : Runnable
    var isReady = false
    var userTypeCustomer : String? = ""
    var userTypeDriver : String? = ""
    var userId : String? = ""
    var handler = Handler()
    val user_field : String = "usr"
    val pwd_field : String = "pwd"
    var driverAuthorized = false
    var readyToTest = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        userId  = FirebaseAuth.getInstance().currentUser?.uid
        var sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()

        if(userId == null) {
            userTypeCustomer =""
            userTypeDriver = ""
            Log.i(kr.co.pirnardoors.pettaxikotlin.Utilities.TAG, "Null")
        }
        if(userId != null) {
            var customerDB = FirebaseDatabase.getInstance().getReference("Customer").child(userId).child("UserType")
            customerDB.addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError?) {
                    if (p0 != null) {
                        p0.message
                    }
                }

                override fun onDataChange(p0: DataSnapshot?) {
                    if (p0 != null) {
                        userTypeCustomer = p0.getValue(String::class.java)
                    }
                }

            })

            var driverDB = FirebaseDatabase.getInstance().getReference("Driver").child(userId)
            driverDB.addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError?) {
                    if (p0 != null) {
                        Toast.makeText(this@StartActivity, p0.message, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onDataChange(p0: DataSnapshot?) {
                    if (p0 != null) {
                        userTypeDriver = p0.child("UserType").getValue(String::class.java)
                        if(userTypeDriver == "Driver") {
                            driverAuthorized = p0.child("Auth").getValue(String::class.java)!!.toBoolean()
                            readyToTest = p0.child("ReadyToTest").getValue(String::class.java)!!.toBoolean()
                        }
                    }
                }
            })
            isReady = true
        }

        customerBtn.setOnClickListener {
            if(userId == null || userTypeCustomer.equals("Customer")) {
                val intent = Intent(this, CustomerLoginActivity::class.java)
                startActivity(intent)
                return@setOnClickListener
            } else if (userTypeCustomer == null) {
                Toast.makeText(this@StartActivity, "Driver 모드를 로그아웃 해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
        driverBtn.setOnClickListener {
            if (driverAuthorized == false && readyToTest == true){
                val intent = Intent(this@StartActivity, WaitingAuthActivity::class.java)
                startActivity(intent)
                finish()
                return@setOnClickListener
            } else if(userId == null || userTypeDriver.equals("Driver")) {
                val intent = Intent(this, DriverStartActivity::class.java)
                startActivity(intent)
                return@setOnClickListener
            }
            else if (userTypeDriver == null){
                Toast.makeText(this@StartActivity, "Customer 모드를 로그아웃 해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
        myRunnable = Runnable {
            kotlin.run {
                progressBar.visibility = View.INVISIBLE
            }
        }
        handler.postDelayed(myRunnable, 2000)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(myRunnable)
    }
}
