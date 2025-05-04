package com.aarsh.maintenanceapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aarsh.maintenanceapp.R
import com.aarsh.maintenanceapp.data.Complaint
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class StatusFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private val db = FirebaseFirestore.getInstance()
    private val complaints = mutableListOf<Complaint>()
    private lateinit var adapter: ComplaintsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        recyclerView = view.findViewById(R.id.complaints_recycler_view)
        emptyView = view.findViewById(R.id.empty_view)
        
        adapter = ComplaintsAdapter(complaints)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        
        loadComplaints()
    }

    private fun loadComplaints() {
        db.collection("complaints")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                complaints.clear()
                snapshot?.documents?.forEach { document ->
                    val complaint = document.toObject(Complaint::class.java)
                    complaint?.let { complaints.add(it) }
                }
                
                adapter.notifyDataSetChanged()
                emptyView.visibility = if (complaints.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private inner class ComplaintsAdapter(private val complaints: List<Complaint>) :
        RecyclerView.Adapter<ComplaintsAdapter.ComplaintViewHolder>() {

        inner class ComplaintViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nameText = itemView.findViewById<android.widget.TextView>(R.id.name_text)
            val designationText = itemView.findViewById<android.widget.TextView>(R.id.designation_text)
            val complaintText = itemView.findViewById<android.widget.TextView>(R.id.complaint_text)
            val dateText = itemView.findViewById<android.widget.TextView>(R.id.date_text)
            val statusText = itemView.findViewById<android.widget.TextView>(R.id.status_text)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComplaintViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_complaint, parent, false)
            return ComplaintViewHolder(view)
        }

        override fun onBindViewHolder(holder: ComplaintViewHolder, position: Int) {
            val complaint = complaints[position]
            holder.nameText.text = complaint.name
            holder.designationText.text = complaint.designation
            holder.complaintText.text = complaint.complaint
            holder.dateText.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(complaint.date)
            holder.statusText.text = complaint.status
        }

        override fun getItemCount() = complaints.size
    }
} 