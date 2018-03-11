package kr.co.pirnardoors.pettaxikotlin.Controller


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import com.bumptech.glide.Glide

import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.PROFILEURL
import org.jetbrains.anko.find


/**
 * A simple [Fragment] subclass.
 */
class ProfileImageFragment : Fragment(), View.OnClickListener {



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view : View = inflater.inflate(R.layout.fragment_profile_image, container, false)

        val closeBtn : Button = view.findViewById(R.id.closeBtn)
        val profileImageView : ImageView = view.findViewById(R.id.profileImageView)
        val profileImageUrl = arguments!!.getString(PROFILEURL)
        if(profileImageUrl != "") {
            Glide.with(this@ProfileImageFragment).load(profileImageUrl).into(profileImageView)
        }
        closeBtn.setOnClickListener(this@ProfileImageFragment)
        return view
    }

    override fun onClick(view: View?) {
        val transaction = fragmentManager!!.beginTransaction()
        when(view!!.id) {
            R.id.closeBtn -> {
                transaction.remove(this@ProfileImageFragment)
                transaction.commit()
            }

        }
    }

}// Required empty public constructor
