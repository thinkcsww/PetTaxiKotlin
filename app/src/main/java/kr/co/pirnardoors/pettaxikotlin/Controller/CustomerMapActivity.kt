package kr.co.pirnardoors.pettaxikotlin.Controller

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_customer_map.*
import kr.co.pirnardoors.pettaxikotlin.R

class CustomerMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    var locationManager : LocationManager? = null
    var locationListener : LocationListener? = null
    lateinit var lastKnownLocation : Location
    var requestActive : Boolean = false
    var driverActive = false
    var userId = FirebaseAuth.getInstance().currentUser?.uid
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_map)
        val auth = FirebaseAuth.getInstance()
        val database = FirebaseDatabase.getInstance().getReference("Request")
        val geoFire = GeoFire(database)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        //call catcardog Button

        callBtn.setOnClickListener {
            if(lastKnownLocation != null) {
                if (requestActive == false) {
                    requestActive = true
                    callBtn.setText("취소하기")
                    geoFire.setLocation(userId, GeoLocation(lastKnownLocation.latitude, lastKnownLocation.longitude))
                } else {
                    requestActive = false
                    callBtn.setText("캣카독 부르기")
                    geoFire.removeLocation(userId, GeoFire.CompletionListener { key, error ->
                        if (error == null) {
                            Toast.makeText(this@CustomerMapActivity, "취소되었습니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            error.message
                        }
                    })
                }
            }
        }


        //Log out
        logoutBtn.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return@setOnClickListener
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
        locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)

        lastKnownLocation = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if(lastKnownLocation != null) {
            var userLocation = LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)
            mMap.clear()
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
            mMap.addMarker(MarkerOptions().position(userLocation).title("Your Location"))
        }

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
