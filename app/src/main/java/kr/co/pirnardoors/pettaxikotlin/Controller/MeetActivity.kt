package kr.co.pirnardoors.pettaxikotlin.Controller

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_meet.*
import kr.co.pirnardoors.pettaxikotlin.R
import org.jetbrains.anko.toast
import java.io.IOException

class MeetActivity : AppCompatActivity(), OnMapReadyCallback{

    private var mainWebView: WebView? = null
    private val APP_SCHEME = "iamporttest://"

    val database = FirebaseDatabase.getInstance().getReference("Request")
    var locationManager : LocationManager? = null
    var locationListener : LocationListener? = null
    var distanceFromDestinationM : Double? = null
    var destinationLatitude : Double? = null
    var destinationLongitude : Double? = null
    var currentPosition : Location? = null
    var mapIsReady = false
    lateinit var mMap : GoogleMap
    lateinit var lastKnownLocation : Location
    lateinit var currentLocation : Location
    lateinit var previousLocation : Location
    private var userId = FirebaseAuth.getInstance().currentUser?.uid
    val customerDB = FirebaseDatabase.getInstance().getReference("Request").child(userId)
    var transportActive = false
    var distance : Double = 0.0
    var wage : Int = 5000
    val TAG = "MeetActivity"
    var check = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_meet)


        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val destination = intent.getStringExtra("Destination")
        destinationText.text = destination
        askArrivalBtn.setOnClickListener {
            askArrival()
        }
        startBtn.setOnClickListener {
            startBtn.visibility = View.GONE
            transportActive = true
        }

        receiveDestinationInfo()
        //update driving distance by 2seconds
        val thread = Thread(object : Runnable {
            override fun run() {
                try {
                    while (!Thread.interrupted()) {
                        if(mapIsReady)previousLocation = lastKnownLocation
                        Thread.sleep(3000)
                        if (transportActive == true){
                            runOnUiThread(object : Runnable {
                                override fun run() {

                                    movingDistance()
                                    distanceFromDestinationM = lastKnownLocation.distanceTo(currentPosition).toDouble()
                                    if(distanceFromDestinationM!! < 100) {
                                        askArrivalBtn.visibility = View.VISIBLE
                                    }
                                    Log.d(TAG, "나는 살아있따!!!")
                                }
                            })
                        }
                    }
                } catch (e: InterruptedException) {
                    e.message
                }

            }
        }).start()

    }
    //oncreate finish

    private fun receiveDestinationInfo() {
        customerDB.addValueEventListener(object : ValueEventListener{
            override fun onCancelled(err: DatabaseError?) {
                if(err != null) {
                    err.message
                }
            }
            override fun onDataChange(dataSnapshot: DataSnapshot?) {
                if(dataSnapshot != null) {
                    destinationLatitude = dataSnapshot.child("DestinationLatitude").getValue().toString().toDouble()
                    destinationLongitude = dataSnapshot.child("DestinationLongitude").getValue().toString().toDouble()
                    currentPosition  = Location("currentPosition")
                    currentPosition?.latitude = destinationLatitude.toString().toDouble()
                    currentPosition?.longitude = destinationLongitude.toString().toDouble()

                }
            }
        })

    }


    private fun askArrival() {
        val simpleAlert = AlertDialog.Builder(this@MeetActivity, R.style.AlertDialogTheme).create()
        simpleAlert.setTitle("확인")
        simpleAlert.setMessage("정말 수락하시겠습니까?")

        //Yes Button
        simpleAlert.setButton(AlertDialog.BUTTON_POSITIVE, "네", {
            dialogInterface, i ->
            transportActive = false
            kakaoPay()

        })
        // No Button
        simpleAlert.setButton(AlertDialog.BUTTON_NEGATIVE, "아니오", {
            dialogInterface, i ->
            toast("취소되었습니다.")
        })

        simpleAlert.show()
    }
    private fun kakaoPay() {
        mainWebView = findViewById(R.id.mainWebView) as WebView
        mainWebView!!.setWebViewClient(KakaoWebViewClient(this))
        val settings = mainWebView!!.getSettings()
        settings!!.javaScriptEnabled = true


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(mainWebView, true)
        }

        val intent = intent
        val intentData = intent.data

        if (intentData == null) run {
            mainWebView!!.loadData("<!DOCTYPE html>\n" +
                    "<!-- 아임포트 자바스크립트는 jQuery 기반으로 개발되었습니다 -->\n" +
                    "<script type=\"text/javascript\" src=\"https://code.jquery.com/jquery-1.12.4.min.js\" ></script>\n" +
                    "<script type=\"text/javascript\" src=\"https://service.iamport.kr/js/iamport.payment-1.1.5.js\" ></script>\n" +
                    "\n" +
                    "<script type=\"text/javascript\">\n" +
                    "var IMP = window.IMP; // 생략가능\n" +
                    "IMP.init('imp03841103'); // 'iamport' 대신 부여받은 \"가맹점 식별코드\"를 사용\n" +
                    "\n" +
                    "/* 중략 */\n" +
                    "\n" +
                    "//onclick, onload 등 원하는 이벤트에 호출합니다\n" +
                    "IMP.request_pay({\n" +
                    "    pg : 'inicis', // version 1.1.0부터 지원.\n" +
                    "    pay_method : 'card',\n" +
                    "    merchant_uid : 'merchant_' + new Date().getTime(),\n" +
                    "    name : '캣카톡 결제',\n" +
                    "    amount : $wage,\n" +
                    "    buyer_email : 'iamport@siot.do',\n" +
                    "    buyer_name : '구매자이름',\n" +
                    "    buyer_tel : '010-1234-5678',\n" +
                    "    buyer_addr : '서울특별시 강남구 삼성동',\n" +
                    "    buyer_postcode : '123-456',\n" +
                    "    m_redirect_url : 'https://www.yourdomain.com/payments/complete',\n" +
                    "    app_scheme : 'iamportapp'\n" +
                    "}, function(rsp) {\n" +
                    "    if ( rsp.success ) {\n" +
                    "        var msg = '결제가 완료되었습니다.';\n" +
                    "        msg += '고유ID : ' + rsp.imp_uid;\n" +
                    "        msg += '상점 거래ID : ' + rsp.merchant_uid;\n" +
                    "        msg += '결제 금액 : ' + rsp.paid_amount;\n" +
                    "        msg += '카드 승인번호 : ' + rsp.apply_num;\n" +
                    "    } else {\n" +
                    "        var msg = '결제에 실패하였습니다.';\n" +
                    "        msg += '에러내용 : ' + rsp.error_msg;\n" +
                    "    }\n" +
                    "\n" +
                    "    alert(msg);\n" +
                    "});\n" +
                    "</script>\n", "text/html; charset=utf-8", "UTF-8")
        } else {
            //isp 인증 후 복귀했을 때 결제 후속조치
            val url = intentData.toString()
            if (url.startsWith(APP_SCHEME)) {
                val redirectURL = url.substring(APP_SCHEME.length + 3)
                mainWebView!!.loadUrl(redirectURL)
            }
        }


    }

    private fun movingDistance() {

        if(lastKnownLocation != null) {
            if(ContextCompat.checkSelfPermission(this@MeetActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this@MeetActivity, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                return
            }
            lastKnownLocation = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            currentLocation = lastKnownLocation
            var distanceKm = currentLocation.distanceTo(previousLocation)
            distance += distanceKm
            distanceText.text = "이동거리 : ${(distance).toString()}m"
            wage = (distance.toInt()/100) * 100 + 5000
            wageText.text = "요금 : ${wage.toString()}원"
            Log.d(TAG + "이동거리-", distance.toString())
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if(locationManager == null) {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }

        locationListener = object :LocationListener{
            override fun onLocationChanged(p0: Location?) {
                var location = p0
                updateMap(location)
//                lastKnownLocation = location as Location
            }

            override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {

            }

            override fun onProviderEnabled(p0: String?) {
            }

            override fun onProviderDisabled(p0: String?) {
            }


        }

        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return
        }
        if(locationManager != null) {
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
            lastKnownLocation = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (lastKnownLocation != null) {
                var userLocation = LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)
                mMap.clear()
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                mMap.addMarker(MarkerOptions().position(userLocation).title("Your Location"))
            }
        }
        mapIsReady = true



    }

    fun updateMap(location : Location?) {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return
        }
        locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)

        var lastKnownLocation = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if(lastKnownLocation != null) {
            var userLocation = LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)

            mMap.clear()
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
            mMap.addMarker(MarkerOptions().position(userLocation).title("Your Location"))
        }


    }
}
