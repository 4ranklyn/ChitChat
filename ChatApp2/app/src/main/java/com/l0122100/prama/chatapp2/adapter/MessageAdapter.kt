package com.l0122100.prama.chatapp2.adapter

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.l0122100.prama.chatapp2.R
import com.l0122100.prama.chatapp2.databinding.DeleteLayoutBinding
import com.l0122100.prama.chatapp2.databinding.ReceiveMsgBinding
import com.l0122100.prama.chatapp2.databinding.SendMsgBinding
import com.l0122100.prama.chatapp2.model.Message

class MessageAdapter(
    private val context: Context,
    private var messages: ArrayList<Message>,
    private val senderRoom: String,
    private val receiverRoom: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val ITEM_SENT = 1
    private val ITEM_RECEIVE = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_SENT) {
            val view: View = LayoutInflater.from(context).inflate(R.layout.send_msg, parent, false)
            SentMsgHolder(view)
        } else {
            val view: View = LayoutInflater.from(context).inflate(R.layout.receive_msg, parent, false)
            ReceiveMsgHolder(view)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (FirebaseAuth.getInstance().uid == message.senderId) {
            ITEM_SENT
        } else {
            ITEM_RECEIVE
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is SentMsgHolder) {
            bindSentMessage(holder, message)
        } else if (holder is ReceiveMsgHolder) {
            bindReceivedMessage(holder, message)
        }
    }

    private fun bindSentMessage(holder: SentMsgHolder, message: Message) {
        if (message.message == "photo") {
            holder.binding.image.visibility = View.VISIBLE
            holder.binding.message.visibility = View.GONE
            holder.binding.mLinear.visibility = View.GONE
            Glide.with(context)
                .load(message.imageUrl)
                .placeholder(R.drawable.placeholder)
                .into(holder.binding.image)
        } else {
            holder.binding.message.text = message.message
        }
        holder.itemView.setOnLongClickListener {
            showDeleteDialog(message, holder.binding.root)
            true
        }
    }

    private fun bindReceivedMessage(holder: ReceiveMsgHolder, message: Message) {
        if (message.message == "photo") {
            holder.binding.image.visibility = View.VISIBLE
            holder.binding.message.visibility = View.GONE
            holder.binding.mLinear.visibility = View.GONE
            Glide.with(context)
                .load(message.imageUrl)
                .placeholder(R.drawable.placeholder)
                .into(holder.binding.image)
        } else {
            holder.binding.message.text = message.message
        }
        holder.itemView.setOnLongClickListener {
            showDeleteDialog(message, holder.binding.root)
            true
        }
    }

    private fun showDeleteDialog(message: Message, view: View) {
        val view = LayoutInflater.from(context).inflate(R.layout.delete_layout, null)
        val binding: DeleteLayoutBinding = DeleteLayoutBinding.bind(view)
        val dialog = AlertDialog.Builder(context)
            .setTitle("Delete Message")
            .setView(binding.root)
            .create()

        binding.everyone.setOnClickListener {
            deleteMessageForEveryone(message)
            dialog.dismiss()
        }
        binding.delete.setOnClickListener {
            deleteMessageForUser(message)
            dialog.dismiss()
        }
        binding.cancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun deleteMessageForEveryone(message: Message) {
        message.message = "This message was deleted"
        message.messageId?.let {
            FirebaseDatabase.getInstance().reference.child("chats")
                .child(senderRoom)
                .child("messages")
                .child(it).setValue(message)
            FirebaseDatabase.getInstance().reference.child("chats")
                .child(receiverRoom)
                .child("messages")
                .child(it).setValue(message)
        }
    }

    private fun deleteMessageForUser(message: Message) {
        message.messageId?.let {
            FirebaseDatabase.getInstance().reference.child("chats")
                .child(senderRoom)
                .child("messages")
                .child(it).setValue(null)
        }
    }

    inner class SentMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding: SendMsgBinding = SendMsgBinding.bind(itemView)
    }

    inner class ReceiveMsgHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding: ReceiveMsgBinding = ReceiveMsgBinding.bind(itemView)
        }
}
