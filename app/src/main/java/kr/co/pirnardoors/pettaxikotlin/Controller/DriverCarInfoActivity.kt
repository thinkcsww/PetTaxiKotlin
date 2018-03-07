package kr.co.pirnardoors.pettaxikotlin.Controller

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.media.Image
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.widget.Button
import android.widget.ImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_driver_car_info.*
import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.*
import org.jetbrains.anko.toast
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class DriverCarInfoActivity : AppCompatActivity() {

    lateinit var filePath : Uri
    lateinit var pictureUri : Uri
    var mStorage = FirebaseStorage.getInstance().getReference()
    var userId = FirebaseAuth.getInstance().currentUser?.uid
    var driverDB = FirebaseDatabase.getInstance().getReference("Driver").child(userId)
    var Id = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_car_info)
        var sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var editor = sharedPreferences.edit()
        Id = sharedPreferences.getString(DRIVER_ID, "")
        carImageView.setOnClickListener {
            val carInfoAlerDialog = AlertDialog.Builder(this@DriverCarInfoActivity)
            val carInfoDialogView = layoutInflater.inflate(R.layout.layout_driver_auth_image_view, null)
            carInfoAlerDialog.setView(carInfoDialogView)
            val takePictureBtn : Button = carInfoDialogView.findViewById(R.id.takePictureBtn)
            val selectPictureBtn : Button = carInfoDialogView.findViewById(R.id.selectPictureBtn)
            val dialog = carInfoAlerDialog.create()
            selectPictureBtn.setOnClickListener {
                chooseImage()
                dialog.dismiss()
            }
            takePictureBtn.setOnClickListener {
                invokeCamera()
                dialog.dismiss()
            }
            dialog.show()
        }

        submitBtn.setOnClickListener {
            var carNumber = carNumberEditText.text.trim().toString()
            var carModel = carModelEditText.text.trim().toString()
            var carOwner = carOwnerEditText.text.trim().toString()
            var carColor = carColorEditText.text.trim().toString()
            driverDB.child("CarNumber").setValue(carNumber)
            driverDB.child("CarColor").setValue(carColor)
            driverDB.child("CarOwner").setValue(carOwner)
            driverDB.child("CarModel").setValue(carModel)
            driverDB.child("ReadyToTest").setValue("true")
            uploadImage()
        }
    }

    private fun invokeCamera() {
        pictureUri = FileProvider.getUriForFile(this@DriverCarInfoActivity, applicationContext.packageName + ".provider", createImageFile())

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, pictureUri)
        intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        startActivityForResult(intent, DRIVER_AUTH_INTENT_CAMERA)
    }

    private fun createImageFile(): File {
        val picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss")
        var timestamp = sdf.format(Date())

        val imageFile = File(picturesDirectory, "picture $timestamp.jpg")
        return imageFile
    }

    private fun chooseImage() {
        val intent = Intent()
        intent.setType("image/*")
        intent.setAction(Intent.ACTION_PICK)
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), CAR_IMAGE_INTENT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == CAR_IMAGE_INTENT && resultCode == Activity.RESULT_OK && data != null
        && data.getData() != null) {
            filePath = data.getData()
            try{
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                carImageView.setImageBitmap(bitmap)
                carImageView.scaleType = ImageView.ScaleType.FIT_XY
            } catch (e : IOException) {
                e.message
            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == DRIVER_AUTH_INTENT_CAMERA) {
            filePath = pictureUri
            try{
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                carImageView.setImageBitmap(bitmap)
                carImageView.scaleType = ImageView.ScaleType.FIT_XY
            } catch (e: IOException) {
                e.message
            }
        }
    }

    private fun uploadImage() {
        if(filePath != null && Id != "") {
            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Uploading...")
            progressDialog.show()
            mStorage.child(Id).child("CarImage").putFile(filePath).addOnSuccessListener {
                progressDialog.dismiss()
            }.addOnFailureListener {
                        progressDialog.dismiss()
                        toast("업로드 실패")
                    }.addOnProgressListener {
                        var progress = (100 * it.bytesTransferred / it.totalByteCount).toInt()
                        progressDialog.setMessage("$progress%")
                        if(progress > 99) {
                            val intent = Intent(this, WaitingAuthActivity::class.java)
                            startActivity(intent)
                            finish()
                            return@addOnProgressListener
                        }
                    }
        }
    }
}
