package kr.co.pirnardoors.pettaxikotlin.Controller

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_customer_register.*
import kr.co.pirnardoors.pettaxikotlin.Model.Customer
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.DRIVER_LOGON
import kr.co.pirnardoors.pettaxikotlin.Utilities.PREF_NAME
import org.jetbrains.anko.toast
import java.util.concurrent.TimeUnit

class CustomerRegisterActivity : AppCompatActivity() {

    // Database
    val customerDB = FirebaseDatabase.getInstance().getReference("Customer")

    //fragment

    val fragmentManager = supportFragmentManager

    val handler = Handler()
    // Log in
    val auth = FirebaseAuth.getInstance()
    var authStateListener: FirebaseAuth.AuthStateListener = FirebaseAuth.AuthStateListener { }
    lateinit var mCallback : PhoneAuthProvider.OnVerificationStateChangedCallbacks
    var phoneNumber : String? = ""
    var verificationCode : String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_register)

        mCallback = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
            override fun onVerificationCompleted(p0: PhoneAuthCredential?) {

            }

            override fun onVerificationFailed(p0: FirebaseException?) {

            }

            override fun onCodeSent(code: String?, p1: PhoneAuthProvider.ForceResendingToken?) {
                super.onCodeSent(code, p1)
                progressBar.visibility = View.GONE
                verificationCode = code!!
                Toast.makeText(this@CustomerRegisterActivity, "인증번호가 전송되었습니다.", Toast.LENGTH_SHORT).show()
                phoneNumberEditText.visibility = View.GONE
                textView46.visibility = View.GONE
                askCodeBtn.visibility = View.GONE
                verificationCodeEditText.visibility = View.VISIBLE
                verifyBtn.visibility = View.VISIBLE
                noticeTextView.text = "인증번호 6자리를 입력해주세요."

            }

        }

        askCodeBtn.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            phoneNumber = phoneNumberEditText.text.toString()
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    phoneNumber!!, 10, TimeUnit.SECONDS, this, mCallback
            )
        }
        verifyBtn.setOnClickListener {
            var sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            var editor = sharedPreferences.edit()
            progressBar.visibility = View.VISIBLE
            verifyBtn.visibility = View.GONE
            var input_code = verificationCodeEditText.text.toString()
            if(verificationCode != null) {
                var credential = PhoneAuthProvider.getCredential(verificationCode, input_code)
                if(credential != null) {
                    auth.signInWithCredential(credential).addOnCompleteListener {
                        if(it.isSuccessful) {
                            var userId = FirebaseAuth.getInstance().currentUser?.uid
                            customerDB.child(userId).child("UserType").setValue("Customer")
                            customerDB.child(userId).child("Profile").setValue("")
                            customerDB.child(userId).child("PhoneNumber").setValue(phoneNumber.toString())
                            val transaction = fragmentManager.beginTransaction()
                            val customerNickNameFragment = CustomerNickNameFragment()
                            transaction.replace(R.id.container, customerNickNameFragment)
                            transaction.commit()
                        } else {
                            toast("회원가입에 실패하였습니다.")
                        }
                    }
                }
            }


        }




//        authStateListener = FirebaseAuth.AuthStateListener {
//            var user = FirebaseAuth.getInstance().currentUser
//            if (user != null) {
//                finish()
//                return@AuthStateListener
//            }
//        }
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
