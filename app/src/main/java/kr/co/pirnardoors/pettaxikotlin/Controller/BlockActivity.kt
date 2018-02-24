package kr.co.pirnardoors.pettaxikotlin.Controller

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_block.*
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.DRIVER_ID
import kr.co.pirnardoors.pettaxikotlin.Utilities.PREF_NAME

class BlockActivity : AppCompatActivity() {
    var userId = FirebaseAuth.getInstance().currentUser?.uid
    val driverDB = FirebaseDatabase.getInstance().getReference("Driver").child(userId)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block)
        var sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()
        var Id = sharedPreferences.getString(DRIVER_ID, "")
        welcomText.text = "${Id}님\n 반갑습니다!"




        nextBtn.setOnClickListener {
            val intent = Intent(this@BlockActivity, DriverAuthorizingActivity::class.java)
            startActivity(intent)
            finish()
            return@setOnClickListener
        }
    }
}
