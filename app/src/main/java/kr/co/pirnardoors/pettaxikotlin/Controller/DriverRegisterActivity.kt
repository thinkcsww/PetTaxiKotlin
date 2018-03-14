package kr.co.pirnardoors.pettaxikotlin.Controller

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_driver_register.*
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.DRIVER_LOGON
import kr.co.pirnardoors.pettaxikotlin.Utilities.PREF_NAME
import org.jetbrains.anko.toast
import java.util.*
import java.util.concurrent.TimeUnit

class DriverRegisterActivity : AppCompatActivity() {
    val driverDB = FirebaseDatabase.getInstance().getReference("Driver")
    var authStateListener : FirebaseAuth.AuthStateListener = FirebaseAuth.AuthStateListener{}
    lateinit var mCallback : PhoneAuthProvider.OnVerificationStateChangedCallbacks
    val auth = FirebaseAuth.getInstance()
    var verificationCode : String = ""
    val calendar = Calendar.getInstance()
    var year = calendar.get(Calendar.YEAR).toString()
    var month = (calendar.get(Calendar.MONTH) + 1).toString()
    val fragmentManager = supportFragmentManager
    var phoneNumber : String? = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_register)

        mCallback = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
            override fun onVerificationCompleted(p0: PhoneAuthCredential?) {

            }

            override fun onVerificationFailed(p0: FirebaseException?) {

            }

            override fun onCodeSent(code: String?, p1: PhoneAuthProvider.ForceResendingToken?) {
                super.onCodeSent(code, p1)
                progressBar.visibility = View.GONE
                verificationCode = code!!
                Toast.makeText(this@DriverRegisterActivity, "인증번호가 전송되었습니다.", Toast.LENGTH_SHORT).show()
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
                            toast("회원가입에 성공하였습니다.")
                            var userId = FirebaseAuth.getInstance().currentUser?.uid
                            driverDB.child(userId).child("PhoneNumber").setValue(phoneNumber)
                            driverDB.child(userId).child("UserType").setValue("Driver")
                            driverDB.child(userId).child("Auth").setValue("false")
                            driverDB.child(userId).child("ReadyToTest").setValue("false")
                            driverDB.child(userId).child("Departure").setValue("false")
                            driverDB.child(userId).child("Profile").setValue("")
                            driverDB.child(userId).child("TimeStamp").setValue("")
                            driverDB.child(userId).child(year + month).child("DriveTime").setValue("0")
                            driverDB.child(userId).child(year + month).child("Earn").setValue("0")
                            if(month == "1") {
                                driverDB.child(userId).child((year.toInt() - 1).toString() + "12").child("DriveTime").setValue("0")
                                driverDB.child(userId).child((year.toInt() - 1).toString() + "12").child("Earn").setValue("0")
                            } else {
                                driverDB.child(userId).child(year + (month.toInt() - 1).toString()).child("DriveTime").setValue("0")
                                driverDB.child(userId).child(year + (month.toInt() - 1).toString()).child("Earn").setValue("0")
                            }
                            val transaction = fragmentManager.beginTransaction()
                            val driverNickNameFragment = DriverNickNameFragment()
                            transaction.replace(R.id.container, driverNickNameFragment)
                            transaction.commit()
                        } else {
                            toast("회원가입에 실패하였습니다.")
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
