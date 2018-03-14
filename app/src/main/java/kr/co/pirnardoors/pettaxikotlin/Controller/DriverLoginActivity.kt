package kr.co.pirnardoors.pettaxikotlin.Controller

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.android.synthetic.main.activity_driver_login.*
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.DRIVER_LOGON
import kr.co.pirnardoors.pettaxikotlin.Utilities.PREF_NAME
import org.jetbrains.anko.toast
import java.util.concurrent.TimeUnit

class DriverLoginActivity : AppCompatActivity() {

    val auth = FirebaseAuth.getInstance()
    lateinit var mCallback : PhoneAuthProvider.OnVerificationStateChangedCallbacks
    var authStateListener : FirebaseAuth.AuthStateListener = FirebaseAuth.AuthStateListener{}
    var verificationCode : String = ""
    var phoneNumber : String? = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_login)

        mCallback = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
            override fun onVerificationCompleted(p0: PhoneAuthCredential?) {

            }

            override fun onVerificationFailed(p0: FirebaseException?) {

            }

            override fun onCodeSent(code: String?, p1: PhoneAuthProvider.ForceResendingToken?) {
                super.onCodeSent(code, p1)
                progressBar.visibility = View.GONE
                verificationCode = code!!
                Toast.makeText(this@DriverLoginActivity, "인증번호가 전송되었습니다.", Toast.LENGTH_SHORT).show()
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
                    phoneNumber!!, 10, TimeUnit.SECONDS, this, mCallback)

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
                            editor.putBoolean(DRIVER_LOGON, true)
                            editor.apply()
                            progressBar.visibility = View.GONE
                            toast("로그인 성공")
                            val intent = Intent(this@DriverLoginActivity, ViewRequestActivity::class.java)
                            startActivity(intent)
                            finish()
                            finish()
                            return@addOnCompleteListener

                        } else {
                            toast("로그인에 실패하였습니다.")
                        }
                    }
                }
            }


        }
    }

    fun signInWithPhone(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener {
                    Toast.makeText(this, "성공", Toast.LENGTH_SHORT).show()
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
