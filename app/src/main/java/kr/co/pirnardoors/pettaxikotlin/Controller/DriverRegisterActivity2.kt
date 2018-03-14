package kr.co.pirnardoors.pettaxikotlin.Controller

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_driver_register2.*
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.DRIVER_ID
import kr.co.pirnardoors.pettaxikotlin.Utilities.PREF_NAME
import java.util.*

class DriverRegisterActivity2 : AppCompatActivity() {
    val handler = Handler()
    var authStateListener = FirebaseAuth.AuthStateListener { }
    val auth = FirebaseAuth.getInstance()
    val calendar = Calendar.getInstance()
    var year = calendar.get(Calendar.YEAR).toString()
    var month = (calendar.get(Calendar.MONTH) + 1).toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_register2)
        var sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()

//        authStateListener = FirebaseAuth.AuthStateListener {
//            var user = FirebaseAuth.getInstance().currentUser
//            if (user != null) {
//                var intent = Intent(this, ViewRequestActivity::class.java)
//                startActivity(intent)
//            }
//        }

        completeBtn.setOnClickListener {
            var email = emailEditText.text.toString().trim()
            var password = passwordEditText.text.toString().trim()

            if(!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
                progressBar.visibility = View.VISIBLE
                handler.postDelayed(Runnable {
                    progressBar.visibility = View.GONE
                }, 2000)
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        editor.putString(DRIVER_ID, email)
                        editor.apply()
                        Toast.makeText(this, "회원가입에 성공하였습니다.",Toast.LENGTH_SHORT).show()
                        var userId = FirebaseAuth.getInstance().currentUser?.uid
                        var database = FirebaseDatabase.getInstance().getReference("Driver")
                        database.child(userId).child("UserType").setValue("Driver")
                        database.child(userId).child("Auth").setValue("false")
                        database.child(userId).child("Id").setValue(email)
                        database.child(userId).child("ReadyToTest").setValue("false")
                        database.child(userId).child("Departure").setValue("false")
                        database.child(userId).child("Profile").setValue("")
                        database.child(userId).child("TimeStamp").setValue("")
                        database.child(userId).child(year + month).child("DriveTime").setValue("0")
                        database.child(userId).child(year + month).child("Earn").setValue("0")
                        if(month == "1") {
                            database.child(userId).child((year.toInt() - 1).toString() + "12").child("DriveTime").setValue("0")
                            database.child(userId).child((year.toInt() - 1).toString() + "12").child("Earn").setValue("0")
                        } else {
                            database.child(userId).child(year + (month.toInt() - 1).toString()).child("DriveTime").setValue("0")
                            database.child(userId).child(year + (month.toInt() - 1).toString()).child("Earn").setValue("0")
                        }
                        val intent = Intent(this@DriverRegisterActivity2, DriverAuthorizingActivity::class.java)
                        startActivity(intent)
                        finish()
                        return@addOnCompleteListener
                    } else {
                        Toast.makeText(this, "회원가입에 실패하였습니다.",Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "정보를 입력해주세요.",Toast.LENGTH_SHORT).show()

            }
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
