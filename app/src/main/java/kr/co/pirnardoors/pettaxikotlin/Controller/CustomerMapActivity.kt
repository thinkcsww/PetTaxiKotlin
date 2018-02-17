package kr.co.pirnardoors.pettaxikotlin.Controller

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment
import com.google.android.gms.location.places.ui.PlaceSelectionListener

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_customer_map.*
import kr.co.pirnardoors.pettaxikotlin.Model.Customer
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.R.id.*
import kr.co.pirnardoors.pettaxikotlin.Utilities.*
import org.jetbrains.anko.share
import org.jetbrains.anko.toast
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class CustomerMapActivity : AppCompatActivity(), OnMapReadyCallback {

    val TAG = "CustomerMapActivity"
    private lateinit var mMap: GoogleMap
    var handler = Handler()
    lateinit var myRunnable: Runnable
    lateinit var translateAnimRight : Animation
    lateinit var translateAnimLeft : Animation
    lateinit var destinationLatLng : LatLng
    var destinationLatitude : Double? = null
    var destinationLongitude : Double? = null
    var destination : String? = ""
    var locationManager : LocationManager? = null
    var locationListener : LocationListener? = null
    lateinit var lastKnownLocation : Location
    var requestActive : Boolean = false
    var driverActive = false
    var timeStamp = System.currentTimeMillis()/1000;
    var userId = FirebaseAuth.getInstance().currentUser?.uid
    var driverUserId : String? = ""
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().getReference("Request")
    val geoFire = GeoFire(database)
    var isPageOpen = false
    var number = ""
    var activityOn = true
    private var umber : String? = null
    private var phoneNumber : String? = null
    var customerMapActive = false

    var customerState = Customer(false, false,
            "", "", "", "")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_map)
        var sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        reincarnation()



        var thread = Thread(object : Runnable {
                override fun run() {
                    try {
                        while (activityOn == true) {
                            Thread.sleep(3000)
                            if (customerState.requestActive == true){
                                runOnUiThread(object : Runnable {
                                    override fun run() {
                                        if (customerState.driverUserId != "") {
                                            customerState.driverActive = true
                                            editor.putBoolean(DRIVER_ACTIVE, customerState.driverActive)
                                            editor.apply()
                                            getDriverInformation()
                                        }
                                        Log.d("InfoMaiton", "나는 살아있따!!!")
                                    }
                                })
                        }
                        }
                    } catch (e: IOException) {
                        e.message
                    }

                }
            }).start()
        //Menu Button

        val animListener = SlidingPageAnimationListener()
        translateAnimRight = AnimationUtils.loadAnimation(this@CustomerMapActivity, R.anim.right_in)
        translateAnimLeft = AnimationUtils.loadAnimation(this@CustomerMapActivity, R.anim.left_out)
        menuBtn.setOnClickListener {
            if (isPageOpen == false) {
                menuLayout.startAnimation(translateAnimRight)
                menuLayout.setVisibility(View.VISIBLE)
                callBtn.visibility = View.INVISIBLE
                menuBtn.visibility = View.INVISIBLE
            } else {
                menuLayout.startAnimation(translateAnimLeft)
            }
        }
        translateAnimLeft.setAnimationListener(animListener)
        translateAnimRight.setAnimationListener(animListener)



        //call catcardog Button
        callBtn.setOnClickListener {
            if(lastKnownLocation != null) {
                if (customerState.requestActive == false) {

                    val mBuilder = AlertDialog.Builder(this@CustomerMapActivity)
                    val mView = layoutInflater.inflate(R.layout.layout_destination, null)
                    var numberEditText : TextView = mView.findViewById(R.id.numberEditText)
                   // var typeEditText : TextView = mView.findViewById(R.id.typeEditText)
                    val okBtn : Button = mView.findViewById(R.id.okBtn)
                    val noBtn : Button = mView.findViewById(R.id.noBtn)

                    mBuilder.setView(mView)
                    val dialog = mBuilder.create()
                    okBtn.setOnClickListener {

                            if (customerState.requestActive == false) {
                                number = numberEditText.text.toString()
                                editor.putString(BOARDING_NUMBER, customerState.number)

                                if (!TextUtils.isEmpty(number) && destination != "") {
                                    customerState.requestActive = true
                                    editor.putBoolean(REQUEST_ACTIVE, customerState.requestActive)
                                    editor.apply()
                                    callBtn.setText("취소하기")

                                    // var type = typeEditText.text.toString()
                                    geoFire.setLocation(userId, GeoLocation(lastKnownLocation.latitude, lastKnownLocation.longitude))
                                    database.child(userId).child("MD").setValue("")
                                    database.child(userId).child("PN").setValue(number)
                                    database.child(userId).child("Destination").setValue(destination)
                                    database.child(userId).child("DestinationLatitude").setValue(destinationLatitude)
                                    database.child(userId).child("DestinationLongitude").setValue(destinationLongitude)
                                    destination = ""
                                    toast("요청이 확인되었습니다.")
                                    dialog.dismiss()
                                } else if(destination == "") {
                                    toast("목적지를 설정해주세요.")
                                } else if(number.isEmpty()) {
                                    toast("탑승객 수를 입력해주세요.")
                                } else {
                                    toast("요청 정보를 입력해주세요.")
                                }
                            }

                    }
                    noBtn.setOnClickListener {
                       dialog.dismiss()
                    }
                    dialog.show()
                } else {
                    customerState.requestActive = false
                    editor.putBoolean(REQUEST_ACTIVE, customerState.requestActive)
                    editor.putBoolean(DRIVER_ACTIVE, false)
                    editor.putString(DRIVER_USERID, "")
                    editor.apply()
                    callBtn.setText("캣카독 부르기")
                    destination = ""
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

        val autocompleteFragment = fragmentManager.findFragmentById(R.id.place_autocomplete_fragment) as PlaceAutocompleteFragment

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                destination = place.name.toString()
                destinationLatLng = place.latLng
                destinationLatitude = destinationLatLng.latitude
                destinationLongitude = destinationLatLng.longitude


            }

            override fun onError(status: Status) {
                status.statusMessage
            }
        })


        //Log out
        logoutBtn.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            geoFire.removeLocation(userId)
            editor.putBoolean(REQUEST_ACTIVE, false)
            editor.putBoolean(DRIVER_ACTIVE, false)
            editor.putString(DRIVER_USERID, "")
            editor.putString(CAR_INFO, "")
            editor.putString(BOARDING_NUMBER, "")
            finish()
            return@setOnClickListener
        }
        editor.apply()
    }

    private fun reincarnation() {
        var sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        customerState.requestActive = sharedPreferences.getBoolean(REQUEST_ACTIVE, false)
        customerState.driverActive = sharedPreferences.getBoolean(DRIVER_ACTIVE, false)
        customerState.driverUserId = sharedPreferences.getString(DRIVER_USERID, "")
        customerState.number = sharedPreferences.getString(BOARDING_NUMBER, "")
        customerState.carInfo = sharedPreferences.getString(CAR_INFO, "")
        if(customerState.requestActive == true) {
            callBtn.text = "취소하기"
        }
        Log.d("정보 requestActive", customerState.requestActive.toString())
        Log.d("정보 driverActive", customerState.driverActive.toString())
        Log.d("정보 driverUserId", customerState.driverUserId)
        Log.d("정보 number", customerState.number)
        Log.d("정보 carInfo", customerState.carInfo)


    }


    /**
     *  When matching is done get driver's location.
     */
    fun getDriverInformation() {
        callBtn.visibility = View.INVISIBLE
        matchText.visibility = View.VISIBLE
        var sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()
        var driverDatabase = FirebaseDatabase.getInstance().getReference("Respond").child(customerState.driverUserId)
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
                    var driverDb = FirebaseDatabase.getInstance().getReference("Driver").child(customerState.driverUserId)
                    driverDb.addValueEventListener(object : ValueEventListener {
                        override fun onCancelled(p0: DatabaseError?) {
                            if (p0 != null) {
                                p0.message
                            }
                        }
                        override fun onDataChange(p0: DataSnapshot?) {
                            var dataSnapshot = p0
                            if (dataSnapshot != null) {
                                customerState.carInfo = dataSnapshot.child("CarNumber").getValue().toString()
                                editor.putString(CAR_INFO, customerState.carInfo)
                                editor.commit()
                                customerState.phoneNumber = dataSnapshot.child("PhoneNumber").getValue().toString()
                                editor.putString(PHONENUMBER, customerState.phoneNumber)
                                editor.commit()

                            }
                        }
                    })


                    if(distanceRound < 0.01) {

                        handler = Handler()
                        customerState.requestActive = false
                        editor.putBoolean(REQUEST_ACTIVE, customerState.requestActive)
                        editor.apply()
                        customerState.driverActive = false
                        editor.putBoolean(DRIVER_ACTIVE, customerState.driverActive)
                        editor.apply()
                        editor.putString(DRIVER_USERID, "")
                        editor.apply()
                        myRunnable = Runnable {
                            callBtn.setVisibility(View.VISIBLE)
                            callBtn.setText("캣카독 부르기")
                            updateMap(lastKnownLocation)

                            var recordDB = FirebaseDatabase.getInstance().getReference("Record").child(timeStamp.toString())
                            recordDB.child("customerId").setValue(userId)
                            recordDB.child("carNumber").setValue(customerState.carInfo)
                            recordDB.child("phoneNumber").setValue(customerState.phoneNumber)

                            if(customerState.driverUserId != "null") {
                                recordDB.child("driverId").setValue(customerState.driverUserId)
                            }

                            matchText.visibility = View.INVISIBLE

//                            geoFire.removeLocation(userId)
                            editor.apply()
                            var intentMeet = Intent(this@CustomerMapActivity, MeetActivity::class.java)
                            intentMeet.putExtra("Destination", destination)
                            startActivity(intentMeet)
                            handler.removeCallbacks(myRunnable)
                            Log.d(TAG, "나는 살아있다!!!")
                            finish()
                            return@Runnable
                        }
                        handler.postDelayed(myRunnable, 5000)
                        matchText.text = "Driver가 도착하였습니다. 잠시만 기다려주세요.."


                    } else {
                        matchText.text = "Driver와 ${distanceRound.toString()}km 거리입니다."
                    }
                }
            }
        })

    }



    private inner class SlidingPageAnimationListener : Animation.AnimationListener {
        /**
         * 애니메이션이 끝날 때 호출되는 메소드
         */
        override fun onAnimationEnd(animation: Animation) {
            if (isPageOpen == false) {
                isPageOpen = true
            } else {
                isPageOpen = false
                menuLayout.visibility = View.GONE
                callBtn.visibility = View.VISIBLE
            }
        }

        override fun onAnimationRepeat(animation: Animation) {

        }

        override fun onAnimationStart(animation: Animation) {

        }

    }


    // oncreate finish

    /**
     *  Check if driver is matched using Child Md is null or not
     */
    fun checkIfDriverIsMatched() {
        var sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()
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
                    customerState.driverUserId = dataSnapshot.getValue().toString()
                    editor.putString(DRIVER_USERID, customerState.driverUserId)
                    editor.commit()
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
                Log.i("Info", customerState.driverUserId)
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

    override fun onBackPressed() {
        if(isPageOpen == true) {
            menuLayout.startAnimation(translateAnimLeft)
            menuBtn.visibility = View.VISIBLE
        } else {
            super.onBackPressed()
        }
    }





    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putParcelable(EXTRA_CUSTOMER, customerState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        if(savedInstanceState != null) {
            customerState = savedInstanceState.getParcelable(EXTRA_CUSTOMER)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityOn = false
    }



}
