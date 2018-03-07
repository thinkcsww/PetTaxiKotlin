package kr.co.pirnardoors.pettaxikotlin.Controller


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import kotlinx.android.synthetic.main.fragment_business_explain_fragment1.*

import kr.co.pirnardoors.pettaxikotlin.R
import org.jetbrains.anko.support.v4.toast


/**
 * A simple [Fragment] subclass.
 */
class BusinessExplainFragment1 : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_business_explain_fragment1, container, false)

    }


}// Required empty public constructor
