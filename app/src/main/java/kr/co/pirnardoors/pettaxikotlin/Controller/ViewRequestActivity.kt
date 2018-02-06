package kr.co.pirnardoors.pettaxikotlin.Controller

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_view_request.*
import kr.co.pirnardoors.pettaxikotlin.R

class ViewRequestActivity : AppCompatActivity() {

    var auth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_request)

        logoutBtn.setOnClickListener {
            auth.signOut()

        }
    }
}
