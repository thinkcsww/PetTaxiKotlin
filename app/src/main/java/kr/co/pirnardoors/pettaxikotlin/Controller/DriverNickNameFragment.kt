package kr.co.pirnardoors.pettaxikotlin.Controller


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.fragment_driver_nick_name.*

import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.DRIVER_NICKNAME
import org.jetbrains.anko.support.v4.toast


/**
 * A simple [Fragment] subclass.
 */
class DriverNickNameFragment : Fragment(), View.OnClickListener {
    val driverDB = FirebaseDatabase.getInstance().getReference("Driver")
    val nicknameDB = FirebaseDatabase.getInstance().getReference("Nickname")
    val driverUserId = FirebaseAuth.getInstance().currentUser!!.uid
    var id : String? = ""
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view : View =  inflater.inflate(R.layout.fragment_driver_nick_name, container, false)
        val okBtn : Button = view.findViewById(R.id.okBtn)
        val idEditText : EditText = view.findViewById(R.id.idEditText)
        okBtn.setOnClickListener(this@DriverNickNameFragment)

        return view

    }
    override fun onClick(view: View?) {
        when(view?.id) {
            R.id.okBtn -> {
                id = idEditText.text.toString()
                if(id != "") {
                    checkIfIdExists(id.toString())
                } else {
                    toast("닉네임을 입력해주세요.")
                }


            }
        }
    }
    private fun checkIfIdExists(id : String){
        var existence = false
        var looping = false
        nicknameDB.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError?) {
                if (p0 != null) p0.message
            }

            override fun onDataChange(dataSnapshot: DataSnapshot?) {
                if(dataSnapshot != null) {
                    var children = dataSnapshot.children
                    if (looping == false) {
                        for (data in children) {
                            Log.d("NickName id : ", id)
                            if (id == data.key.toString() && looping == false) {
                                existence = true
                                Log.d("NickName", "i'm working")
                                Log.d("NickName", data.key.toString())
                                toast("이미 존재하는 닉네임입니다.")
                                return
                            }
                        }
                        if (existence == false) {
//                            val transaction = fragmentManager?.beginTransaction()
//                            transaction?.remove(this@DriverNickNameFragment)
//                            transaction?.commit()
                            nicknameDB.child(id).setValue("")
                            driverDB.child(driverUserId).child("Id").setValue(id)
                            looping = true
//                            val intent = Intent(activity, DriverAuthorizingActivity::class.java)
//                            intent.putExtra(DRIVER_NICKNAME, id)
//                            startActivity(intent)
//                            activity!!.finish()
                        }
                    }

                }
            }
        })
    }

}// Required empty public constructor
