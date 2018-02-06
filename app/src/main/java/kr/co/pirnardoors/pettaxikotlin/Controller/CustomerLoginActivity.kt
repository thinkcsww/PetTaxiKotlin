package kr.co.pirnardoors.pettaxikotlin.Controller

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_customer_login.*
import kr.co.pirnardoors.pettaxikotlin.Model.Customer
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.EXTRA_USERTYPE

class CustomerLoginActivity : AppCompatActivity() {

    val auth = FirebaseAuth.getInstance()
    var authStateListener : FirebaseAuth.AuthStateListener = FirebaseAuth.AuthStateListener{}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_login)

        authStateListener = FirebaseAuth.AuthStateListener {
            var user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val intent = Intent(this, CustomerMapActivity::class.java)
                startActivity(intent)
                finish()
                return@AuthStateListener
            }
        }



        registerBtn.setOnClickListener {
            var intent = Intent(this, CustomerRegisterActivity::class.java)
            startActivity(intent)
        }

        loginBtn.setOnClickListener {
            var email: String = emailEditTxt.text.toString().trim()
            var password: String = passwordEditTxt.text.toString().trim()
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this,"정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else {
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if(task.isSuccessful) {
                        Toast.makeText(this,"로그인이 성공하였습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this,"로그인에 실패하였습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
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
