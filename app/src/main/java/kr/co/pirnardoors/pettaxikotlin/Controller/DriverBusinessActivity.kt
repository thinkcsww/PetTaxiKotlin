package kr.co.pirnardoors.pettaxikotlin.Controller

import android.app.Activity
import android.app.FragmentManager
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.ImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_driver_business.*
import kotlinx.android.synthetic.main.fragment_business_explain_fragment1.*

import kr.co.pirnardoors.pettaxikotlin.R
import kr.co.pirnardoors.pettaxikotlin.Utilities.*
import org.jetbrains.anko.toast
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class DriverBusinessActivity : AppCompatActivity() {
    lateinit var filePath : Uri
    lateinit var pictureUri : Uri
    val fragmentManager = supportFragmentManager
    var Id = ""

    var mStorage = FirebaseStorage.getInstance().getReference()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_business)
        val sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        Id = sharedPreferences.getString(DRIVER_NICKNAME, "")
        businessImageView.setOnClickListener {
            val driverBusinessAlertDialog = AlertDialog.Builder(this@DriverBusinessActivity)
            val driverBusinessDialogView = layoutInflater.inflate(R.layout.layout_driver_auth_image_view, null)
            driverBusinessAlertDialog.setView(driverBusinessDialogView)
            val dialog = driverBusinessAlertDialog.create()
            val selectPictureBtn : Button = driverBusinessDialogView.findViewById(R.id.selectPictureBtn)
            val takePictureBtn : Button = driverBusinessDialogView.findViewById(R.id.takePictureBtn)

            selectPictureBtn.setOnClickListener {
                choose()
                dialog.dismiss()
            }

            takePictureBtn.setOnClickListener {
                invokeCamera()
                dialog.dismiss()
            }
            dialog.show()
        }

        uploadBtn.setOnClickListener {
            if(businessImageView.drawable == null) {
                toast("사진을 설정해주세요.")
            } else {
                uploadImage()
            }
        }

        explainBtn.setOnClickListener {
            val transaction = fragmentManager.beginTransaction()
            val businessFragment1 = BusinessExplainFragment1()
            transaction.replace(R.id.holder, businessFragment1)
            transaction.addToBackStack(null)
            transaction.commit()
            uploadBtn.visibility = View.GONE
        }

        backBtn.setOnClickListener {
            super.onBackPressed()
            return@setOnClickListener
        }


    }

    private fun invokeCamera() {
        pictureUri = FileProvider.getUriForFile(this@DriverBusinessActivity, applicationContext.packageName + ".provider", createImageFile())
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, pictureUri)
        intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        startActivityForResult(intent, DRIVER_AUTH_INTENT_CAMERA)
    }

    private fun createImageFile(): File {
        val pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss")
        val timeStamp = sdf.format(Date())

        val imageFile = File(pictureDirectory, "picture$timeStamp.jpg")
        return imageFile
    }

    private fun choose() {
        val intent = Intent()
        intent.setType("image/*")
        intent.setAction(Intent.ACTION_PICK)
        startActivityForResult(intent, BUSINESS_IMAGE_INTENT)
    }

    private fun uploadImage() {
        if (Id != "") {
            val progressDialog = ProgressDialog(this@DriverBusinessActivity)
            progressDialog.setTitle("Uploading...")
            progressDialog.show()
            mStorage.child(Id).child("Business").putFile(filePath).addOnSuccessListener {
                progressDialog.dismiss()
            }.addOnFailureListener {
                        progressDialog.dismiss()
                        toast("업로드 실패")
                    }.addOnProgressListener {
                        var progress = (100 * it.bytesTransferred / it.totalByteCount).toInt()
                        progressDialog.setMessage("$progress%")
                        if(progress > 99) {
                            val intent = Intent(this@DriverBusinessActivity, DriverCarInfoActivity::class.java)
                            startActivity(intent)
                        }
                    }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode == Activity.RESULT_OK && requestCode == BUSINESS_IMAGE_INTENT) {
            filePath = data!!.data
            try{
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                businessImageView.setImageBitmap(bitmap)
                businessImageView.scaleType = ImageView.ScaleType.FIT_XY
            } catch (e : IOException) {

            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == DRIVER_AUTH_INTENT_CAMERA) {
            filePath = pictureUri
            try{
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                businessImageView.setImageBitmap(bitmap)
                businessImageView.scaleType = ImageView.ScaleType.FIT_XY
            } catch (e : IOException) {

            }
        }
    }
}
