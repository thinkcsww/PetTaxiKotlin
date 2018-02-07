package kr.co.pirnardoors.pettaxikotlin.Controller

import android.app.AlertDialog
import android.app.PendingIntent.getActivity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.RelativeLayout
import android.widget.Toast

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.kakao.kakaonavi.Location
import com.kakao.kakaonavi.NaviOptions
import com.kakao.kakaonavi.options.CoordType
import com.kakao.kakaonavi.options.RpOption
import com.kakao.kakaonavi.options.VehicleType
import kotlinx.android.synthetic.main.activity_driver_map.*
import kr.co.pirnardoors.pettaxikotlin.Model.Request
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.EXTRA_REQUEST
import org.jetbrains.anko.alert
import org.jetbrains.anko.toast
import java.util.ArrayList
import com.kakao.kakaonavi.KakaoNaviParams
import com.kakao.kakaonavi.KakaoNaviService


class DriverMapActivity : AppCompatActivity(), OnMapReadyCallback {

    lateinit var mMap: GoogleMap
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
        var driverLocation = LatLng(req.driverLatitude, req.driverLongitude)
        var requestLocation = LatLng(req.requestLatitude, req.requestLongitude)
        var requestUserId = req.requestUserId

        //Kakao navigation
   /*     val options = NaviOptions.newBuilder()
                .setCoordType(CoordType.WGS84)
                .setVehicleType(VehicleType.FIRST)
                .setRpOption(RpOption.SHORTEST).build()

        val destination = Location.newBuilder("목적지", req.requestLatitude, req.requestLongitude).build()
        val builder = KakaoNaviParams.newBuilder(destination).setNaviOptions(options)*/




        // Double marker driver, and customer
        val mapLayout = findViewById(R.id.mapRelativeLayout) as RelativeLayout
        mapLayout.viewTreeObserver.addOnGlobalLayoutListener {
            val markers = ArrayList<Marker>()
            if (driverLocation != null && requestLocation != null) {
                markers.add(mMap.addMarker(MarkerOptions().position(driverLocation).title("Your Location")))
                markers.add(mMap.addMarker(MarkerOptions().position(requestLocation).title("Request Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))))
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

        //accept button -> add data on customer request using customer userId

        acceptBtn.setOnClickListener {

            val simpleAlert = AlertDialog.Builder(this@DriverMapActivity, R.style.AlertDialogTheme).create()
            simpleAlert.setTitle("확인")
            simpleAlert.setMessage("정말 수락하시겠습니까?")

            //Yes Button
            simpleAlert.setButton(AlertDialog.BUTTON_POSITIVE, "네", {
                dialogInterface, i ->
                acceptBtn.visibility = View.INVISIBLE
                var database = FirebaseDatabase.getInstance().getReference("Request").child(requestUserId).child("MD")
                database.setValue(driverUserId)
                //kakao
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

                finish()
                return@setButton
            })
            // No Button
            simpleAlert.setButton(AlertDialog.BUTTON_NEGATIVE, "아니오", {
                dialogInterface, i ->
                toast("취소되었습니다.")
            })

            simpleAlert.show()

        }
    } //oncreate finish

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

/*
        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(requestLocation).title("당신의 위치입니다."))

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(requestLocation, 15f))*/
    }
}
