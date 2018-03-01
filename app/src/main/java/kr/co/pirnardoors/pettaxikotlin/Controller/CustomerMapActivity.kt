package kr.co.pirnardoors.pettaxikotlin.Controller

import android.app.*
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
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.places.ui.PlacePicker

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_customer_map.*
import kotlinx.android.synthetic.main.layout_destination.*
import kr.co.pirnardoors.pettaxikotlin.Model.Customer
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.*
import org.jetbrains.anko.toast
import java.io.IOException
import java.util.*

class CustomerMapActivity : AppCompatActivity(), OnMapReadyCallback {
    var reservation = false
    var reserveTime = ""
    var departureLatLng : LatLng? = null
    var departure : String? = ""
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
    var distanceRound : Double? = null
    var activityOn = true
    private var umber : String? = null
    private var phoneNumber : String? = null
    var customerMapActive = false
    var year = 0; var month = 0; var day = 0 ; var hour = 0; var minute = 0
    val calendar = Calendar.getInstance()

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

        editor.putBoolean(CUSTOMER_LOGON, true)
        editor.apply()


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
        //Reservation Button

        reserveBtn.setOnClickListener {
            val mBuilder = AlertDialog.Builder(this@CustomerMapActivity)
            val mView = layoutInflater.inflate(R.layout.time_picker_layout, null)
            mBuilder.setView(mView)
            val dialog = mBuilder.create()
            val dateTextView : TextView = mView.findViewById(R.id.dateTextView)
            val timeTextView : TextView = mView.findViewById(R.id.timeTextViewInDestinationlayout)
            val timeCompleteBtn : Button = mView.findViewById(R.id.timeCompleteBtn)

            dateTextView.setOnClickListener {
                year = calendar.get(Calendar.YEAR);
                month = calendar.get(Calendar.MONTH);
                day = calendar.get(Calendar.DAY_OF_MONTH)

                val datePickerDialog = DatePickerDialog(this@CustomerMapActivity, DatePickerDialog.OnDateSetListener {
                    datePicker, year2, month2, day2 ->
                    calendar.set(Calendar.YEAR, year2)
                    calendar.set(Calendar.MONTH, month2)
                    calendar.set(Calendar.DAY_OF_MONTH, day2)
                    year = calendar.get(Calendar.YEAR);
                    month = calendar.get(Calendar.MONTH);
                    day = calendar.get(Calendar.DAY_OF_MONTH)
                    dateTextView.setText("${year.toString()}년 ${month + 1}월 ${day}일")
                    reserveTime = ""
                    reserveTime = "$year 년 ${month + 1} 월 $day 일 "
                }, year, month, day)
                    datePickerDialog.show()

            }
            timeTextView.setOnClickListener {
                hour = calendar.get(Calendar.HOUR_OF_DAY)
                minute = calendar.get(Calendar.MINUTE)
                val timePickerDialog = TimePickerDialog(this@CustomerMapActivity, TimePickerDialog.OnTimeSetListener {
                    timePicker, hour, minute ->
                    timeTextView.setText("${hour.toString()}시 ${minute}분")
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    reserveTime = "$year 년 ${month + 1} 월 $day 일 "
                    reserveTime += "$hour 시 $minute 분 "
                }, hour, minute, false)
                timePickerDialog.show()

            }
            timeCompleteBtn.setOnClickListener {
                toast(calendar.timeInMillis.toString())
                Log.d("Inf123", calendar.time.toString())
                dialog.dismiss()
            }

            dialog.show()
        }
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

        //departure selecting text

        departureText.setOnClickListener {
            val intentBuilder = PlacePicker.IntentBuilder()
            val intent = intentBuilder.build(this@CustomerMapActivity)
            startActivityForResult(intent, PLACEPICKER_DEPARTURE_REQUESTCODE)
        }

        destinationText.setOnClickListener {
            val intentBuilder = PlacePicker.IntentBuilder()
            val intent = intentBuilder.build(this@CustomerMapActivity)
            startActivityForResult(intent, PLACEPICKER_ARRIVAL_REQUESTCODE)
        }


        //call catcardog Button
        callBtn.setOnClickListener {
            if(lastKnownLocation != null) {
                if (customerState.requestActive == false) {

                    val mBuilder = AlertDialog.Builder(this@CustomerMapActivity)
                    val mView = layoutInflater.inflate(R.layout.layout_destination, null)
                    val numberSetTextView : TextView = mView.findViewById(R.id.numberSetTextView)
                    val numberTextView : TextView = mView.findViewById(R.id.numberTextView)
                   // var typeEditText : TextView = mView.findViewById(R.id.typeEditText)
                    val okBtn : Button = mView.findViewById(R.id.okBtn)
                    val noBtn : Button = mView.findViewById(R.id.noBtn)
                    val reserveTextView : TextView = mView.findViewById(R.id.reserveTextView)


                    mBuilder.setView(mView)
                    val dialog = mBuilder.create()

                    //numberSet TextView

                    numberSetTextView.setOnClickListener {
                        val mBuilder = AlertDialog.Builder(this@CustomerMapActivity)
                        val mView = layoutInflater.inflate(R.layout.layout_number_picker, null)
                        mBuilder.setView(mView)
                        val dialog = mBuilder.create()
                        val numberPicker : NumberPicker = mView.findViewById(R.id.numberPicker)
                        val setBtn : Button = mView.findViewById(R.id.setBtn)
                        numberPicker.maxValue = 5
                        numberPicker.minValue = 1
                        numberPicker.wrapSelectorWheel = true
                        numberPicker.setOnValueChangedListener { numberPicker, oldval, newval ->
                            number = newval.toString()
                        }
                        setBtn.setOnClickListener {
                            dialog.dismiss()
                            numberTextView.text = "인원: $number"
                        }
                        dialog.show()

                    }
                    //reserve TextView
                    reserveTextView.setOnClickListener {
                        val mBuilder = AlertDialog.Builder(this@CustomerMapActivity)
                        val mView = layoutInflater.inflate(R.layout.time_picker_layout, null)
                        mBuilder.setView(mView)
                        val dialog = mBuilder.create()
                        val dateTextView : TextView = mView.findViewById(R.id.dateTextView)
                        val timeTextView : TextView = mView.findViewById(R.id.timeTextViewInDestinationlayout)
                        val timeCompleteBtn : Button = mView.findViewById(R.id.timeCompleteBtn)

                        dateTextView.setOnClickListener {
                            year = calendar.get(Calendar.YEAR);
                            month = calendar.get(Calendar.MONTH);
                            day = calendar.get(Calendar.DAY_OF_MONTH)

                            val datePickerDialog = DatePickerDialog(this@CustomerMapActivity, DatePickerDialog.OnDateSetListener {
                                datePicker, year2, month2, day2 ->
                                calendar.set(Calendar.YEAR, year2)
                                calendar.set(Calendar.MONTH, month2)
                                calendar.set(Calendar.DAY_OF_MONTH, day2)
                                year = calendar.get(Calendar.YEAR);
                                month = calendar.get(Calendar.MONTH);
                                day = calendar.get(Calendar.DAY_OF_MONTH)
                                dateTextView.setText("${year.toString()}년 ${month + 1}월 ${day}일")
                                reserveTime = ""
                                reserveTime = "$year 년 ${month + 1} 월 $day 일 "
                            }, year, month, day)
                            datePickerDialog.show()

                        }
                        timeTextView.setOnClickListener {
                            hour = calendar.get(Calendar.HOUR_OF_DAY)
                            minute = calendar.get(Calendar.MINUTE)
                            val timePickerDialog = TimePickerDialog(this@CustomerMapActivity, TimePickerDialog.OnTimeSetListener {
                                timePicker, hour, minute ->
                                timeTextView.setText("${hour.toString()}시 ${minute}분")
                                calendar.set(Calendar.HOUR_OF_DAY, hour)
                                calendar.set(Calendar.MINUTE, minute)
                                calendar.set(Calendar.SECOND, 0)
                                reserveTime = "$year 년 ${month + 1} 월 $day 일 "
                                reserveTime += "$hour 시 $minute 분 "
                            }, hour, minute, false)
                            timePickerDialog.show()

                        }
                        timeCompleteBtn.setOnClickListener {
                            timeTextView.text = reserveTime
                            timeTextViewInDestinationlayout.text = reserveTime
                            Log.d("Inf123", calendar.time.toString())
                            dialog.dismiss()
                        }

                        dialog.show()
                    }
                    // ok Button
                    okBtn.setOnClickListener {

                            if (customerState.requestActive == false) {
                                number = numberTextView.text.toString()
                                editor.putString(BOARDING_NUMBER, customerState.number)




                                if (!TextUtils.isEmpty(number) && destination != "" && departureLatLng != null) {
                                    customerState.requestActive = true
                                    editor.putBoolean(REQUEST_ACTIVE, customerState.requestActive)
                                    editor.apply()
                                    callBtn.setText("취소하기")

                                    // var type = typeEditText.text.toString()
                                    geoFire.setLocation(userId, GeoLocation(departureLatLng!!.latitude, departureLatLng!!.longitude))
                                    database.child(userId).child("MD").setValue("")
                                    database.child(userId).child("PN").setValue(number)
                                    database.child(userId).child("Destination").setValue(destination)
                                    database.child(userId).child("DestinationLatitude").setValue(destinationLatitude)
                                    database.child(userId).child("DestinationLongitude").setValue(destinationLongitude)
                                    database.child(userId).child("Reservation").setValue(calendar.time.toString())
                                    departure = departureText.text.toString()
                                    destination = destinationText.text.toString()
                                    editor.putString(DEPARTURE, departure)
                                    editor.putString(DESTINATION, destination)
                                    editor.apply()

                                    val intent = Intent(this@CustomerMapActivity, NotificationReceiver::class.java)
                                    val pendingIntent = PendingIntent.getBroadcast(this@CustomerMapActivity, ALARM_BROADCAST, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                    calendar.set(Calendar.HOUR_OF_DAY, hour - 1)
                                    alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_FIFTEEN_MINUTES, pendingIntent)
                                    Toast.makeText(this@CustomerMapActivity, "${reserveTime}예약되었습니다.", Toast.LENGTH_SHORT).show()
                                    dialog.dismiss()
                                } else if(destination == "") {
                                    toast("목적지를 설정해주세요.")
                                } else if(number.isEmpty()) {
                                    toast("탑승객 수를 입력해주세요.")
                                } else if(departure == "") {
                                    toast("출발지를 설정해주세요.")
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
                    editor.putString(DEPARTURE, "출발지를 설정해주세요.")
                    editor.putString(DESTINATION, "목적지를 설정해주세요.")
                    editor.apply()
                    destinationText.text = "목적지를 설정해주세요."
                    departureText.text = "출발지를 설정해주세요."
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
            editor.putBoolean(CUSTOMER_LOGON,false)
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
        if(customerState.requestActive == false) {
            destinationText.text = "목적지를 설정해주세요."
            departureText.text = "출발지를 설정해주세요."
        } else {
            destinationText.text = sharedPreferences.getString(DESTINATION, "")
            departureText.text = sharedPreferences.getString(DEPARTURE, "")
        }
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
                    distanceRound = Math.round(distanceKm).toDouble()
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
                }
            }
        })
        if(distanceRound != null) {
            if (distanceRound!! < 0.01) {

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

                    if (customerState.driverUserId != "null") {
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
                matchText.text = "Driver가 도착하였습니다.\n잠시만 기다려주세요.."


            } else {
                matchText.text = "Driver와 ${distanceRound.toString()}km 거리입니다."
            }
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PLACEPICKER_DEPARTURE_REQUESTCODE) {
            if(resultCode == Activity.RESULT_OK) {
                var place = PlacePicker.getPlace(data, this@CustomerMapActivity)
                departureLatLng = place.latLng
                departure = String.format("출발지: ${place.address}")
                departureText.text = "출발지: ${departure!!.substring(10)}"
            }
        } else if (requestCode == PLACEPICKER_ARRIVAL_REQUESTCODE) {
            if(resultCode == Activity.RESULT_OK) {
                var place = PlacePicker.getPlace(data, this@CustomerMapActivity)
                destination = String.format("도착지: ${place.address}")
                destinationLatLng = place.latLng
                destinationLatitude = destinationLatLng.latitude
                destinationLongitude = destinationLatLng.longitude
                destinationText.text = "도착지: ${destination!!.substring(10)}"
            }
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
        if(requestActive == false) {
            destination = ""
            departure = ""
        }
    }



}
