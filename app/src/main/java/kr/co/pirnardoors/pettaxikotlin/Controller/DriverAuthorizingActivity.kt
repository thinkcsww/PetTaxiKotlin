package kr.co.pirnardoors.pettaxikotlin.Controller

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build.ID
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.text.TextUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_driver_authorizing.*
import kr.co.pirnardoors.pettaxikotlin.Manifest
import kr.co.pirnardoors.pettaxikotlin.Model.License
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.*
import org.jetbrains.anko.toast
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class DriverAuthorizingActivity : AppCompatActivity() {



    lateinit var pictureUri: Uri
    lateinit var filePath : Uri
    var mStorage = FirebaseStorage.getInstance().getReference()
    var userId = FirebaseAuth.getInstance().currentUser?.uid
    var driverDB = FirebaseDatabase.getInstance().getReference("Driver").child(userId)
    var license  = License("", "", "", "", "")
    var Id : String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_authorizing)
        var sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()
        Id = sharedPreferences.getString(DRIVER_NICKNAME, "")
        if (Id.equals("")) {
            Id = intent.getStringExtra(DRIVER_NICKNAME)
            editor.putString(DRIVER_NICKNAME, ID)
            editor.apply()
        }

        nextBtn.setOnClickListener {
            val intent = Intent(this, DriverBusinessActivity::class.java)
            startActivity(intent)
            finish()
            return@setOnClickListener
        }

        licenseImageView.setOnClickListener {

            val driverAuthAlertDialog = AlertDialog.Builder(this@DriverAuthorizingActivity)
            val driverAuthDialogView = layoutInflater.inflate(R.layout.layout_driver_auth_image_view, null)
            driverAuthAlertDialog.setView(driverAuthDialogView)
            val selectPictureBtn : Button = driverAuthDialogView.findViewById(R.id.selectPictureBtn)
            val takePictureBtn : Button = driverAuthDialogView.findViewById(R.id.takePictureBtn)
            val dialog = driverAuthAlertDialog.create()
            selectPictureBtn.setOnClickListener {
                choose()
                dialog.dismiss()
            }
            takePictureBtn.setOnClickListener {
                if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this,android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@DriverAuthorizingActivity,
                            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    android.Manifest.permission.CAMERA),
                            REQUEST_PERMISSIONS)
                } else {
                    invokeCamera()
                    dialog.dismiss()
                }
            }
            dialog.show()

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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this@DriverAuthorizingActivity, "권한이 주어졌습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@DriverAuthorizingActivity, "권한이 주어지지 않았습니다. 설정을 변경해주세요.", Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun invokeCamera() {
        pictureUri = FileProvider.getUriForFile(this@DriverAuthorizingActivity, applicationContext.packageName + ".provider", createImageFile())

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        intent.putExtra(MediaStore.EXTRA_OUTPUT, pictureUri)

        intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        startActivityForResult(intent, DRIVER_AUTH_INTENT_CAMERA)
    }

    private fun createImageFile(): File {
        val picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss")
        var timeStamp = sdf.format(Date())

        val imageFile = File(picturesDirectory, "picture" + timeStamp + ".jpg")
        return imageFile
    }


    private fun uploadImage() {
        if(Id != "") {
            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Uploading...")
            progressDialog.show()
            mStorage.child(Id).child("License").putFile(filePath).addOnSuccessListener {
                progressDialog.dismiss()
            }
                    .addOnFailureListener {
                        progressDialog.dismiss();
                        toast("업로드 실패")
                    }
                    .addOnProgressListener {
                        var progress = (100 * it.bytesTransferred / it.totalByteCount).toInt()
                        progressDialog.setMessage("$progress%" )
                        if(progress > 99) {
                            val intent = Intent(this@DriverAuthorizingActivity, DriverBusinessActivity::class.java)
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

        } else if (requestCode == DRIVER_AUTH_INTENT_CAMERA && resultCode == Activity.RESULT_OK){
            toast("Saved")
            filePath = pictureUri
            try{
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                licenseImageView.setImageBitmap(bitmap)
                licenseImageView.setScaleType(ImageView.ScaleType.FIT_XY)
            } catch (e : IOException) {
                e.message
            }
        }
    }

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
}
