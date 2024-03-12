package com.example.easyattend

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.easyattend.databinding.ViewStudentAttendanceItemBinding
import com.google.firebase.database.core.Context
import com.squareup.picasso.Picasso

class ViewAttendanceAdapter (context: FacultyAttendanceViewActivity, var attendanceList : ArrayList<StudentAttendance>) :
RecyclerView.Adapter<ViewAttendanceAdapter.AttendanceViewHolder>() {

    inner class AttendanceViewHolder(val adapterBinding : ViewStudentAttendanceItemBinding) : RecyclerView.ViewHolder(adapterBinding.root){}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val binding = ViewStudentAttendanceItemBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return AttendanceViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return attendanceList.size
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        holder.adapterBinding.textViewStudentNameViewStudentItem.text = attendanceList[position].studentName
        holder.adapterBinding.textViewStudentRollNumberViewStudentItem.text = attendanceList[position].userId
        if(attendanceList[position].attendanceStatus){
            holder.adapterBinding.textViewStudentAttendanceStatusViewStudentItem.text = "Present"
        }else{
            holder.adapterBinding.textViewStudentAttendanceStatusViewStudentItem.text = "Absent"
        }
        val imageUri = Uri.parse(attendanceList[position].profilePictureUrl)
        imageUri?.let {

            Picasso.get().load(it).into(holder.adapterBinding.StudentImageViewStudentItem)

        }
    }


}