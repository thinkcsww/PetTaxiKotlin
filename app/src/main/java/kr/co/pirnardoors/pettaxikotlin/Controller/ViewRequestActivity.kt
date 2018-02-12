package kr.co.pirnardoors.pettaxikotlin.Controller

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_view_request.*
import kr.co.pirnardoors.pettaxikotlin.Model.Request
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.EXTRA_REQUEST
import kr.co.pirnardoors.pettaxikotlin.Utilities.LISTVIEW
import java.util.*

class ViewRequestActivity : AppCompatActivity() {
    var request = ArrayList<String>()
    var requestUserId = ArrayList<String>()
    var requestDestinations = ArrayList<String>()
    //var requestTypes = ArrayList<String>()
    var requestNumbers = ArrayList<String>()
    var requestLatitudes = ArrayList<Double>()
    var requestLongitudes = ArrayList<Double>()
    var auth = FirebaseAuth.getInstance()
    var locationManager : LocationManager? = null
    var locationListener : LocationListener? = null
    var lastKnownLocation : Location? = null
    var adapter :ArrayAdapter<String>? = null
    var driverUserId : String? = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_request)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, request)
        listView.adapter = adapter

        driverUserId = FirebaseAuth.getInstance().currentUser?.uid


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
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return@setOnClickListener

        }

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

                    var req : Request = Request(0.0,0.0,"",0.0,0.0,"","")
                    req.requestLatitude = requestLatitudes[i]
                    req.requestLongitude = requestLongitudes[i]
                    req.driverLatitude = lastKnownLocation!!.latitude
                    req.driverLongitude = lastKnownLocation!!.longitude
                    req.requestUserId = requestUserId[i]
                    req.requestDestination = requestDestinations[i]
                    req.requestNumber = requestNumbers[i]
                    //req.requestType = requestTypes[i]

                    var intent = Intent(this@ViewRequestActivity, DriverMapActivity::class.java)
                    intent.putExtra(EXTRA_REQUEST, req)
                    startActivity(intent)
                }
            }

        }
    }
    //oncreate finish


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
                            var requestDestination = data.child("Destination").getValue()
                            //var requestType = data.child("Type").getValue()
                            var requestNumber = data.child("Number").getValue()
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
                                request.add("${distanceRound.toString()}Km,  " +
                                        "목적지: $requestDestination")
                                requestLatitudes.add(requestLatitude.toString().toDouble())
                                requestLongitudes.add(requestLongitued.toString().toDouble())
                                requestUserId.add(userId)
                                requestDestinations.add(requestDestination.toString())
                                //requestTypes.add(requestType.toString())
                                requestNumbers.add(requestNumber.toString())
                            }
                        }
//                        Collections.sort(request)
                        adapter!!.notifyDataSetChanged()

                    }
                }
               }
        })
    }
}
