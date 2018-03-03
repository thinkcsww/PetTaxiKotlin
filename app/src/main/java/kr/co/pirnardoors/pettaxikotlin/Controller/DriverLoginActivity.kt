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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_driver_login.*
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.DRIVER_ID
import kr.co.pirnardoors.pettaxikotlin.Utilities.DRIVER_LICENSE_AUTHORIZED
import kr.co.pirnardoors.pettaxikotlin.Utilities.PREF_NAME

class DriverLoginActivity : AppCompatActivity() {
    val auth = FirebaseAuth.getInstance()
    var authStateListener = FirebaseAuth.AuthStateListener {  }
    val handler = Handler()
    var driverAuthorized = false
    var userId = FirebaseAuth.getInstance().currentUser?.uid
    var driverDB = FirebaseDatabase.getInstance().getReference("Driver")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_login)
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

        authStateListener = FirebaseAuth.AuthStateListener {
            var user = FirebaseAuth.getInstance().currentUser

            if (user != null) {
                if(driverAuthorized == true) {
                    val intent = Intent(this, ViewRequestActivity::class.java)
                    startActivity(intent)
                    progressBar.visibility = View.GONE
                    finish()
                    return@AuthStateListener
                } else {
                    val intent = Intent(this@DriverLoginActivity, BlockActivity::class.java)
                    startActivity(intent)
                    finish()
                    return@AuthStateListener
                }
            }
        }

        loginBtn.setOnClickListener {
            var email = emailEditText.text.toString().trim()
            var password = passwordEditText.text.toString().trim()
            if(!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
                progressBar.visibility = View.VISIBLE
                handler.postDelayed(Runnable {
                    progressBar.visibility = View.GONE
                }, 2000)
                handler.removeCallbacksAndMessages(null)
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "로그인 되었습니다.", Toast.LENGTH_SHORT).show()
                        editor.putString(DRIVER_ID, email)
                        editor.apply()
                    } else {
                        Toast.makeText(this, "로그인이 실패하였습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
        registerBtn.setOnClickListener {
//            var intent = Intent(this, DriverAuthorizingActivity::class.java)
//            startActivity(intent)
            var intent = Intent(this, DriverRegisterActivity::class.java)
            startActivity(intent)
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
