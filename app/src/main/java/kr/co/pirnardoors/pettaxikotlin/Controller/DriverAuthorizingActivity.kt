package kr.co.pirnardoors.pettaxikotlin.Controller

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.widget.ImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_driver_authorizing.*
import kr.co.pirnardoors.pettaxikotlin.Model.License
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.*
import org.jetbrains.anko.toast
import java.io.IOException

class DriverAuthorizingActivity : AppCompatActivity() {



    lateinit var filePath: Uri
    var mStorage = FirebaseStorage.getInstance().getReference()
    var userId = FirebaseAuth.getInstance().currentUser?.uid
    var driverDB = FirebaseDatabase.getInstance().getReference("Driver").child(userId)
    var license  = License("", "", "", "", "")

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putParcelable(LICENSE_PARCELABLE, license)
    }
    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        if(savedInstanceState != null) {
            license = savedInstanceState.getParcelable(EXTRA_CUSTOMER)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_authorizing)
        var sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()

        nextBtn.setOnClickListener {
            val intent = Intent(this, DriverCarInfoActivity::class.java)
            startActivity(intent)
            finish()
            return@setOnClickListener
        }

        licenseImageView.setOnClickListener {
            choose()
        }
        uploadBtn.setOnClickListener {
            license.licenseNumber = licenseNumberEditText.text.trim().toString()
            license.licenseType = licenseTypeEditText.text.trim().toString()
            license.licenseExpire = licenseExpireEditText.text.trim().toString()
            license.licenseAuthNumber = licenseAuthEditText.text.trim().toString()
            license.birth = licenseBirthEditText.text.trim().toString()
            if(licenseImageView.drawable == null) {
                toast("사진을 설정해주세요.")
            } else if(TextUtils.isEmpty(license.licenseNumber)
                    ||TextUtils.isEmpty(license.licenseType)
                    ||TextUtils.isEmpty(license.licenseExpire)
                    ||TextUtils.isEmpty(license.licenseAuthNumber)
                    ||TextUtils.isEmpty(license.birth)) {
                toast("정보를 입력해주세요.")
            } else {

                uploadImage()
                driverDB.child("LicenseNumber").setValue(license.licenseNumber)
                driverDB.child("licenseType").setValue(license.licenseType)
                driverDB.child("licenseExpire").setValue(license.licenseExpire)
                driverDB.child("licenseAuthNumber").setValue(license.licenseAuthNumber)
                driverDB.child("birth").setValue(license.birth)

            }
        }

    }

    private fun uploadImage() {
        if(filePath != null) {
            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Uploading...")
            progressDialog.show()
            mStorage.child(userId!!).putFile(filePath).addOnSuccessListener {
                progressDialog.dismiss()
            }
                    .addOnFailureListener {
                        progressDialog.dismiss()
                        toast("업로드 실패")
                    }
                    .addOnProgressListener {
                        var progress = (100 * it.bytesTransferred / it.totalByteCount).toInt()
                        progressDialog.setMessage("$progress%" )
                        if(progress > 99) {
                            val intent = Intent(this@DriverAuthorizingActivity, DriverCarInfoActivity::class.java)
                            startActivity(intent)
                        }
                    }

        }
    }

    private fun choose() {
        val intent = Intent()
        intent.setType("image/*")
        intent.setAction(Intent.ACTION_PICK)
        startActivityForResult(Intent.createChooser(intent,"Select Picture"), LICENSE_IMAGE_INTENT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == LICENSE_IMAGE_INTENT && resultCode == Activity.RESULT_OK
        && data != null && data.getData() != null) {
            filePath = data.getData()
            try{
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                licenseImageView.setImageBitmap(bitmap)
                licenseImageView.setScaleType(ImageView.ScaleType.FIT_XY)
            } catch (e:IOException) {
                e.message
            }

        }
    }
}
