package kr.co.pirnardoors.pettaxikotlin.Controller


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
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
import kotlinx.android.synthetic.main.fragment_customer_nick_name.*

import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.CUSTOMER_NICKNAME
import kr.co.pirnardoors.pettaxikotlin.Utilities.PREF_NAME
import org.jetbrains.anko.support.v4.toast


/**
 * A simple [Fragment] subclass.
 */
class CustomerNickNameFragment : Fragment(), View.OnClickListener {

    val customerDB = FirebaseDatabase.getInstance().getReference("Customer")
    val nicknameDB = FirebaseDatabase.getInstance().getReference("Nickname")
    val customerUserId = FirebaseAuth.getInstance().currentUser!!.uid
    var id : String? = ""
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view : View = inflater.inflate(R.layout.fragment_customer_nick_name, container, false)
        val okBtn : Button = view.findViewById(R.id.okBtn)
        val idEditText : EditText = view.findViewById(R.id.idEditText)
        okBtn.setOnClickListener(this@CustomerNickNameFragment)

        return view
    }

    override fun onClick(view: View?) {
        when(view?.id) {
            R.id.okBtn -> {
                id = idEditText.text.toString()
                if (id != "") {
                    checkIfIdExists(id.toString())
                } else {
                    toast("닉네임을 입력해주세요.")
                }
            }
        }
    }

    private fun checkIfIdExists(id : String) {
        nicknameDB.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError?) {
                if (p0 != null) p0.message
            }

            override fun onDataChange(p0: DataSnapshot?) {
                if (p0 != null) {
                    if (p0.hasChild(id)) {
                        toast("이미 존재하는 닉네임입니다.")
                    } else {
                        var sharedPreferences = activity!!.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        var editor = sharedPreferences.edit()
                        editor.putString(CUSTOMER_NICKNAME, id)
                        editor.apply()
                        val transaction = fragmentManager?.beginTransaction()
                        transaction?.remove(this@CustomerNickNameFragment)
                        transaction?.commit()
                        nicknameDB.child(id).setValue("")
                        customerDB.child(customerUserId).child("Id").setValue(id)
                        val intent = Intent(activity, CustomerMapActivity::class.java)
                        startActivity(intent)
                        activity!!.finish()
                    }
                }
            }

        })
    }


}// Required empty public constructor
