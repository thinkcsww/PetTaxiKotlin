package kr.co.pirnardoors.pettaxikotlin.Controller


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.PROFILEURL
import org.jetbrains.anko.find
import java.lang.Exception


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
        val imageProgressBar : ProgressBar = view.findViewById(R.id.imageProgressBar)
        if(profileImageUrl != "") {
            imageProgressBar.visibility = View.VISIBLE
            Glide.with(this@ProfileImageFragment).load(profileImageUrl)
                    .listener(object : RequestListener<String, GlideDrawable> {
                        override fun onException(e: Exception?, model: String?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                            imageProgressBar.visibility = View.GONE
                            return false
                        }

                        override fun onResourceReady(resource: GlideDrawable?, model: String?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                            imageProgressBar.visibility = View.GONE
                            return false                        }

                    })
                    .into(profileImageView)
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
