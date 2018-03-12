package kr.co.pirnardoors.pettaxikotlin.Controller

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.ContactsContract
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import com.bumptech.glide.Glide
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.places.ui.PlacePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import jp.wasabeef.glide.transformations.CropCircleTransformation
import kotlinx.android.synthetic.main.activity_view_request.*
import kr.co.pirnardoors.pettaxikotlin.Model.Request
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.*
import org.jetbrains.anko.toast
import java.io.File
import java.io.IOException
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

class ViewRequestActivity : AppCompatActivity() {
    val calendar = Calendar.getInstance()
    var year = calendar.get(Calendar.YEAR).toString()
    var month = (calendar.get(Calendar.MONTH) + 1).toString()
    lateinit var profileImagefilePath : Uri
    lateinit var pictureUri : Uri
    val mStorage = FirebaseStorage.getInstance().getReference()
    val driverDB = FirebaseDatabase.getInstance().getReference("Driver")
    var profileImageUrl = ""
    var request = ArrayList<String>()
    var requestUserId = ArrayList<String>()
    var requestDestinations = ArrayList<String>()
    //var requestTypes = ArrayList<String>()
    lateinit var profileImageViewFragment : ProfileImageFragment
    lateinit var translateAnimRight : Animation
    lateinit var translateAnimLeft : Animation
    var requestNumbers = ArrayList<String>()
    var requestLatitudes = ArrayList<Double>()
    var requestLongitudes = ArrayList<Double>()
    var requestDistances = ArrayList<Double>()
    var auth = FirebaseAuth.getInstance()
    var locationManager : LocationManager? = null
    var locationListener : LocationListener? = null
    var lastKnownLocation : Location? = null
    var adapter :ArrayAdapter<String>? = null
    var driverUserId : String? = ""
    var profileFragmentIsOn = false
    var isPageOpen = false
    var reservationTime = ""
    var reserveResult = ""
    var Id = ""
    val fragmentManager = supportFragmentManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_request)
        var sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()
        editor.putBoolean(DRIVER_LOGON, true)
        editor.apply()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, request)
        listView.adapter = adapter

        driverUserId = FirebaseAuth.getInstance().currentUser!!.uid
        Id = FirebaseAuth.getInstance().currentUser!!.email!!


        //Get Profile info from firebase

        driverDB.addValueEventListener(object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError?) {
                if(p0 != null)p0.message
            }

            override fun onDataChange(dataSnapshot: DataSnapshot?) {
                if(dataSnapshot != null) {
                    profileImageUrl = dataSnapshot.child(driverUserId).child("Profile").getValue().toString()
                    if(profileImageUrl != "") {
                        Glide.with(this@ViewRequestActivity).load(profileImageUrl)
                                .centerCrop()
                                .bitmapTransform(CropCircleTransformation(this@ViewRequestActivity))
                                .into(profileImageView)
                    } else {
                        Glide.with(this@ViewRequestActivity).load(R.drawable.profile)
                                .centerCrop()
                                .bitmapTransform(CropCircleTransformation(this@ViewRequestActivity))
                                .into(profileImageView)
                    }
                    Log.d("PROFILE", profileImageUrl)
                }
            }
        })


        // Find driver location
        if (locationManager == null) {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }

        locationListener = object : LocationListener {
            override fun onLocationChanged(p0: Location?) {
                var location = p0
                if (location != null) {
                    updateListView(location)
                    var databaseDriver = FirebaseDatabase.getInstance().getReference("Respond")
                    var geoFire = GeoFire(databaseDriver)
                    geoFire.setLocation(driverUserId, GeoLocation(location!!.latitude, location!!.longitude))
                }
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
        lastKnownLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if(lastKnownLocation != null) {
            updateListView(lastKnownLocation!!)
        } else {
            Toast.makeText(this, "위치 파악 문제가 발생하였습니다.", Toast.LENGTH_SHORT).show()
        }
        //Logout button

        logoutBtn.setOnClickListener {
            auth.signOut()
            editor.putBoolean(DRIVER_LOGON, false)
            editor.putBoolean(DRIVER_LICENSE_AUTHORIZED,false)
            editor.apply()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return@setOnClickListener

        }
        //refresh Button

        refreshBtn.setOnClickListener {
            finish();
            startActivity(getIntent());
        }

        //menu button animation

        val animListener = SlidingPageAnimationListener()
        translateAnimRight = AnimationUtils.loadAnimation(this@ViewRequestActivity, R.anim.right_in)
        translateAnimLeft = AnimationUtils.loadAnimation(this@ViewRequestActivity, R.anim.left_out)
        menuBtn.setOnClickListener {
            if (isPageOpen == false) {
                menuLayout.startAnimation(translateAnimRight)
                menuLayout.setVisibility(View.VISIBLE)
                menuBtn.visibility = View.INVISIBLE
                refreshBtn.visibility = View.GONE
            } else {
                menuLayout.startAnimation(translateAnimLeft)
            }
        }
        translateAnimLeft.setAnimationListener(animListener)
        translateAnimRight.setAnimationListener(animListener)

        // Listview item clicked listener
        listView.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(p0: AdapterView<*>?, p1: View?, i: Int, p3: Long) {
                if (ActivityCompat.checkSelfPermission(this@ViewRequestActivity,
                                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this@ViewRequestActivity, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return
                }

                 lastKnownLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if(requestLatitudes.size > i && requestLongitudes.size > i && requestUserId.size > i
                        && lastKnownLocation != null) {

                    var req : Request = Request(0.0,0.0,"",0.0,0.0,"","",0.0)
                    req.requestLatitude = requestLatitudes[i]
                    req.requestLongitude = requestLongitudes[i]
                    req.driverLatitude = lastKnownLocation!!.latitude
                    req.driverLongitude = lastKnownLocation!!.longitude
                    req.requestUserId = requestUserId[i]
                    req.requestDestination = requestDestinations[i]
                    req.requestNumber = requestNumbers[i]
                    req.requestDistance = requestDistances[i]
                    //req.requestType = requestTypes[i]

                    var intent = Intent(this@ViewRequestActivity, DriverMapActivity::class.java)
                    intent.putExtra(EXTRA_REQUEST, req)
                    startActivity(intent)
                }
            }

        }
        /**
         *  Menu layout Setting!
         */
        imageSelectBtn.setOnClickListener {

            val profileAlertDialogBuilder = AlertDialog.Builder(this@ViewRequestActivity)
            val customerProfileDialogView = layoutInflater.inflate(R.layout.layout_customer_profile_imageview, null)
            profileAlertDialogBuilder.setView(customerProfileDialogView)
            val takePicutreBtn : Button = customerProfileDialogView.findViewById(R.id.takePictureBtn)
            val selectPicutreBtn : Button = customerProfileDialogView.findViewById(R.id.selectPictureBtn)
            val basicImageBtn : Button = customerProfileDialogView.findViewById(R.id.basicImageBtn)
            val dialog = profileAlertDialogBuilder.create()
            takePicutreBtn.setOnClickListener {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(intent, CUSTOMERMAP_INTENT_CAMERA)
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
                profileImageView.setImageResource(R.mipmap.ic_launcher)
                dialog.dismiss()
            }

            dialog.show()
        }

        //profile ImageView Fragment to show bigger size

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

        //Make storage for next month !
        if(month.toInt() != 12) {
            driverDB.child(driverUserId).child(year + (month.toInt() + 1).toString()).child("DriveTime").setValue(0)
            driverDB.child(driverUserId).child(year + (month.toInt() + 1).toString()).child("Earn").setValue(0)
        } else if (month.toInt() == 12) {
            driverDB.child(driverUserId).child((year.toInt() + 1).toString() + "1").child("DriveTime").setValue(0)
            driverDB.child(driverUserId).child((year.toInt() + 1).toString() + "1").child("Earn").setValue(0)
        }

        //Button to show how much money i earn this month
        monthlyDataBtn.setOnClickListener {
            val monthlyDataAlertDialog = AlertDialog.Builder(this@ViewRequestActivity)
            val monthlyDataDialogView = layoutInflater.inflate(R.layout.layout_monthly_data, null)
            monthlyDataAlertDialog.setView(monthlyDataDialogView)
            val okBtn : Button = monthlyDataDialogView.findViewById(R.id.okBtn)
            val dialog = monthlyDataAlertDialog.create()

            okBtn.setOnClickListener {
                dialog.dismiss()
            }
            dialog.show()
        }

    }

    //oncreate finish

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
                refreshBtn.visibility = View.VISIBLE
            }
        }

        override fun onAnimationRepeat(animation: Animation) {

        }

        override fun onAnimationStart(animation: Animation) {

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CUSTOMERMAP_INTENT_CAMERA) {
            if(resultCode == Activity.RESULT_OK) {
                profileImagefilePath = pictureUri
                try{
                    Glide.with(this@ViewRequestActivity).load(profileImagefilePath)
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
                    Glide.with(this@ViewRequestActivity)
                            .load(profileImagefilePath).centerCrop()
                            .bitmapTransform(CropCircleTransformation(this))
                            .into(profileImageView)
                    uploadImage()
                } catch ( e: IOException) {
                    e.message
                }
            }
        }
    }


    //Read data from firebase to caculate distance
    private fun updateListView(location : Location) {

        var driverLocation = location
        var database = FirebaseDatabase.getInstance().getReference("Request")
        database.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError?) {
                if (p0 != null) {
                    p0.message
                }
            }

            override fun onDataChange(p0: DataSnapshot?) {
                var dataSnapShot = p0
                if(dataSnapShot != null) {
                    request.clear(); requestLatitudes.clear(); requestLongitudes.clear()
                    requestDestinations.clear() ; requestNumbers.clear()
                    var children = dataSnapShot.children
                    for (data in children) {
                        var userId = data.key
                        var md = data.child("MD").getValue().toString()
                        if(md == "") {
                            var requestLatitude = data.child("l").child("0").getValue()
                            var requestLongitued = data.child("l").child("1").getValue()
                            var requestDestination = data.child("Destination").getValue().toString()
                            reservationTime = data.child("Reservation").getValue().toString()
                            if(reservationTime != "null") {
                                Log.d("RESERVEINFO", reservationTime)
                                Log.d("RESERVEINFO", reservationTime.substring(0, 3))
                                Log.d("RESERVEINFO", reservationTime.substring(8, 10))
                                Log.d("RESERVEINFO", reservationTime.substring(11, 13))
                                Log.d("RESERVEINFO", reservationTime.substring(14, 16))
                                Log.d("RESERVEINFO", reservationTime.substring(30, 34))
                                var reserveDay = reservationTime.substring(8, 10).trim()
                                var reserveHour = reservationTime.substring(11, 13).trim()
                                var reserveMinute = reservationTime.substring(14, 16).trim()
                                var reserveYear = reservationTime.substring(30, 34).trim()
                                var reserveMonthBeforeFilter = reservationTime.substring(4, 7).trim()
                                var reserveMonth = monthFilter(reserveMonthBeforeFilter)
                                reserveResult = "${reserveYear}년 ${reserveMonth}월 ${reserveDay}일 " +
                                        "${reserveHour}시 ${reserveMinute}분"
                            }

                            //var requestType = data.child("Type").getValue()
                            var requestNumber = data.child("PN").getValue()
                            Log.d(LISTVIEW, userId)
                            Log.d(LISTVIEW, requestLatitude.toString())
                            Log.d(LISTVIEW, requestLongitued.toString())
                            var destination = Location("Destination")
                            destination.latitude = requestLatitude.toString().toDouble()
                            destination.longitude = requestLongitued.toString().toDouble()
                            var distanceKm = driverLocation.distanceTo(destination) / 1000
                            var distanceRound = Math.round(distanceKm)
                            Log.d(LISTVIEW + "distance =", distanceRound.toString())

                            if (distanceRound >= 0) {
                                request.add("현위치로부터 : ${distanceRound.toString()}Km, 탑승인원 : $requestNumber," +
                                        "\n목적지 : $requestDestination\n예약시간 : $reserveResult")
                                requestLatitudes.add(requestLatitude.toString().toDouble())
                                requestLongitudes.add(requestLongitued.toString().toDouble())
                                requestUserId.add(userId)
                                requestDestinations.add(requestDestination.toString())
                                //requestTypes.add(requestType.toString())
                                requestNumbers.add(requestNumber.toString())
                                requestDistances.add(distanceRound.toDouble())
                            }
                        }
//                        Collections.sort(request)
                        adapter!!.notifyDataSetChanged()

                    }
                }
               }
        })
    }

    fun monthFilter(month : String) : Int {
        if(month == "Jan") {
            return 1
        } else if (month == "Feb") {
            return 2
        } else if (month == "Mar") {
            return 3
        } else if (month == "Apr") {
            return 4
        } else if (month == "May") {
            return 5
        } else if (month == "Jun") {
            return 6
        } else if (month == "Jul") {
            return 7
        } else if (month == "Aug") {
            return 8
        } else if (month == "Sep") {
            return 9
        } else if (month == "Oct") {
            return 10
        } else if (month == "Nov") {
            return 11
        } else if (month == "Dec") {
            return 12
        }
        return 0
    }
    //Profile Part
    private fun invokeCamera() {
        pictureUri = FileProvider.getUriForFile(
                this@ViewRequestActivity,
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
                driverDB.child(driverUserId).child("Profile").setValue(profileImageUrl)
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

}
