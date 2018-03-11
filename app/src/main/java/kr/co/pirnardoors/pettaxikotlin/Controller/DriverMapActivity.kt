package kr.co.pirnardoors.pettaxikotlin.Controller

import android.app.AlertDialog
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.common.api.GoogleApi

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.internal.IGoogleMapDelegate
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.kakao.kakaonavi.Location
import com.kakao.kakaonavi.NaviOptions
import com.kakao.kakaonavi.options.CoordType
import com.kakao.kakaonavi.options.RpOption
import com.kakao.kakaonavi.options.VehicleType
import kotlinx.android.synthetic.main.activity_driver_map.*
import kr.co.pirnardoors.pettaxikotlin.Model.Request
import kr.co.pirnardoors.pettaxikotlin.R
import org.jetbrains.anko.alert
import org.jetbrains.anko.toast
import com.kakao.kakaonavi.KakaoNaviParams
import com.kakao.kakaonavi.KakaoNaviService
import kr.co.pirnardoors.pettaxikotlin.R.id.departureBtn
import kr.co.pirnardoors.pettaxikotlin.R.id.toDestinationBtn
import kr.co.pirnardoors.pettaxikotlin.Utilities.*
import java.io.IOException
import java.util.*


class DriverMapActivity : AppCompatActivity(), OnMapReadyCallback {

    var calendar = Calendar.getInstance()
    var year = calendar.get(Calendar.YEAR).toString()
    var month = (calendar.get(Calendar.MONTH) + 1).toString()
    val recordDB = FirebaseDatabase.getInstance().getReference("Record")
    lateinit var mMap: GoogleMap
    var distance = ""
    var wage = 0
    var driveTime = 0
    var earn = 0
    var timeStamp = ""
    var handler = Handler()
    var driverLocation : LatLng? = null
    var requestLocation : LatLng? = null
    var requestDestination : String? = ""
    var requestUserId = ""
    var driverUserId = FirebaseAuth.getInstance().currentUser!!.uid
    var destinationDatabase = FirebaseDatabase.getInstance().getReference("Request")
    var driverDB = FirebaseDatabase.getInstance().getReference("Driver")
    lateinit var myRunnable : Runnable
    var destinationLatitude : Double = 0.0
    var destinationLongitude : Double = 0.0
    var userId = FirebaseAuth.getInstance().currentUser?.uid
    var step3 = false
    var step2 = false
    var step1 = false
    var activityOn = true
    var customerToDestination = false
    val fragmentManager = supportFragmentManager
    lateinit var req : Request
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_map)
        reincarnation()
        var sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()
        /**
         *  step1 = Driver accept request
         *  step2 = Driver departure ; Driver is going to customer
         *  step3 = Driver is going to final destination
         */


        /**  Thread for check if driver is start to destination
         *   If flag is true -> get the information from the DB
         *   to calculate wage, distance, driverTimes, earn of this month.
         */
        val thread = Thread(object : Runnable{
            override fun run() {
                try {
                     while(activityOn == true) {
                         Thread.sleep(2000)
                         if (step1 == true && step2 == true) {
                             runOnUiThread(object : Runnable {
                                 override fun run() {
                                     getFinalInformationForDriver()
                                     if(customerToDestination == true && step3 == true) {
                                         showFinalAlertDialog()
                                     }
                                     Log.d("WHATTHEHELL", "나는 살아있다!!")
                                 }
                             })
                         }
                     }
                } catch (e: IOException) {
                    e.message
                }
            }

        }).start()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        //intent receive
        if(step1 == false) {
            req = intent.getParcelableExtra(EXTRA_REQUEST)
            driverUserId = FirebaseAuth.getInstance().currentUser!!.uid
            driverLocation = LatLng(req.driverLatitude, req.driverLongitude)
            requestLocation = LatLng(req.requestLatitude, req.requestLongitude)
            requestUserId = req.requestUserId
            editor.putString(DRIVER_MAP_REQUEST_USER_ID, requestUserId)
            editor.putString(DRIVERMAP_REQUEST_LATITUDE, req.requestLatitude.toString())
            editor.putString(DRIVERMAP_REQUEST_LONGITUDE, req.requestLongitude.toString())
            editor.putString(DRIVERMAP_DRIVER_LATITUDE, req.driverLatitude.toString())
            editor.putString(DRIVERMAP_DRIVER_LONGITUDE, req.driverLongitude.toString())
            editor.apply()
            requestDestination = req.requestDestination
        }
        if(step1 == true && step2 == false) {
            departureBtn.visibility = View.VISIBLE
            acceptBtn.visibility = View.GONE
        }
        if(step2 == true && step3 == false) {
            departureBtn.visibility = View.GONE
            toDestinationBtn.visibility = View.VISIBLE
            acceptBtn.visibility = View.GONE
        }
        if(step3 == true) {
            showFinalAlertDialog()
            return
        }

        button4.setOnClickListener {
            step1 = false
            editor.putBoolean(DRIVERMAP_STEP1, step1)
            editor.apply()
        }

        /**
         *  Get Destination LatLng
         */


        //First Visit of this activity -> Get info from DB or break using return
        if(destinationLatitude == 0.0) {
            if(step1 == true) return
            destinationDatabase.addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError?) {
                    if (p0 != null) {
                        p0.message
                    }
                }

                override fun onDataChange(p0: DataSnapshot?) {
                    var dataSnapshot = p0
                    if (dataSnapshot != null) {
                        destinationLatitude = dataSnapshot.child(requestUserId).child("DestinationLatitude").getValue().toString().toDouble()
                        destinationLongitude = dataSnapshot.child(requestUserId).child("DestinationLongitude").getValue().toString().toDouble()
                        customerToDestination = dataSnapshot.child(requestUserId).child("TD").getValue().toString().toBoolean()
                        editor.putBoolean(CUSTOMER_TO_DESTINATION, customerToDestination)
                        editor.putString(DESTINATION_LATITUDE, destinationLatitude.toString())
                        editor.putString(DESTINATION_LONGITUDE, destinationLongitude.toString())
                        editor.apply()
                    }
                }
            })
        }

        //accept button -> add data on customer request using customer userId

        acceptBtn.setOnClickListener {

            val simpleAlert = AlertDialog.Builder(this@DriverMapActivity, R.style.AlertDialogTheme).create()
            simpleAlert.setTitle("확인")
            simpleAlert.setMessage("정말 수락하시겠습니까?")

            //Yes Button
            simpleAlert.setButton(AlertDialog.BUTTON_POSITIVE, "네", {
                dialogInterface, i ->
                departureBtn.visibility = View.VISIBLE
                acceptBtn.visibility = View.INVISIBLE

//                acceptBtn.visibility = View.INVISIBLE
                var databaseCustomer = FirebaseDatabase.getInstance().getReference("Request").child(requestUserId)
                databaseCustomer.child("MD").setValue(driverUserId)
                step1 = true
                editor.putBoolean(DRIVERMAP_STEP1, step1)
                editor.apply()
                /*    val handler = Handler()
                    myRunnable = Runnable {
                        toDestinationBtn.visibility = View.VISIBLE
                        val options = NaviOptions.newBuilder()
                                .setCoordType(CoordType.WGS84)
                                .setVehicleType(VehicleType.FIRST)
                                .setRpOption(RpOption.SHORTEST).build()

                        val destination = Location.newBuilder("목적지", req.requestLongitude, req.requestLatitude).build()


                        // 경유지를 1개 포함하는 KakaoNaviParams.Builder 객체

                        val builder = KakaoNaviParams.newBuilder(destination)
                                .setNaviOptions(NaviOptions.newBuilder().setCoordType(CoordType.WGS84).build())

                        KakaoNaviService.shareDestination(this@DriverMapActivity, builder.build())
                        KakaoNaviService.navigate(this@DriverMapActivity, builder.build())
                        toDestinationBtn.visibility = View.VISIBLE
                        explainText.visibility = View.INVISIBLE
                        handler.removeCallbacks(myRunnable)
                    }
                    handler.postDelayed(myRunnable, 5000)
                    explainText.visibility = View.VISIBLE
                    acceptBtn.visibility = View.INVISIBLE
                    step2 = true
                    editor.putBoolean(DRIVERMAP_STEP2, step2)
                    editor.apply()*/

                //kakao

            })
            // No Button
            simpleAlert.setButton(AlertDialog.BUTTON_NEGATIVE, "아니오", {
                dialogInterface, i ->
                toast("취소되었습니다.")
            })

            simpleAlert.show()

        }

        // Departure Button -> 출발했음을 알림
        departureBtn.setOnClickListener {
            departureBtn.visibility = View.GONE
            val handler = Handler()
            //Write in DB that driver is departure
            destinationDatabase.child(requestUserId).child("DD").setValue("true")
            myRunnable = Runnable {
                toDestinationBtn.visibility = View.VISIBLE
                val options = NaviOptions.newBuilder()
                        .setCoordType(CoordType.WGS84)
                        .setVehicleType(VehicleType.FIRST)
                        .setRpOption(RpOption.SHORTEST).build()

                val destination = Location.newBuilder("목적지", requestLocation!!.longitude, requestLocation!!.latitude).build()


                // 경유지를 1개 포함하는 KakaoNaviParams.Builder 객체

                val builder = KakaoNaviParams.newBuilder(destination)
                        .setNaviOptions(NaviOptions.newBuilder().setCoordType(CoordType.WGS84).build())

                KakaoNaviService.shareDestination(this@DriverMapActivity, builder.build())
                KakaoNaviService.navigate(this@DriverMapActivity, builder.build())
                toDestinationBtn.visibility = View.VISIBLE
                explainText.visibility = View.INVISIBLE
                handler.removeCallbacks(myRunnable)
            }
            handler.postDelayed(myRunnable, 5000)
            explainText.visibility = View.VISIBLE
            step2 = true
            editor.putBoolean(DRIVERMAP_STEP2, step2)
            editor.apply()


        }
        // toDestination button

        toDestinationBtn.setOnClickListener {
            handler = Handler()
            editor.putString(DESTINATION_LONGITUDE, "0")
            editor.putString(DESTINATION_LATITUDE, "0")
            step3 = true
            editor.putBoolean(DRIVERMAP_STEP3, step3)
            editor.apply()

            val options = NaviOptions.newBuilder()
                    .setCoordType(CoordType.WGS84)
                    .setVehicleType(VehicleType.FIRST)
                    .setRpOption(RpOption.SHORTEST).build()

            val destination = Location.newBuilder("목적지", destinationLongitude, destinationLatitude).build()

            // 경유지를 1개 포함하는 KakaoNaviParams.Builder 객체

            val builder = KakaoNaviParams.newBuilder(destination)
                    .setNaviOptions(NaviOptions.newBuilder().setCoordType(CoordType.WGS84).build())

            KakaoNaviService.shareDestination(this@DriverMapActivity, builder.build())
            KakaoNaviService.navigate(this@DriverMapActivity, builder.build())



            myRunnable = Runnable {
                val transaction = fragmentManager.beginTransaction()
                val finishFragment = DriverFinishFragment()
                transaction.replace(R.id.fragmentHolder, finishFragment)
                transaction.addToBackStack(null)
                transaction.commit()
                toDestinationBtn.visibility = View.GONE
                handler.removeCallbacks(myRunnable)
            }

            handler.postDelayed(myRunnable, 2000)
            }

    } // Oncreate finish

    private fun getFinalInformationForDriver() {
        driverDB.child(driverUserId).addValueEventListener(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError?) {
                if (p0 != null) p0.message
            }
            override fun onDataChange(dataSnapshot: DataSnapshot?) {
                if (dataSnapshot != null) {
                    timeStamp = dataSnapshot.child("TimeStamp").getValue().toString()
                    Log.d("WHATTHE timestamp : ", timeStamp + "??")
                    driveTime = dataSnapshot.child(year + month).child("DriveTime").getValue().toString().toInt()
                    Log.d("WHATTHE driveTime ", driveTime.toString())
                    earn = dataSnapshot.child(year + month).child("Earn").getValue().toString().toInt()
                    if(timeStamp != "") {
                        customerToDestination = true
                        recordDB.addValueEventListener(object: ValueEventListener{
                            override fun onCancelled(p0: DatabaseError?) {
                                if (p0 != null) p0.message
                            }

                            override fun onDataChange(dataSnapshot: DataSnapshot?) {
                                if (dataSnapshot != null) {
                                    if(timeStamp != "") {
                                        wage = dataSnapshot.child(timeStamp).child("Wage").getValue().toString().toInt()
                                        distance = dataSnapshot.child(timeStamp).child("Distance").getValue().toString()
                                    }
                                }
                            }
                        })
                    }
                }
            }

        })
    }

    private fun showFinalAlertDialog() {
        val editor = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
        val finishAlertDialog = AlertDialog.Builder(this@DriverMapActivity)
        val finishDialogView = layoutInflater.inflate(R.layout.layout_driver_finish, null)
        finishAlertDialog.setView(finishDialogView)
        val okBtn: Button = finishDialogView.findViewById(R.id.okBtn)
        val distanceTextView : TextView = finishDialogView.findViewById(R.id.distanceTextView)
        val wageTextView : TextView = finishDialogView.findViewById(R.id.wageTextView)
        val driveTimesTextView : TextView = finishDialogView.findViewById(R.id.driveTimesTextView)
        val earnTextView : TextView = finishDialogView.findViewById(R.id.earnTextView)
        val dialog = finishAlertDialog.create()

        activityOn = false
        calendar = Calendar.getInstance()
        year = calendar.get(Calendar.YEAR).toString()
        month = (calendar.get(Calendar.MONTH) + 1).toString()

        distanceTextView.text = "운행거리 : ${distance}"
        wageTextView.text = "운행요금(수수료 처리 전) : ${wage}"
        driveTimesTextView.text = "이번 달 운행 수 : ${driveTime + 1}"
        earnTextView.text = "이번 달 소득 : ${earn + wage}"
        dialog.show()
        okBtn.setOnClickListener {

            driverDB.child(driverUserId).child(year + month).child("DriveTime").setValue(driveTime + 1)
            driverDB.child(driverUserId).child(year + month).child("Earn").setValue(earn + wage)
            driverDB.child(driverUserId).child("TimeStamp").setValue("")
            step1 = false; step2 = false; step3 = false; customerToDestination = false
            editor.putBoolean(DRIVERMAP_STEP1, step1)
            editor.putBoolean(DRIVERMAP_STEP2, step2)
            editor.putBoolean(DRIVERMAP_STEP3, step3)
            editor.putBoolean(CUSTOMER_TO_DESTINATION, customerToDestination)
            editor.putString(DRIVER_MAP_REQUEST_USER_ID, "")
            editor.apply()
            dialog.dismiss()
            val intent = Intent(this@DriverMapActivity, ViewRequestActivity::class.java)
            startActivity(intent)
            finish()
            return@setOnClickListener

        }

    }


    /**
         *  WebView kakaonavi
         */

    /*    val kakaoWebView = findViewById(R.id.webView) as WebView
        kakaoWebView!!.setWebViewClient(KakaoWebViewClient(this))
        val settings = webView.settings
        kakaoWebView.settings.javaScriptEnabled

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(webView, true)
        }

        val intent = intent
        val intentData = intent.data
        if (intentData == null) run {
        kakaoWebView.loadData("<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<meta charset=\"utf-8\"/>\n" +
                "<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\"/>\n" +
                "<meta name=\"viewport\" content=\"user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, width=device-width\"/>\n" +
                "<title>API Demo - Kakao JavaScript SDK</title>\n" +
                "<script src=\"//developers.kakao.com/sdk/js/kakao.min.js\"></script>\n" +
                "\n" +
                "</head>\n" +
                "<body>\n" +
                "<a id=\"navi\" href=\"#\" onclick=\"navi();\">\n" +
                "<img src=\"/assets/img/about/buttons/navi/kakaonavi_btn_medium.png\"/>\n" +
                "</a>\n" +
                "<script type='text/javascript'>\n" +
                "  //<![CDATA[\n" +
                "    // 사용할 앱의 JavaScript 키를 설정해 주세요.\n" +
                "    Kakao.init('ac61973c37daf8c5af4e99ee9b1e2caf');\n" +
                "    // 카카오 로그인 버튼을 생성합니다.\n" +
                "    function navi(){\n" +
                "        Kakao.Navi.start({\n" +
                "            name: \"현대백화점 판교점\",\n" +
                "            x: 127.11205203011632,\n" +
                "            y: 37.39279717586919,\n" +
                "            coordType: 'wgs84'\n" +
                "        });\n" +
                "    }\n" +
                "  //]]>\n" +
                "</script>\n" +
                "\n" +
                "</body>\n" +
                "</html>", "text/html; charset=utf-8", "UTF-8")
        }*/

        //Kakao navigation

//        val options = NaviOptions.newBuilder()
//                .setCoordType(CoordType.WGS84)
//                .setVehicleType(VehicleType.FIRST)
//                .setRpOption(RpOption.SHORTEST).build()
//
//        val destination = Location.newBuilder("목적지", req.requestLatitude, req.requestLongitude).build()
//        val builder = KakaoNaviParams.newBuilder(destination).setNaviOptions(options)



    private fun reincarnation() {
        var sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if(sharedPreferences.getString(DESTINATION_LATITUDE, "") == "" ||
                sharedPreferences.getString(DESTINATION_LONGITUDE, "") == "") {
            destinationLatitude = 0.0
            destinationLongitude = 0.0
        } else {
            destinationLatitude = sharedPreferences.getString(DESTINATION_LATITUDE, "").toDouble()
            destinationLongitude = sharedPreferences.getString(DESTINATION_LONGITUDE, "").toDouble()
            driverLocation = LatLng(
                    sharedPreferences.getString(DRIVERMAP_DRIVER_LATITUDE,"").toDouble(),
                    sharedPreferences.getString(DRIVERMAP_DRIVER_LONGITUDE,"").toDouble()
            )
            requestLocation = LatLng(
                    sharedPreferences.getString(DRIVERMAP_REQUEST_LATITUDE,"").toDouble(),
                    sharedPreferences.getString(DRIVERMAP_REQUEST_LONGITUDE,"").toDouble()
            )

            requestUserId = sharedPreferences.getString(DRIVER_MAP_REQUEST_USER_ID, "")
        }
        step1 = sharedPreferences.getBoolean(DRIVERMAP_STEP1, false)
        step2 = sharedPreferences.getBoolean(DRIVERMAP_STEP2, false)
        step3 = sharedPreferences.getBoolean(DRIVERMAP_STEP3, false)

        if(step2 == true) {
            toDestinationBtn.visibility = View.VISIBLE
            departureBtn.visibility = View.GONE
        } else if (step1 == true && step2 == false) {
            toDestinationBtn.visibility = View.GONE
            departureBtn.visibility = View.VISIBLE
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap


        // Double marker driver, and customer
        val mapLayout = findViewById(R.id.mapRelativeLayout) as RelativeLayout
        mapLayout.viewTreeObserver.addOnGlobalLayoutListener {
            val markers = ArrayList<Marker>()
            if (driverLocation != null && requestLocation != null) {
                markers.add(mMap.addMarker(MarkerOptions().position(driverLocation!!).title("Your Location")))
                markers.add(mMap.addMarker(MarkerOptions().position(requestLocation!!).title("Request Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))))
                val builder = LatLngBounds.Builder()
                for (marker in markers) {
                    builder.include(marker.position)

                }
                val bounds = builder.build()

                val padding = 100
                val cu = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                mMap.animateCamera(cu)
            } else {
                Toast.makeText(this@DriverMapActivity, "GPS 수신 에러입니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
            }
        }

/*
        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(requestLocation).title("당신의 위치입니다."))

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(requestLocation, 15f))*/
    }

    override fun onDestroy() {
        super.onDestroy()
        activityOn = false
    }


}
