package kr.co.pirnardoors.pettaxikotlin.Controller


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import kr.co.pirnardoors.pettaxikotlin.R


/**
 * A simple [Fragment] subclass.
 */
class DriverFinishFragment : Fragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_driver_finish, container, false)
    }

}// Required empty public constructor
