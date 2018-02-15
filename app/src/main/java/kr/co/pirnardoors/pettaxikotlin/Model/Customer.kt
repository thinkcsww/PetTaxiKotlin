package kr.co.pirnardoors.pettaxikotlin.Model

import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng

/**
 * Created by std on 2018-02-06.
 */
class Customer constructor (var requestActive : Boolean, var driverActive : Boolean, var carInfo : String,
                            var driverUserId : String, var phoneNumber : String) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readByte() != 0.toByte(),
            parcel.readByte() != 0.toByte(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (requestActive) 1 else 0)
        parcel.writeByte(if (driverActive) 1 else 0)
        parcel.writeString(carInfo)
        parcel.writeString(driverUserId)
        parcel.writeString(phoneNumber)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Customer> {
        override fun createFromParcel(parcel: Parcel): Customer {
            return Customer(parcel)
        }

        override fun newArray(size: Int): Array<Customer?> {
            return arrayOfNulls(size)
        }
    }

}