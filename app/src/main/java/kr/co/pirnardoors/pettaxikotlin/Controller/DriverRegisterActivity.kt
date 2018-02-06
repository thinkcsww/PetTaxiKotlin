package kr.co.pirnardoors.pettaxikotlin.Controller

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_driver_register.*
import kr.co.pirnardoors.pettaxikotlin.R

class DriverRegisterActivity : AppCompatActivity() {

    var authStateListener = FirebaseAuth.AuthStateListener { }
    val auth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_register)

        authStateListener = FirebaseAuth.AuthStateListener {
            var user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                var intent = Intent(this, ViewRequestActivity::class.java)
                startActivity(intent)
            }
        }

        completeBtn.setOnClickListener {
            var email = emailEditText.text.toString().trim()
            var password = passwordEditText.text.toString().trim()
            var phoneNumber = phoneEditText.text.toString().trim()
            var carNumber = carEditText.text.toString().trim()

            if(!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password) &&
               !TextUtils.isEmpty(phoneNumber) && !TextUtils.isEmpty(carNumber)) {

                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "회원가입에 성공하였습니다.",Toast.LENGTH_SHORT).show()
                        var userId = FirebaseAuth.getInstance().currentUser?.uid
                        var database = FirebaseDatabase.getInstance().getReference("Driver")
                        database.child(userId).child("UserType").setValue("Driver")
                        database.child(userId).child("CarNumber").setValue(carNumber)
                        database.child(userId).child("PhoneNumber").setValue(phoneNumber)
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
