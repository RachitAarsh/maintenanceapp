package com.aarsh.maintenanceapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aarsh.maintenanceapp.R
import com.aarsh.maintenanceapp.data.Complaint
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class NewComplaintFragment : Fragment() {
    private lateinit var nameInput: TextInputEditText
    private lateinit var designationInput: TextInputEditText
    private lateinit var complaintInput: TextInputEditText
    private lateinit var submitButton: MaterialButton
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_new_complaint, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        nameInput = view.findViewById(R.id.name_input)
        designationInput = view.findViewById(R.id.designation_input)
        complaintInput = view.findViewById(R.id.complaint_input)
        submitButton = view.findViewById(R.id.submit_button)

        submitButton.setOnClickListener {
            submitComplaint()
        }
    }

    private fun submitComplaint() {
        val name = nameInput.text.toString().trim()
        val designation = designationInput.text.toString().trim()
        val complaintText = complaintInput.text.toString().trim()

        if (name.isEmpty() || designation.isEmpty() || complaintText.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val complaint = Complaint(
            name = name,
            designation = designation,
            complaint = complaintText,
            date = Date(),
            status = "Pending"
        )

        db.collection("complaints")
            .add(complaint)
            .addOnSuccessListener {
                Toast.makeText(context, "Complaint submitted successfully", Toast.LENGTH_SHORT).show()
                clearFields()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to submit complaint", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearFields() {
        nameInput.text?.clear()
        designationInput.text?.clear()
        complaintInput.text?.clear()
    }
} 