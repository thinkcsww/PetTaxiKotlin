package kr.co.pirnardoors.pettaxikotlin.Model

import android.os.Parcel
import android.os.Parcelable

/**
 * Created by std on 2018-02-07.
 */
class Request constructor(var requestLatitude : Double, var requestLongitude : Double,
                          var requestUserId : String, var driverLatitude : Double, var driverLongitude : Double,
                          var requestDestination: String, var requestNumber: String, var requestDistance : Double) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readDouble(),
            parcel.readDouble(),
            parcel.readString(),
            parcel.readDouble(),
            parcel.readDouble(),
            parcel.readString(),
            parcel.readString(),
            parcel.readDouble()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(requestLatitude)
        parcel.writeDouble(requestLongitude)
        parcel.writeString(requestUserId)
        parcel.writeDouble(driverLatitude)
        parcel.writeDouble(driverLongitude)
        parcel.writeString(requestDestination)
        parcel.writeString(requestNumber)
        parcel.writeDouble(requestDistance)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Request> {
        override fun createFromParcel(parcel: Parcel): Request {
            return Request(parcel)
        }

        override fun newArray(size: Int): Array<Request?> {
            return arrayOfNulls(size)
        }
    }

}
