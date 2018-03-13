package kr.co.pirnardoors.pettaxikotlin.Controller

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.*
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.provider.Settings
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import com.bumptech.glide.Glide
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
import com.google.firebase.storage.FirebaseStorage
import jp.wasabeef.glide.transformations.CropCircleTransformation
import kotlinx.android.synthetic.main.activity_customer_map.*
import kr.co.pirnardoors.pettaxikotlin.Model.Customer
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.*
import org.jetbrains.anko.toast
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CustomerMapActivity : AppCompatActivity(), OnMapReadyCallback {

    //DB ZONE
    var Id = FirebaseAuth.getInstance().currentUser!!.email!!
    val mStorage = FirebaseStorage.getInstance().getReference()
    val customerDB = FirebaseDatabase.getInstance().getReference("Customer")
    var respondDB = FirebaseDatabase.getInstance().getReference("Respond")
    var driverDB = FirebaseDatabase.getInstance().getReference("Driver")
    val requestDB = FirebaseDatabase.getInstance().getReference("Request")
    val geoFire = GeoFire(requestDB)
    var driverProfileImageUrl = ""
    var profileImageUrl = ""
    lateinit var pictureUri : Uri
    lateinit var profileImagefilePath: Uri
///
    lateinit var geocoder : Geocoder
    val fragmentManager = supportFragmentManager
    var carNumber = ""
    var carColor = ""
    var carModel = ""
    var driverId = ""
    var driverDeparture = false
    var reserveTime = ""
    var departureLatLng : LatLng? = null
    var departure : String? = ""
    val TAG = "CustomerMapActivity"
    private lateinit var mMap: GoogleMap
    var handler = Handler()
    lateinit var profileImageViewFragment : ProfileImageFragment
    lateinit var myRunnable: Runnable
    lateinit var translateAnimRight : Animation
    lateinit var translateAnimLeft : Animation
    lateinit var destinationLatLng : LatLng
    var destinationLatitude : Double? = null
    var destinationLongitude : Double? = null
    var destination : String? = ""
    lateinit var locationManager : LocationManager
    var locationListener : LocationListener? = null
    lateinit var lastKnownLocation : Location
    var requestActive : Boolean = false
    var driverActive = false
    var userId = FirebaseAuth.getInstance().currentUser!!.uid
    var driverUserId : String? = ""
    val auth = FirebaseAuth.getInstance()
    var isPageOpen = false
    var number = ""
    var distanceRound : Double? = null
    var activityOn = true
    var alarmAlerted = false
    var driverMatchedAlarm = false
    var customerMapActive = false
    var year = 0; var month = 0; var day = 0 ; var hour = 0; var minute = 0
    var currentYear = 0; var currentMonth = 0; var currentDay = 0 ; var currentHour = 0; var currentMinute = 0
    var profileFragmentIsOn = false

    var calendar = Calendar.getInstance()
    var customerState = Customer(false, false,
            "", "", "", "")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_map)
        var sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()


        geocoder = Geocoder(this@CustomerMapActivity, Locale.getDefault())
        //get current date, year , month , hour, day
        getCurrentDate()
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
                                            if(driverDeparture == true && alarmAlerted == false) {
                                                //if driver is coming, let you know by notification alarm.
                                                notifyDriverDeparture()
                                            }
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

        // Driver Info Button when driver is matched

        driverInfoBtn.setOnClickListener {
            if(customerState.driverActive == true ) {
                val driverInfoAlertDialog = AlertDialog.Builder(this@CustomerMapActivity)
                val driverInfoDialogView = layoutInflater.inflate(R.layout.layout_driver_info, null)
                driverInfoAlertDialog.setView(driverInfoDialogView)
                val dialog = driverInfoAlertDialog.create()
                val okBtn : Button = driverInfoDialogView.findViewById(R.id.okBtn)
                val driverProfileImageView : ImageView = driverInfoDialogView.findViewById(R.id.driverProfileImageView)
                val driverCarNumberTextView : TextView = driverInfoDialogView.findViewById(R.id.driverCarNumberTextView)
                val driverCarTextView : TextView = driverInfoDialogView.findViewById(R.id.driverCarTextView)
                val driverIdTextView : TextView = driverInfoDialogView.findViewById(R.id.driverIdTextView)
                driverInfoDialogView.setOnClickListener {
                    dialog.dismiss()
                    profileFragmentIsOn = true
                    val transaction = fragmentManager.beginTransaction()
                    profileImageViewFragment = ProfileImageFragment()
                    val bundle = Bundle()
                    bundle.putString(PROFILEURL, driverProfileImageUrl)
                    profileImageViewFragment.arguments = bundle
                    transaction.replace(R.id.profileHolder, profileImageViewFragment)
                    transaction.addToBackStack(null)
                    transaction.commit()
                }
                if (driverProfileImageUrl != "") {
                    Glide.with(this@CustomerMapActivity).load(driverProfileImageUrl)
                            .centerCrop()
                            .bitmapTransform(CropCircleTransformation(this@CustomerMapActivity))
                            .into(driverProfileImageView)
                } else {
                    Glide.with(this@CustomerMapActivity).load(R.drawable.profile)
                            .centerCrop()
                            .bitmapTransform(CropCircleTransformation(this@CustomerMapActivity))
                            .into(driverProfileImageView)
                }
                driverIdTextView.text = driverId
                driverCarTextView.text = carColor + " " + carModel
                driverCarNumberTextView.text = carNumber
                okBtn.setOnClickListener {


                    dialog.dismiss()
                }

                dialog.show()

            } else {
                toast("매칭된 Driver가 없습니다.")
            }
        }
        //Reservation Button

        /*reserveBtn.setOnClickListener {
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
        }*/


        //Menu Button || Animation

        val animListener = SlidingPageAnimationListener()
        translateAnimRight = AnimationUtils.loadAnimation(this@CustomerMapActivity, R.anim.right_in)
        translateAnimLeft = AnimationUtils.loadAnimation(this@CustomerMapActivity, R.anim.left_out)
        menuBtn.setOnClickListener {
            if (isPageOpen == false) {
                menuLayout.startAnimation(translateAnimRight)
                menuLayout.setVisibility(View.VISIBLE)
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
            try {
                if (lastKnownLocation != null) {
                    if (customerState.requestActive == false) {

                        if (departureText.text == "출발지를 설정해주세요." || destinationText.text == "목적지를 설정해주세요.") {
                            toast("출발지 또는 목적지를 설정해주세요.")
                            return@setOnClickListener
                        }

                        val mBuilder = AlertDialog.Builder(this@CustomerMapActivity)
                        val mView = layoutInflater.inflate(R.layout.layout_asking_info, null)
                        val numberSetTextView: TextView = mView.findViewById(R.id.numberSetTextView)
                        val numberTextView: TextView = mView.findViewById(R.id.numberTextView)
                        // var typeEditText : TextView = mView.findViewById(R.id.typeEditText)
                        val okBtn: Button = mView.findViewById(R.id.okBtn)
                        val noBtn: Button = mView.findViewById(R.id.noBtn)
                        val reserveTextView: TextView = mView.findViewById(R.id.reserveTextView)
                        val timeTextViewInDestinationLayout: TextView = mView.findViewById(R.id.timeTextViewInDestinationLayout)


                        mBuilder.setView(mView)
                        val dialog = mBuilder.create()

                        //numberSet TextView

                        numberSetTextView.setOnClickListener {
                            val mBuilder = AlertDialog.Builder(this@CustomerMapActivity)
                            val mView = layoutInflater.inflate(R.layout.layout_number_picker, null)
                            mBuilder.setView(mView)
                            val dialog = mBuilder.create()
                            val numberPicker: NumberPicker = mView.findViewById(R.id.numberPicker)
                            val setBtn: Button = mView.findViewById(R.id.setBtn)
                            numberPicker.maxValue = 5
                            numberPicker.minValue = 1
                            numberPicker.wrapSelectorWheel = true
                            number = "1"
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
                            val dateTextView: TextView = mView.findViewById(R.id.dateTextView)
                            val timeTextView: TextView = mView.findViewById(R.id.timeTextView)
                            val timeCompleteBtn: Button = mView.findViewById(R.id.timeCompleteBtn)

                            dateTextView.setOnClickListener {
                                calendar = Calendar.getInstance()
                                year = calendar.get(Calendar.YEAR);
                                month = calendar.get(Calendar.MONTH);
                                day = calendar.get(Calendar.DAY_OF_MONTH)

                                val datePickerDialog = DatePickerDialog(this@CustomerMapActivity, R.style.DialogTheme, DatePickerDialog.OnDateSetListener { datePicker, year, month, day ->
                                    calendar.set(Calendar.YEAR, year)
                                    calendar.set(Calendar.MONTH, month)
                                    calendar.set(Calendar.DAY_OF_MONTH, day)
                                    this.year = calendar.get(Calendar.YEAR).toInt()
                                    this.month = calendar.get(Calendar.MONTH).toInt()
                                    this.day = calendar.get(Calendar.DAY_OF_MONTH).toInt()
                                    dateTextView.setText("${this.year.toString()}년 ${this.month + 1}월 ${this.day}일")
                                    reserveTime = ""
                                    reserveTime = "$year 년 ${month + 1} 월 $day 일 "
                                }, year, month, day)
                                datePickerDialog.show()

                            }
                            timeTextView.setOnClickListener {
                                hour = calendar.get(Calendar.HOUR_OF_DAY)
                                minute = calendar.get(Calendar.MINUTE)
                                val timePickerDialog = TimePickerDialog(this@CustomerMapActivity, R.style.DialogTheme, TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
                                    timeTextView.setText("${hour.toString()}시 ${minute}분")
                                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                                    calendar.set(Calendar.MINUTE, minute)
                                    calendar.set(Calendar.SECOND, 0)
                                    this.hour = calendar.get(Calendar.HOUR_OF_DAY).toInt()
                                    this.minute = calendar.get(Calendar.MINUTE).toInt()
                                    reserveTime = "${this.year} 년 ${this.month + 1} 월 ${this.day} 일 "
                                    reserveTime += "$hour 시 $minute 분 "
                                }, hour, minute, false)
                                timePickerDialog.show()

                            }
                            timeCompleteBtn.setOnClickListener {
                                timeTextView.text = reserveTime
                                timeTextViewInDestinationLayout.text = reserveTime
                                hour = calendar.get(Calendar.HOUR_OF_DAY)
                                minute = calendar.get(Calendar.MINUTE)
                                year = calendar.get(Calendar.YEAR);
                                month = calendar.get(Calendar.MONTH);
                                day = calendar.get(Calendar.DAY_OF_MONTH)
                                Log.d("Inf123", calendar.time.toString())
                                dialog.dismiss()
                            }

                            dialog.show()
                        }
                        // ok Button
                        okBtn.setOnClickListener {

                            if (customerState.requestActive == false) {
                                editor.putString(BOARDING_NUMBER, customerState.number)




                                if (!TextUtils.isEmpty(number) && destination != "" && departureLatLng != null) {
                                    customerState.requestActive = true
                                    editor.putBoolean(REQUEST_ACTIVE, customerState.requestActive)
                                    editor.apply()
                                    callBtn.setText("취소하기")

                                    // var type = typeEditText.text.toString()
                                    geoFire.setLocation(userId, GeoLocation(departureLatLng!!.latitude, departureLatLng!!.longitude))
                                    requestDB.child(userId).child("MD").setValue("")
                                    requestDB.child(userId).child("PN").setValue(number)
                                    requestDB.child(userId).child("Destination").setValue(destination)
                                    requestDB.child(userId).child("DestinationLatitude").setValue(destinationLatitude)
                                    requestDB.child(userId).child("DestinationLongitude").setValue(destinationLongitude)
                                    requestDB.child(userId).child("Reservation").setValue(calendar.time.toString())
                                    requestDB.child(userId).child("DD").setValue("false")
                                    requestDB.child(userId).child("TD").setValue("false")
                                    departure = departureText.text.toString()
                                    destination = destinationText.text.toString()
                                    editor.putString(DEPARTURE, departure)
                                    editor.putString(DESTINATION, destination)
                                    editor.apply()

                                    val intent = Intent(this@CustomerMapActivity, NotificationReceiver::class.java)
                                    val pendingIntent = PendingIntent.getBroadcast(this@CustomerMapActivity, ALARM_BROADCAST, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

                                    calendar.set(Calendar.HOUR_OF_DAY, hour - 1)
                                    Log.d("ASASD year : ", calendar.get(Calendar.YEAR).toString())
                                    Log.d("ASASD currentYear : ", currentYear.toString())
                                    Log.d("ASASD month : ", (calendar.get(Calendar.MONTH) + 1).toString())
                                    Log.d("ASASD currentMonth : ", currentMonth.toString())
                                    Log.d("ASASD day : ", (calendar.get(Calendar.DAY_OF_MONTH)).toString())
                                    Log.d("ASASD currentDay : ", (currentDay).toString())
                                    Log.d("ASASD hour : ", (calendar.get(Calendar.HOUR_OF_DAY) + 1).toString())
                                    Log.d("ASASD currentHour : ", currentHour.toString())
                                    Log.d("ASASD minute : ", minute.toString())
                                    if (
                                            currentHour == calendar.get(Calendar.HOUR_OF_DAY) + 1
                                            && currentYear == calendar.get(Calendar.YEAR)
                                            && currentMonth == calendar.get(Calendar.MONTH) + 1
                                            && currentDay == calendar.get(Calendar.DAY_OF_MONTH)
                                    ) {
                                        toast("요청되었습니다.")
                                        calendar.set(Calendar.HOUR_OF_DAY, hour + 1)
                                        requestDB.child(userId).child("Reservation").setValue(calendar.time.toString())
                                        dialog.dismiss()
                                    } else {
                                        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                                        Toast.makeText(this@CustomerMapActivity, "${reserveTime}예약되었습니다.", Toast.LENGTH_SHORT).show()
                                        dialog.dismiss()
                                    }
                                } else if (destination == "") {
                                    toast("목적지를 설정해주세요.")
                                } else if (number.isEmpty()) {
                                    toast("탑승객 수를 입력해주세요.")
                                } else if (departure == "") {
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
                        editor.putBoolean(DRIVER_MATCHED_ALARM, false)
                        editor.putBoolean(ALARM_ALERTED, false)
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
            } catch(e: UninitializedPropertyAccessException) {
                toast("GPS 통신 에러 : 잠시 후 다시 시도해주세요.")
                startActivity(getIntent());
                finish()
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
            editor.putBoolean(CUSTOMER_LOGON, false)
            editor.putBoolean(ALARM_ALERTED, false)
            editor.putBoolean(DRIVER_MATCHED_ALARM, false)
            editor.putString(DEPARTURE, "출발지를 설정해주세요.")
            editor.putString(DESTINATION, "목적지를 설정해주세요.")
            editor.putBoolean(TRANSPORT_ACTIVE, false)
            editor.apply()
            finish()
            return@setOnClickListener
        }

        // Show Profile image bigger in Fragment
        profileImageView.setOnClickListener {
            profileFragmentIsOn = true
            val transaction = fragmentManager.beginTransaction()
            profileImageViewFragment = ProfileImageFragment()
            val bundle = Bundle()
            bundle.putString(PROFILEURL, profileImageUrl)
            profileImageViewFragment.arguments = bundle
            transaction.replace(R.id.profileHolder, profileImageViewFragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }
        // MenuLayout setting!

        imageSelectBtn.setOnClickListener {

            val profileAlertDialogBuilder = AlertDialog.Builder(this@CustomerMapActivity)
            val customerProfileDialogView = layoutInflater.inflate(R.layout.layout_customer_profile_imageview, null)
            profileAlertDialogBuilder.setView(customerProfileDialogView)
            val takePicutreBtn : Button = customerProfileDialogView.findViewById(R.id.takePictureBtn)
            val selectPicutreBtn : Button = customerProfileDialogView.findViewById(R.id.selectPictureBtn)
            val basicImageBtn : Button = customerProfileDialogView.findViewById(R.id.basicImageBtn)
            val dialog = profileAlertDialogBuilder.create()
            takePicutreBtn.setOnClickListener {
                invokeCamera()
                dialog.dismiss()
            }
            selectPicutreBtn.setOnClickListener {
                val intent = Intent()
                intent.setType("image/*")
                intent.setAction(Intent.ACTION_PICK)
                startActivityForResult(intent, CUSTOMERMAP_INTENT_CHOOSER)
                dialog.dismiss()
            }
            basicImageBtn.setOnClickListener {
                profileImageView.setImageResource(R.drawable.profile)
                dialog.dismiss()
            }

            dialog.show()
        }

        //get ProfileUrl image from Firebase
        customerDB.addValueEventListener(object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError?) {
                if(p0 != null)p0.message
            }

            override fun onDataChange(dataSnapshot: DataSnapshot?) {
                try {
                    if (dataSnapshot != null) {
                        profileImageUrl = dataSnapshot.child(userId).child("Profile").getValue().toString()
                        if (profileImageUrl != "") {
                            Glide.with(this@CustomerMapActivity).load(profileImageUrl)
                                    .centerCrop()
                                    .bitmapTransform(CropCircleTransformation(this@CustomerMapActivity))
                                    .into(profileImageView)
                        } else {
                            Glide.with(this@CustomerMapActivity).load(R.drawable.profile)
                                    .centerCrop()
                                    .bitmapTransform(CropCircleTransformation(this@CustomerMapActivity))
                                    .into(profileImageView)
                        }
                    }
                } catch (e : IllegalArgumentException) {
                    e.message
                }
            }
        })

    }

    /**
     *  onCreate Finish
     */

    private fun getCurrentDate() {
        currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        currentYear = calendar.get(Calendar.YEAR)
        currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        currentMonth = calendar.get(Calendar.MONTH) + 1
    }

    private fun notifyDriverDeparture() {

        var editor = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this@CustomerMapActivity, CustomerMapActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this@CustomerMapActivity, ALARM_BROADCAST, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationCompat.Builder(this@CustomerMapActivity)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setContentTitle("CatCarDog 알림")
                    .setContentText("Driver가 출발하였습니다.")
                    .setSmallIcon(R.drawable.ic_subdirectory_arrow_left_black_24dp)
                    .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
                    .setPriority(NotificationManager.IMPORTANCE_HIGH)
        } else {
            NotificationCompat.Builder(this@CustomerMapActivity)
                    .setAutoCancel(true)
                    .setContentTitle("CatCarDog 알림")
                    .setContentText("Driver가 출발하였습니다.")
                    .setSmallIcon(R.drawable.ic_subdirectory_arrow_left_black_24dp)
                    .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
                    .setPriority(Notification.PRIORITY_MAX)
        }
        notificationManager.notify(ALARM_BROADCAST, builder.build())
        alarmAlerted = true
        editor.putBoolean(ALARM_ALERTED, alarmAlerted)
        editor.apply()


    }


    private fun reincarnation() {

        var sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()
        customerState.requestActive = sharedPreferences.getBoolean(REQUEST_ACTIVE, false)
        customerState.driverActive = sharedPreferences.getBoolean(DRIVER_ACTIVE, false)
        customerState.driverUserId = sharedPreferences.getString(DRIVER_USERID, "")
        customerState.number = sharedPreferences.getString(BOARDING_NUMBER, "")
        customerState.carInfo = sharedPreferences.getString(CAR_INFO, "")
        alarmAlerted = sharedPreferences.getBoolean(ALARM_ALERTED, false)
        driverMatchedAlarm = sharedPreferences.getBoolean(DRIVER_MATCHED_ALARM, false)
        if(customerState.requestActive == false) {
            destinationText.text = "목적지를 설정해주세요."
            departureText.text = "출발지를 설정해주세요."
            editor.putBoolean(ALARM_ALERTED, false)
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
        respondDB.child(customerState.driverUserId).child("l").addValueEventListener(object : ValueEventListener{
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
                    driverDB.child(customerState.driverUserId).addValueEventListener(object : ValueEventListener {
                        override fun onCancelled(p0: DatabaseError?) {
                            if (p0 != null) {
                                p0.message
                            }
                        }
                        override fun onDataChange(p0: DataSnapshot?) {
                            var dataSnapshot = p0
                            if (dataSnapshot != null) {
//                                customerState.carInfo = dataSnapshot.child("CarNumber").getValue().toString()
//                                editor.putString(CAR_INFO, customerState.carInfo)
//                                editor.apply()
//                                customerState.phoneNumber = dataSnapshot.child("PhoneNumber").getValue().toString()
//                                editor.putString(PHONENUMBER, customerState.phoneNumber)
//                                editor.apply()
                                driverProfileImageUrl = dataSnapshot.child("Profile").getValue().toString()

                                carNumber = dataSnapshot.child("CarNumber").getValue().toString()
                                carColor = dataSnapshot.child("CarColor").getValue().toString()
                                carModel = dataSnapshot.child("CarModel").getValue().toString()
                                driverId = dataSnapshot.child("Id").getValue().toString()

                            }
                        }
                    })
                }
            }
        })
        if(driverDeparture == true) {
            if (distanceRound != null) {
                if (distanceRound!! < 0.00001) {

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

                        matchText.visibility = View.INVISIBLE

//                            geoFire.removeLocation(userId)
                        alarmAlerted = false
                        editor.putBoolean(ALARM_ALERTED, alarmAlerted)
                        editor.apply()
                        var intentMeet = Intent(this@CustomerMapActivity, MeetActivity::class.java)
                        intentMeet.putExtra("Destination", destination)
                        startActivity(intentMeet)
                        handler.removeCallbacks(myRunnable)
                        Log.d(TAG, "나는 살아있다!!!")
                        //finish()
                        return@Runnable
                    }
                    handler.postDelayed(myRunnable, 5000)
                    matchText.text = "Driver가 도착하였습니다.\n잠시만 기다려주세요.."


                } else {
                    matchText.text = "Driver와 ${distanceRound.toString()}km 거리입니다."
                }
            }
        } else {
            if(driverMatchedAlarm == false) {
                driverMatchedAlarm = true
                editor.putBoolean(DRIVER_MATCHED_ALARM, driverMatchedAlarm)
                editor.apply()
                //Alarm for driverMatched
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val intent = Intent(this@CustomerMapActivity, CustomerMapActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                val pendingIntent = PendingIntent.getActivity(this@CustomerMapActivity, ALARM_BROADCAST, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    NotificationCompat.Builder(this@CustomerMapActivity)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent)
                            .setContentTitle("CatCarDog 알림")
                            .setContentText("Driver가 매칭되었습니다.")
                            .setSmallIcon(R.drawable.ic_subdirectory_arrow_left_black_24dp)
                            .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
                            .setPriority(NotificationManager.IMPORTANCE_HIGH)
                } else {
                    NotificationCompat.Builder(this@CustomerMapActivity)
                            .setAutoCancel(true)
                            .setContentTitle("CatCarDog 알림")
                            .setContentText("Driver가 매칭되었습니다.")
                            .setSmallIcon(R.drawable.ic_subdirectory_arrow_left_black_24dp)
                            .setDefaults(Notification.DEFAULT_LIGHTS or Notification.DEFAULT_SOUND)
                            .setPriority(Notification.PRIORITY_MAX)
                }
                notificationManager.notify(ALARM_BROADCAST, builder.build())
                //AlertDialog
                val driverMatchedBuilder = AlertDialog.Builder(this@CustomerMapActivity)
                val driverMatchedView = layoutInflater.inflate(R.layout.layout_driver_matched, null)
                driverMatchedBuilder.setView(driverMatchedView)
                val okBtn: Button = driverMatchedView.findViewById(R.id.okBtn)
                val driverMatchedDialog = driverMatchedBuilder.create()


                okBtn.setOnClickListener {
                    driverMatchedDialog.dismiss()
                }
                driverMatchedDialog.show()
            } else {
                matchText.visibility = View.VISIBLE
                matchText.text = "출발 대기중입니다."
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
                if(customerState.driverActive == false) {
                    callBtn.visibility = View.VISIBLE
                }
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
        var databaseForMatching = FirebaseDatabase.getInstance().getReference("Request").child(userId)
        databaseForMatching.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError?) {
                if (p0 != null) {
                    p0.message
                }
            }
            override fun onDataChange(p0: DataSnapshot?) {
                var dataSnapshot = p0
                if (dataSnapshot != null) {
                    customerState.driverUserId = dataSnapshot.child("MD").getValue().toString()
                    driverDeparture = dataSnapshot.child("DD").getValue().toString().toBoolean()
                    Log.d("ASDASD", driverDeparture.toString())
                    editor.putString(DRIVER_USERID, customerState.driverUserId)
                    editor.apply()
                }
            }

        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PLACEPICKER_DEPARTURE_REQUESTCODE) {
            if(resultCode == Activity.RESULT_OK) {
                try {
                    var place = PlacePicker.getPlace(data, this@CustomerMapActivity)
                    departureLatLng = place.latLng
                    if (Character.isDigit(place.name.elementAt(0))) {
                        departure = String.format("출발지: ${place.address}")
                    } else {
                        departure = String.format("출발지: ${place.name}")
                    }
                    departureText.text = departure
                } catch (e : StringIndexOutOfBoundsException) {
                    toast("GPS 수신 에러 : 잠시 후 다시 시도해주세요.")
                }
            }

        } else if (requestCode == PLACEPICKER_ARRIVAL_REQUESTCODE) {
            if(resultCode == Activity.RESULT_OK) {
                try {
                    var place = PlacePicker.getPlace(data, this@CustomerMapActivity)
                    if (Character.isDigit(place.name.elementAt(0))) {
                        destination = String.format("${place.address}")
                    } else {
                        destination = String.format("${place.name}")
                    }
                    destinationLatLng = place.latLng
                    destinationLatitude = destinationLatLng.latitude
                    destinationLongitude = destinationLatLng.longitude
                    destinationText.text = "도착지: $destination"
                } catch (e : StringIndexOutOfBoundsException) {
                    toast("GPS 수신 에러 : 잠시 후 다시 시도해주세요.")
                }
            }
        } else if (requestCode == CUSTOMERMAP_INTENT_CAMERA) {
            if(resultCode == Activity.RESULT_OK) {
                profileImagefilePath = pictureUri
                try{
                    Glide.with(this@CustomerMapActivity).load(profileImagefilePath)
                            .centerCrop()
                            .bitmapTransform(CropCircleTransformation(this))
                            .into(profileImageView)
                    uploadImage()
                } catch (e : IOException) {
                    e.message
                }

            }
        } else if (requestCode == CUSTOMERMAP_INTENT_CHOOSER) {
            if (resultCode == Activity.RESULT_OK) {
                profileImagefilePath = data!!.data
                try {
                    Glide.with(this@CustomerMapActivity).load(profileImagefilePath).centerCrop().bitmapTransform(CropCircleTransformation(this))
                            .into(profileImageView)
                    uploadImage()
                } catch ( e: IOException) {
                    e.message
                }
            }
        }

    }

    //Profile Part
    private fun invokeCamera() {
        pictureUri = FileProvider.getUriForFile(
                this@CustomerMapActivity,
                applicationContext.packageName + ".provider",
                createImageFile())

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        intent.putExtra(MediaStore.EXTRA_OUTPUT, pictureUri)

        intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        startActivityForResult(intent, CUSTOMERMAP_INTENT_CAMERA)
    }

    private fun createImageFile(): File {
        val picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss")
        var timeStamp = sdf.format(Date())

        val imageFile = File(picturesDirectory, "picture" + timeStamp + ".jpg")
        return imageFile
    }


    private fun uploadImage() {
        if(Id != "") {
            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("잠시만 기다려주세요...")
            progressDialog.show()
            mStorage.child(Id).child("Profile").putFile(profileImagefilePath).addOnSuccessListener {
                profileImageUrl = it.downloadUrl.toString()
                customerDB.child(userId).child("Profile").setValue(profileImageUrl)

                progressDialog.dismiss()
            }
                    .addOnFailureListener {
                        progressDialog.dismiss();
                        toast("업로드 실패")
                    }
                    .addOnProgressListener {
                        var progress = (100 * it.bytesTransferred / it.totalByteCount).toInt()
                        progressDialog.setMessage("$progress%" )
                    }

        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        Log.d("GPS", isGPSEnabled.toString())
        if(isGPSEnabled == false) {
            val locationAlertBuilder = AlertDialog.Builder(this@CustomerMapActivity)
            val locationAlertView = layoutInflater.inflate(R.layout.layout_location_setting, null)
            locationAlertBuilder.setView(locationAlertView)
            val dialog = locationAlertBuilder.create()
            dialog.setCanceledOnTouchOutside(false)
            val okBtn : Button = locationAlertView.findViewById(R.id.okBtn)
            val settingBtn : Button = locationAlertView.findViewById(R.id.settingBtn)

            okBtn.setOnClickListener {
                dialog.dismiss()
            }

            settingBtn.setOnClickListener {
                if(isGPSEnabled == false) {
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    intent.addCategory(Intent.CATEGORY_DEFAULT)
                    startActivity(intent)
                    dialog.dismiss()
                    return@setOnClickListener
                }
            }
            dialog.show()
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
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
        if(isGPSEnabled) {

            try {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                var userLocation = LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)
                mMap.clear()
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                mMap.addMarker(MarkerOptions().position(userLocation).title("Your Location"))
                val curAddress: List<Address> = geocoder.getFromLocation(lastKnownLocation.latitude, lastKnownLocation.longitude, 1)
                Log.d("Address : : ", curAddress.get(0).getAddressLine(0))
                curLocationTextView.text = curAddress.get(0).getAddressLine(0).substring(5)
            } catch (e:IllegalStateException ) {
                e.message
            }
        }


    }

    fun updateMap(location : Location?) {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)

        val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if(lastKnownLocation != null) {
            if(curLocationTextView.text == "주소 검색중...") {
                val curAddress: List<Address> = geocoder.getFromLocation(lastKnownLocation.latitude, lastKnownLocation.longitude, 1)
                Log.d("Address : : ", curAddress.get(0).getAddressLine(0))
                curLocationTextView.text = curAddress.get(0).getAddressLine(0).substring(5)
            }
            val userLocation = LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)

            mMap.clear()
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
            mMap.addMarker(MarkerOptions().position(userLocation).title("Your Location"))
        }


    }

    override fun onBackPressed() {
        if (profileFragmentIsOn == true && isPageOpen == true) {
            profileFragmentIsOn = false
            val transaction = fragmentManager.beginTransaction()
            transaction.remove(profileImageViewFragment)
            transaction.commit()
        } else if(isPageOpen == true) {
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
