package kr.co.pirnardoors.pettaxikotlin.Controller


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import kotlinx.android.synthetic.main.fragment_business_explain_fragment1.*

import kr.co.pirnardoors.pettaxikotlin.R
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.toast


/**
 * A simple [Fragment] subclass.
 */
class BusinessExplainFragment1 : Fragment(), View.OnClickListener {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view : View =  inflater.inflate(R.layout.fragment_business_explain_fragment1, container, false)
        val nextBtn : Button = view.findViewById(R.id.nextBtn)
        nextBtn.setOnClickListener(this@BusinessExplainFragment1)

        return view
    }

    override fun onClick(view: View?) {
        var fragment = Fragment()
        when(view?.id) {
            R.id.nextBtn -> {
                fragment = BusinessExplainFragment2()
                replaceFragment(fragment)
            }

            else -> {

            }
        }
    }

    private fun replaceFragment(fragment : Fragment) {
        val transaction = fragmentManager!!.beginTransaction()
        transaction.replace(R.id.holder, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }


}// Required empty public constructor
