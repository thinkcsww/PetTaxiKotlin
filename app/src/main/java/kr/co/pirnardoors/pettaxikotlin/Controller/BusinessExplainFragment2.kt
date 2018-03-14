package kr.co.pirnardoors.pettaxikotlin.Controller


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

import kr.co.pirnardoors.pettaxikotlin.R
import org.jetbrains.anko.support.v4.toast


/**
 * A simple [Fragment] subclass.
 */
class BusinessExplainFragment2 : Fragment(), View.OnClickListener {



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view : View = inflater.inflate(R.layout.fragment_business_explain_fragment2, container, false)
        val nextBtn : Button = view.findViewById(R.id.nextBtn)
        val backBtn : Button = view.findViewById(R.id.backBtn)
        nextBtn.setOnClickListener(this@BusinessExplainFragment2)
        backBtn.setOnClickListener(this@BusinessExplainFragment2)
        return view
    }
    override fun onClick(view: View?) {
        var fragment : Fragment
        when(view!!.id) {
            R.id.nextBtn -> {
                toast("SDFSD")
            }

            R.id.backBtn -> {
                fragment = BusinessExplainFragment1()
                backFragment(fragment)
            }
        }
    }

    private fun backFragment(fragment : Fragment) {
        val transaction = fragmentManager!!.beginTransaction()

        if(fragmentManager!!.backStackEntryCount > 0) {
            fragmentManager!!.popBackStack(fragmentManager!!.getBackStackEntryAt(0).id, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            transaction.replace(R.id.holder, fragment)
            transaction.addToBackStack(null)
            transaction.commit()

        }

    }



}// Required empty public constructor
