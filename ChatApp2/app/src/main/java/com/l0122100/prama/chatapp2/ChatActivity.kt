package com.l0122100.prama.chatapp2

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.l0122100.prama.chatapp2.adapter.MessageAdapter
import com.l0122100.prama.chatapp2.databinding.ActivityChatBinding
import com.l0122100.prama.chatapp2.model.Message
import java.util.Calendar
import java.util.Date

class ChatActivity : AppCompatActivity() {

    private var binding: ActivityChatBinding? = null
    private var adapter: MessageAdapter? = null
    private var messages: ArrayList<Message>? = null
    private var senderRoom: String? = null
    private var receiverRoom: String? = null
    private var database: FirebaseDatabase? = null
    private var storage: FirebaseStorage? = null
    private var dialog: ProgressDialog? = null
    private var senderUid: String? = null
    private var receiverUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        setSupportActionBar(binding!!.toolbar)
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        dialog = ProgressDialog(this@ChatActivity).apply {
            setMessage("Uploading image..")
            setCancelable(false)
        }
        messages = ArrayList()

        val name = intent.getStringExtra("name")
        val profile = intent.getStringExtra("image")
        binding!!.name.text = name
        Glide.with(this@ChatActivity)
            .load(profile)
            .placeholder(R.drawable.placeholder)
            .into(binding!!.profile)

        binding!!.imageView.setOnClickListener { finish() }

        receiverUid = intent.getStringExtra("uid")
        senderUid = FirebaseAuth.getInstance().uid

        setupPresenceListener()
        setupChatRooms()
        setupRecyclerView()
        setupSendButton()
        setupAttachmentButton()

        val handler = Handler()
        binding!!.messageBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // You can add any additional logic here if needed
            }

            override fun afterTextChanged(s: Editable?) {
                database!!.reference.child("Presence").child(senderUid!!).setValue("Typing...")
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed(userStoppedTyping, 1000)
            }

            var userStoppedTyping = Runnable {
                database!!.reference.child("Presence").child(senderUid!!).setValue("Online")
            }
        })

        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun setupPresenceListener() {
        database!!.reference.child("Presence").child(receiverUid!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val status = snapshot.getValue(String::class.java)
                        if (status == "offline") {
                            binding!!.status.visibility = View.GONE
                        } else {
                            binding!!.status.text = status
                            binding!!.status.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatActivity", "Failed to retrieve presence: ${error.message}")
                }
            })
    }

    private fun setupChatRooms() {
        senderRoom = senderUid + receiverUid
        receiverRoom = receiverUid + senderUid

        database!!.reference.child("chats").child(senderRoom!!).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messages!!.clear()
                    for (snapshot1 in snapshot.children) {
                        val message: Message? = snapshot1.getValue(Message::class.java)
                        message?.let {
                            it.messageId = snapshot1.key
                            messages!!.add(it)
                        }
                    }
                    adapter!!.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatActivity", "Failed to retrieve messages: ${error.message}")
                }
            })
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter(this@ChatActivity, messages, senderRoom!!, receiverRoom!!)
        binding!!.recyclerview.layoutManager = LinearLayoutManager(this@ChatActivity)
        binding!!.recyclerview.adapter = adapter
    }

    private fun setupSendButton() {
        binding!!.send.setOnClickListener {
            val messageTxt: String = binding!!.messageBox.text.toString()
            if (messageTxt.isNotEmpty()) {
                sendMessage(messageTxt, "text")
            }
        }
    }

    private fun setupAttachmentButton() {
        binding!!.attachment.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "image/*"
            startActivityForResult(intent, 25)
        }
    }

    private fun sendMessage(content: String, type: String) {
        val date = Date()
        val message = Message(content, senderUid, date.time)
        if (type == "photo") {
            message.message = "photo"
            message.imageUrl = content
        }
        binding!!.messageBox.setText("")
        val randomKey = database!!.reference.push().key
        val lastMsgObj = hashMapOf(
            "lastMsg" to message.message!!,
            "lastMsgTime" to date.time
        )

        val castedLastMsgObj = lastMsgObj as Map<String, Any>

        database!!.reference.child("chats").child(senderRoom!!).updateChildren(castedLastMsgObj)
        database!!.reference.child("chats").child(receiverRoom!!).updateChildren(castedLastMsgObj)
        database!!.reference.child("chats").child(senderRoom!!)
            .child("messages")
            .child(randomKey!!)
            .setValue(message).addOnSuccessListener {
                database!!.reference.child("chats")
                    .child(receiverRoom!!)
                    .child("messages")
                    .child(randomKey)
                    .setValue(message)
            }.addOnFailureListener {
                Log.e("ChatActivity", "Failed to send message: ${it.message}")
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 25 && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val selectedImage = data.data
            val calendar = Calendar.getInstance()
            val reference = storage!!.reference.child("chats").child(calendar.timeInMillis.toString() + "")
            dialog!!.show()
            reference.putFile(selectedImage!!)
                .addOnCompleteListener { task ->
                    dialog!!.dismiss()
                    if (task.isSuccessful) {
                        reference.downloadUrl.addOnSuccessListener { uri ->
                            val filePath = uri.toString()
                            sendMessage(filePath, "photo")
                        }
                    } else {
                        Log.e("ChatActivity", "Failed to upload image: ${task.exception?.message}")
                    }
                }
        }
    }

    override fun onResume() {
        super.onResume()
        val currentId = FirebaseAuth.getInstance().uid
        database!!.reference.child("Presence").child(currentId!!).setValue("Online")
    }

    override fun onPause() {
        super.onPause()
        val currentId = FirebaseAuth.getInstance().uid
        database!!.reference.child("Presence").child(currentId!!).setValue("Offline")
    }
}
