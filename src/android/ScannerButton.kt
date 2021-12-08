package de.impacgroup.mlbarcodescanner.module

import android.os.Parcel
import android.os.Parcelable

data class ScannerButton(
    val title: String?,
    val tintColor: String?,
    val backgroundColor: String?,
    val roundedCorners: Boolean
): Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(tintColor)
        parcel.writeString(backgroundColor)
        parcel.writeByte(if (roundedCorners) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ScannerButton> {
        override fun createFromParcel(parcel: Parcel): ScannerButton {
            return ScannerButton(parcel)
        }

        override fun newArray(size: Int): Array<ScannerButton?> {
            return arrayOfNulls(size)
        }
    }
}
