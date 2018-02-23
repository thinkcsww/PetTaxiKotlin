package kr.co.pirnardoors.pettaxikotlin.Controller

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.kakao.kakaonavi.KakaoNaviWebViewActivity
import kotlinx.android.synthetic.main.activity_main.*
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.*
import org.jetbrains.anko.share
import org.jetbrains.anko.toast
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity() {

    var transportActive : Boolean = false
    var customerMapActive : Boolean = false
    var meetActivityActive : Boolean = false
    var driverLogon = false
    var customerLogon = false
    var driverAuthorized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()
        driverLogon = sharedPreferences.getBoolean(DRIVER_LOGON, false)
        customerLogon = sharedPreferences.getBoolean(CUSTOMER_LOGON, false)
        transportActive = sharedPreferences.getBoolean(TRANSPORT_ACTIVE, false)
        customerMapActive = sharedPreferences.getBoolean(REQUEST_ACTIVE, false)
        meetActivityActive = sharedPreferences.getBoolean(MEET_ACTIVITY_ACTIVE, false)
        driverAuthorized = sharedPreferences.getBoolean(DRIVER_LICENSE_AUTHORIZED, false)
        if(transportActive == true || meetActivityActive == true ) {
            val intent = Intent(this@MainActivity, MeetActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        if(customerMapActive == true) {
            val intent = Intent(this@MainActivity, CustomerMapActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        if(FirebaseAuth.getInstance().currentUser != null) {
            if(driverLogon == true && driverAuthorized == true){
                val intent = Intent(this@MainActivity, ViewRequestActivity::class.java)
                startActivity(intent)
                finish()
                return
            } else if (customerLogon == true) {7
                val intent = Intent(this@MainActivity, CustomerMapActivity::class.java)
                startActivity(intent)
                finish()
                return
            }
//            else if (driverLogon == true && driverAuthorized == false) {
//                val intent = Intent(this@MainActivity, BlockActivity::class.java)
//                startActivity(intent)
//                finish()
//                return
//            }
        }

        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ){
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 100)
        }



        startBtn.setOnClickListener {
//            var intent = Intent(this, StartActivity::class.java)
            val intent = Intent(this@MainActivity, StartActivity::class.java)
            startActivity(intent)
            finish()
            return@setOnClickListener

        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this@MainActivity, "위치정보 권한이 주어졌습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "위치정보 권한이 주어지지 않았습니다. 설정을 변경해주세요.", Toast.LENGTH_LONG).show()
            }
        }
    }

}
