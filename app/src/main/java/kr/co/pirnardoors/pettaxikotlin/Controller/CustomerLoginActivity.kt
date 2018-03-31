package kr.co.pirnardoors.pettaxikotlin.Controller

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat.startActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_customer_login.*
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.PREF_NAME
import org.jetbrains.anko.toast
import java.util.concurrent.TimeUnit

class CustomerLoginActivity : AppCompatActivity() {
    val handler = Handler()

    // login

    val auth = FirebaseAuth.getInstance()
    var authStateListener : FirebaseAuth.AuthStateListener = FirebaseAuth.AuthStateListener{}
    lateinit var mCallback : PhoneAuthProvider.OnVerificationStateChangedCallbacks
    var phoneNumber : String? = ""
    var verificationCode : String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_login)

        mCallback = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
            override fun onVerificationCompleted(p0: PhoneAuthCredential?) {

            }

            override fun onVerificationFailed(p0: FirebaseException?) {

            }

            override fun onCodeSent(code: String?, p1: PhoneAuthProvider.ForceResendingToken?) {
                super.onCodeSent(code, p1)
                progressBar.visibility = View.GONE
                verificationCode = code!!
                Toast.makeText(this@CustomerLoginActivity, "인증번호가 전송되었습니다.", Toast.LENGTH_SHORT).show()
                phoneNumberEditText.visibility = View.GONE
                textView46.visibility = View.GONE
                askCodeBtn.visibility = View.GONE
                verificationCodeEditText.visibility = View.VISIBLE
                verifyBtn.visibility = View.VISIBLE
                registerBtn.visibility = View.GONE
                noticeTextView.text = "인증번호 6자리를 입력해주세요."

            }

        }

        authStateListener = FirebaseAuth.AuthStateListener {
            var user = FirebaseAuth.getInstance().currentUser
            if(progressBar.isActivated) {
                progressBar.visibility = View.GONE
            }
            if (user != null) {
//                val intent = Intent(this, MeetActivity::class.java)
//                startActivity(intent)
                val intent = Intent(this, CustomerMapActivity::class.java)
                startActivity(intent)
                finish()
                return@AuthStateListener
            }
        }

        askCodeBtn.setOnClickListener {
            phoneNumber = phoneNumberEditText.text.toString()
            if(TextUtils.isEmpty(phoneNumberEditText.text)) {
                toast("번호를 입력해주세요.")
            } else {
                progressBar.visibility = View.VISIBLE
                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                        phoneNumber!!, 10, TimeUnit.SECONDS, this, mCallback
                )
            }
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
                            progressBar.visibility = View.GONE
                            toast("로그인 되었습니다.")
                            val intent = Intent(this@CustomerLoginActivity, CustomerMapActivity::class.java)
                            startActivity(intent)
                            finish()
                            return@addOnCompleteListener
                        } else {
                            toast("회원가입에 실패하였습니다.")
                        }
                    }
                }
            }
        }

        registerBtn.setOnClickListener {
            var intent = Intent(this, CustomerRegisterActivity::class.java)
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