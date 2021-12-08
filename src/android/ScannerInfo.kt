package de.impacgroup.mlbarcodescanner.module

import android.os.Parcel
import android.os.Parcelable

data class ScannerInfo (
    val title: String?,
    val infoText: String?,
    val button: ScannerButton?
): Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readParcelable(ScannerButton::class.java.classLoader)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(infoText)
        parcel.writeParcelable(button, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ScannerInfo> {
        override fun createFromParcel(parcel: Parcel): ScannerInfo {
            return ScannerInfo(parcel)
        }

        override fun newArray(size: Int): Array<ScannerInfo?> {
            return arrayOfNulls(size)
        }
    }
}
