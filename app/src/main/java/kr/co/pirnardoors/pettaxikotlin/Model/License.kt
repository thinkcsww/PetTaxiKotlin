package kr.co.pirnardoors.pettaxikotlin.Model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.synthetic.main.activity_driver_authorizing.*
import kr.co.pirnardoors.pettaxikotlin.R.id.*

/**
 * Created by std on 2018-02-23.
 */
class License constructor(var licenseNumber : String, var licenseType : String, var licenseExpire : String,
                          var licenseAuthNumber : String, var birth : String) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(licenseNumber)
        parcel.writeString(licenseType)
        parcel.writeString(licenseExpire)
        parcel.writeString(licenseAuthNumber)
        parcel.writeString(birth)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<License> {
        override fun createFromParcel(parcel: Parcel): License {
            return License(parcel)
        }

        override fun newArray(size: Int): Array<License?> {
            return arrayOfNulls(size)
        }
    }

}