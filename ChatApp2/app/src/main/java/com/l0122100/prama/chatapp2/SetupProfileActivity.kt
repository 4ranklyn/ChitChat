package com.l0122100.prama.chatapp2

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.l0122100.prama.chatapp2.databinding.ActivitySetupProfileBinding
import com.l0122100.prama.chatapp2.model.User
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

class SetupProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private var selectedImage: Uri? = null
    private lateinit var dialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dialog = ProgressDialog(this)
        dialog.setMessage("Updating Profile...")
        dialog.setCancelable(false)

        // Initialize Firebase components
        database = FirebaseDatabase.getInstance("https://appchat2-4d966-default-rtdb.asia-southeast1.firebasedatabase.app")
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()

        supportActionBar?.hide()

        binding.imageView.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(intent, 45)
        }

        binding.continueBtn02.setOnClickListener {
            val name: String = binding.nameBox.text.toString()
            if (name.isEmpty()) {
                binding.nameBox.error = "Please enter your name"
            } else {
                dialog.show()
                if (selectedImage != null) {
                    uploadImageAndSaveUser(name)
                } else {
                    saveUser(name, "No Image")
                }
            }
        }

        // Load existing user data if available
        loadUserData()
    }

    private fun uploadImageAndSaveUser(name: String) {
        val reference = storage.reference.child("Profile").child(auth.uid!!)
        reference.putFile(selectedImage!!)
            .addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
                dialog.setMessage("Uploading: ${progress.toInt()}%")
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    reference.downloadUrl.addOnCompleteListener { uri ->
                        val imageUrl = uri.result.toString()
                        saveUser(name, imageUrl)
                    }
                } else {
                    dialog.dismiss()
                    Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUser(name: String, imageUrl: String) {
        val uid = auth.uid
        val phone = auth.currentUser!!.phoneNumber
        val user = User(uid, name, phone, imageUrl)
        database.reference.child("users").child(uid!!).setValue(user)
            .addOnCompleteListener {
                dialog.dismiss()
                if (it.isSuccessful) {
                    startActivity(Intent(this@SetupProfileActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Profile update failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loadUserData() {
        val uid = auth.uid ?: return
        val userRef = database.reference.child("users").child(uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        binding.nameBox.setText(user.name)
                        if (user.profileImage != "No Image") {
                            binding.imageView.setImageURI(Uri.parse(user.profileImage))
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 45 && resultCode == RESULT_OK && data != null && data.data != null) {
            val uri = data.data
            binding.imageView.setImageURI(uri)
            selectedImage = uri
        }
    }
}
