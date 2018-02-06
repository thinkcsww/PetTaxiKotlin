package kr.co.pirnardoors.pettaxikotlin.Controller

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_customer_register.*
import kr.co.pirnardoors.pettaxikotlin.R

class CustomerRegisterActivity : AppCompatActivity() {

    val auth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_register)




        completeBtn.setOnClickListener {
            val email = emailEditTxt.text.toString().trim()
            val password = passwordEditTxt.text.toString().trim()

            Log.d("Info", email)
            Log.d("Info", password)
            if(!email.isEmpty() && !password.isEmpty()) {
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if(task.isSuccessful) {
                        Toast.makeText(this, "성공.", Toast.LENGTH_SHORT).show()
                        var userId = FirebaseAuth.getInstance().currentUser?.uid
                        var database = FirebaseDatabase.getInstance().getReference("Customer")
                        database.child(userId).child("UserType").setValue("Customer")
                    } else {
                        Toast.makeText(this, "실패.", Toast.LENGTH_SHORT).show()

                    }
                }
            } else {
                Toast.makeText(this, "정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
