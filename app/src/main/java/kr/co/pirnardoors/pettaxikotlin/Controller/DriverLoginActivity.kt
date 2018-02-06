package kr.co.pirnardoors.pettaxikotlin.Controller

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_driver_login.*
import kr.co.pirnardoors.pettaxikotlin.R

class DriverLoginActivity : AppCompatActivity() {
    val auth = FirebaseAuth.getInstance()
    var authStateListener = FirebaseAuth.AuthStateListener {  }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_login)
        authStateListener = FirebaseAuth.AuthStateListener {
            var user = FirebaseAuth.getInstance().currentUser

            if (user != null) {
                var intent = Intent(this, ViewRequestActivity::class.java)
                startActivity(intent)
                finish()
                return@AuthStateListener
            }
        }

        loginBtn.setOnClickListener {
            var email = emailEditText.text.toString().trim()
            var password = passwordEditText.text.toString().trim()
            if(!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "로그인 되었습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "로그인이 실패하였습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "오류가 발생하였습니다.", Toast.LENGTH_SHORT).show()
            }
        }
        registerBtn.setOnClickListener {
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
