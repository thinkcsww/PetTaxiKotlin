package kr.co.pirnardoors.pettaxikotlin.Model

import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng

/**
 * Created by std on 2018-02-06.
 */
class Customer constructor(var destinationLatitude : Double, var destinationLongitude : Double,
                           var requestActive : Boolean, var driverActive : Boolean, var carNumber : String,
                           var phoneNumber : String, var destination : String, var driverUserId : String,
                           var destinationLatLng : LatLng, var number : String) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readDouble(),
            parcel.readDouble(),
            parcel.readByte() != 0.toByte(),
            parcel.readByte() != 0.toByte(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readParcelable(LatLng::class.java.classLoader),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(destinationLatitude)
        parcel.writeDouble(destinationLongitude)
        parcel.writeByte(if (requestActive) 1 else 0)
        parcel.writeByte(if (driverActive) 1 else 0)
        parcel.writeString(carNumber)
        parcel.writeString(phoneNumber)
        parcel.writeString(destination)
        parcel.writeString(driverUserId)
        parcel.writeParcelable(destinationLatLng, flags)
        parcel.writeString(number)
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