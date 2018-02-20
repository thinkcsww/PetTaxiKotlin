package kr.co.pirnardoors.pettaxikotlin.Controller

import android.app.AlertDialog
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.view.View
import android.view.ViewTreeObserver
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.RelativeLayout
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
import java.util.ArrayList
import com.kakao.kakaonavi.KakaoNaviParams
import com.kakao.kakaonavi.KakaoNaviService
import kr.co.pirnardoors.pettaxikotlin.Utilities.*


class DriverMapActivity : AppCompatActivity(), OnMapReadyCallback {

    lateinit var mMap: GoogleMap
    var driverLocation : LatLng? = null
    var requestLocation : LatLng? = null
    var requestDestination : String? = ""
    var destinationDatabase = FirebaseDatabase.getInstance().getReference("Request")
    lateinit var myRunnable : Runnable
    var destinationLatitude : Double = 0.0
    var destinationLongitude : Double = 0.0
    var step2 = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_map)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        //intent receive
        var req : Request = intent.getParcelableExtra(EXTRA_REQUEST)
        var driverUserId = FirebaseAuth.getInstance().currentUser?.uid
         driverLocation = LatLng(req.driverLatitude, req.driverLongitude)
         requestLocation = LatLng(req.requestLatitude, req.requestLongitude)
        var requestUserId = req.requestUserId

        var sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()
        requestDestination = req.requestDestination
        //var requestType = req.requestType
        var requestNumber = req.requestNumber

        /**
         *  Get Destination LatLng
         */
        step2 = sharedPreferences.getBoolean(DRIVERMAP_STEP2, false)
        if(step2 == true)reincarnation()
        destinationDatabase.addValueEventListener(object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError?) {
                if(p0 != null) {
                    p0.message
                }
            }

            override fun onDataChange(p0: DataSnapshot?) {
                var dataSnapshot = p0
                if (dataSnapshot != null) {
                    destinationLatitude = dataSnapshot.child(requestUserId).child("DestinationLatitude").getValue().toString().toDouble()
                    destinationLongitude = dataSnapshot.child(requestUserId).child("DestinationLongitude").getValue().toString().toDouble()
                    editor.putString(DESTINATION_LATITUDE, destinationLatitude.toString())
                    editor.putString(DESTINATION_LONGITUDE, destinationLongitude.toString())
                    editor.apply()
                }
            }
        })
        // toDestination button

        toDestinationBtn.setOnClickListener {
            val options = NaviOptions.newBuilder()
                    .setCoordType(CoordType.WGS84)
                    .setVehicleType(VehicleType.FIRST)
                    .setRpOption(RpOption.SHORTEST).build()

            val destination = Location.newBuilder("목적지", destinationLongitude, destinationLatitude).build()
            editor.putString(DESTINATION_LONGITUDE, "0")
            editor.putString(DESTINATION_LATITUDE, "0")
            editor.apply()

                // 경유지를 1개 포함하는 KakaoNaviParams.Builder 객체

            val builder = KakaoNaviParams.newBuilder(destination)
                    .setNaviOptions(NaviOptions.newBuilder().setCoordType(CoordType.WGS84).build())

            KakaoNaviService.shareDestination(this@DriverMapActivity, builder.build())
            KakaoNaviService.navigate(this@DriverMapActivity, builder.build())
            editor.putBoolean(DRIVERMAP_STEP2, false)
            editor.apply()
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





        //accept button -> add data on customer request using customer userId

        acceptBtn.setOnClickListener {

            val simpleAlert = AlertDialog.Builder(this@DriverMapActivity, R.style.AlertDialogTheme).create()
            simpleAlert.setTitle("확인")
            simpleAlert.setMessage("정말 수락하시겠습니까?")

            //Yes Button
            simpleAlert.setButton(AlertDialog.BUTTON_POSITIVE, "네", {
                dialogInterface, i ->
//                acceptBtn.visibility = View.INVISIBLE
                var databaseCustomer = FirebaseDatabase.getInstance().getReference("Request").child(requestUserId)
                databaseCustomer.child("MD").setValue(driverUserId)

                val handler = Handler()
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
                editor.apply()

                //kakao

            })
            // No Button
            simpleAlert.setButton(AlertDialog.BUTTON_NEGATIVE, "아니오", {
                dialogInterface, i ->
                toast("취소되었습니다.")
            })

            simpleAlert.show()

        }
    } //oncreate finish

    private fun reincarnation() {
        var sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()
        destinationLatitude = sharedPreferences.getString(DESTINATION_LATITUDE, "").toDouble()
        destinationLongitude = sharedPreferences.getString(DESTINATION_LONGITUDE, "").toDouble()
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
}
