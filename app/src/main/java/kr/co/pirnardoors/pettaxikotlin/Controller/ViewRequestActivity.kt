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
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Adapter
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_view_request.*
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.LISTVIEW

class ViewRequestActivity : AppCompatActivity() {
    var request = ArrayList<String>()
    var requestuserId = ArrayList<String>()
    var requestLatitudes = ArrayList<Double>()
    var requestLongitudes = ArrayList<Double>()
    var auth = FirebaseAuth.getInstance()
    var locationManager : LocationManager? = null
    var locationListener : LocationListener? = null
    var lastKnowLocation : Location? = null
    var adapter :ArrayAdapter<String>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_request)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, request)


        request.add("test")
        listView.adapter = adapter



        // Find driver location
        if (locationManager == null) {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }

        locationListener = object : LocationListener {
            override fun onLocationChanged(p0: Location?) {
                var location = p0
                if (location != null) {
                    updateListView(location)
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
        lastKnowLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if(lastKnowLocation != null) {
            updateListView(lastKnowLocation!!)
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
                    request.clear()
                    requestLatitudes.clear()
                    requestLongitudes.clear()
                    var children = dataSnapShot.children
                    for (data in children) {
                        var userId = data.key
                        var requestLatitude = data.child("l").child("0").getValue() as Double
                        var requestLongitued = data.child("l").child("1").getValue() as Double
                        Log.d(LISTVIEW, userId)
                        Log.d(LISTVIEW, requestLatitude.toString())
                        Log.d(LISTVIEW, requestLongitued.toString())
                        var destination = Location("Destination")
                        destination.latitude = requestLatitude
                        destination.longitude = requestLongitued
                        var distanceKm = driverLocation.distanceTo(destination) / 1000
                        var distanceRound = Math.round(distanceKm)
                        Log.d(LISTVIEW + "distance =", distanceRound.toString())

                        if(distanceRound >= 0) {
                            request.add(distanceRound.toString() + "Km")
                            requestLatitudes.add(requestLatitude)
                            requestLongitudes.add(requestLongitued)
                            requestuserId.add(userId)
                        }

                        adapter!!.notifyDataSetChanged()

                    }
                }
               }
        })
    }
}
