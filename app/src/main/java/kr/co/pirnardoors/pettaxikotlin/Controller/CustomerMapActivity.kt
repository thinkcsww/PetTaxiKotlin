package kr.co.pirnardoors.pettaxikotlin.Controller

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_customer_map.*
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.R.id.callBtn
import kr.co.pirnardoors.pettaxikotlin.R.id.matchText
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import kotlin.concurrent.thread

class CustomerMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    var locationManager : LocationManager? = null
    var locationListener : LocationListener? = null
    lateinit var lastKnownLocation : Location
    var requestActive : Boolean = false
    var driverActive = false
    var userId = FirebaseAuth.getInstance().currentUser?.uid
    var driverUserId : String? = ""
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().getReference("Request")
    val geoFire = GeoFire(database)
    val driverUserIdForRecord = driverUserId
    private var carNumber : String? = null
    private var phoneNumber : String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_map)




        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //Thread to check if drvier is matched


            var thread = Thread(object : Runnable {
                override fun run() {
                    try {
                        while (!Thread.interrupted()) {
                            Thread.sleep(3000)
                            if (requestActive == true){
                                runOnUiThread(object : Runnable {
                                    override fun run() {
                                        if (driverUserId != "") {
                                            driverActive = true
                                            getDriverInformation()
                                        }
                                        Log.d("InfoMaiton", "나는 살아있따!!!")
                                    }
                                })
                        }
                        }
                    } catch (e: InterruptedException) {
                        e.message
                    }

                }
            }).start()




        //call catcardog Button

        callBtn.setOnClickListener {
            if(lastKnownLocation != null) {
                if (requestActive == false) {
                    requestActive = true
                    callBtn.setText("취소하기")
                    geoFire.setLocation(userId, GeoLocation(lastKnownLocation.latitude, lastKnownLocation.longitude))
                    database.child(userId).child("MD").setValue("")
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

    /**
     *  When matching is done get driver's location and erase the request.
     */
    fun getDriverInformation() {
        callBtn.visibility = View.INVISIBLE
        matchText.visibility = View.VISIBLE
        var driverDatabase = FirebaseDatabase.getInstance().getReference("Respond").child(driverUserId)
        driverDatabase.child("l").addValueEventListener(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError?) {
                if (p0 != null) {
                    p0.message
                }
            }
            override fun onDataChange(p0: DataSnapshot?) {
                var dataSnapshot = p0
                if (dataSnapshot != null) {

                    var driverLatitude = dataSnapshot.child("0").getValue()
                    var driverLongitude = dataSnapshot.child("1").getValue()
                    var destination = Location("Destination")
                    destination.latitude = driverLatitude.toString().toDouble()
                    destination.longitude = driverLongitude.toString().toDouble()
                    var distanceKm = lastKnownLocation.distanceTo(destination) / 1000
                    var distanceRound = Math.round(distanceKm)

                    if(distanceRound < 0.01) {
                        var handler = Handler()
                        handler.postDelayed(Runnable {
                            callBtn.setVisibility(View.VISIBLE)
                            callBtn.setText("캣카독 부르기")
                            matchText.text = "Driver가 도착하였습니다."
                            requestActive = false
                            driverActive = false
                            updateMap(lastKnownLocation)
                            var driverDb = FirebaseDatabase.getInstance().getReference("Driver").child(driverUserId)
                            driverDb.addValueEventListener(object : ValueEventListener {
                                override fun onCancelled(p0: DatabaseError?) {
                                    if (p0 != null) {
                                        p0.message
                                    }
                                }

                                override fun onDataChange(p0: DataSnapshot?) {
                                    var dataSnapshot = p0
                                    if (dataSnapshot != null) {
                                        carNumber = dataSnapshot.child("CarNumber").getValue().toString()
                                        phoneNumber = dataSnapshot.child("PhoneNumber").getValue().toString()
                                    }
                                }

                            })


                            var recordDB = FirebaseDatabase.getInstance().getReference("Record")
                            recordDB.child("customerId").setValue(userId)
                            if(driverUserId != "null") {
                                recordDB.child("driverId").setValue(driverUserId)
                            }
                            recordDB.child("carNumber").setValue(carNumber)
                            recordDB.child("phoneNumber").setValue(phoneNumber)

                            matchText.visibility = View.INVISIBLE

                            geoFire.removeLocation(userId)
                        }, 5000)



                    } else {
                        matchText.text = "Driver와 ${distanceRound.toString()}km 거리입니다."
                    }
                }
            }
        })

    }






    // oncreate finish

    /**
     *  Check if driver is matched using Child Md is null or not
     */
    fun checkIfDriverIsMatched() {
        var databaseForMatching = FirebaseDatabase.getInstance().getReference("Request").child(userId).child("MD")
        databaseForMatching.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError?) {
                if (p0 != null) {
                    p0.message
                }
            }
            override fun onDataChange(p0: DataSnapshot?) {
                var dataSnapshot = p0
                if (dataSnapshot != null) {
                    driverUserId = dataSnapshot.getValue().toString()

                }
            }

        })
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
                checkIfDriverIsMatched()
                Log.i("Info", driverUserId)
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
