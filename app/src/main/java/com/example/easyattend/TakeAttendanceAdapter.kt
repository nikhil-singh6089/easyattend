package com.example.easyattend

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.easyattend.databinding.StudentAttendanceItemBinding
import com.squareup.picasso.Picasso

class TakeAttendanceAdapter(
    context: FacultyTakeAttendanceActivity,
    var attendanceList: ArrayList<StudentAttendance>)
    : RecyclerView.Adapter<TakeAttendanceAdapter.AttendanceViewHolder>() {

        private var isEnabled = false
    private val selectedList = mutableListOf<Int>()
    inner class AttendanceViewHolder(val adapterBinding : StudentAttendanceItemBinding) : RecyclerView.ViewHolder(adapterBinding.root){}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val binding = StudentAttendanceItemBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return AttendanceViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return attendanceList.size
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {

        holder.adapterBinding.checkedImage.visibility = View.INVISIBLE
        holder.adapterBinding.textViewStudentNameStudentItem.text = attendanceList[position].studentName
        holder.adapterBinding.textViewStudentRollNumber.text = attendanceList[position].userId
        if(attendanceList[position].attendanceStatus){
            holder.adapterBinding.textViewStudentAttendanceStatus.text = "Present"
        }else{
            holder.adapterBinding.textViewStudentAttendanceStatus.text = "Absent"
        }
        val imageUri = Uri.parse(attendanceList[position].profilePictureUrl)
        imageUri?.let {

            Picasso.get().load(it).into(holder.adapterBinding.StudentImageStudentItem)

        }

        holder.adapterBinding.linearLayoutStudentItem.setOnLongClickListener(){

            selectItem(holder,attendanceList[position],position)

            true

        }

        holder.adapterBinding.linearLayoutStudentItem.setOnClickListener(){

            if(selectedList.contains(position)){

                selectedList.removeAt(position)
                holder.adapterBinding.checkedImage.visibility = View.INVISIBLE
                if(selectedList.isEmpty()){
                    isEnabled = false
                }

            }else if(isEnabled){
                selectItem(holder,attendanceList[position],position)
            }

        }

    }
    private fun selectItem(holder : TakeAttendanceAdapter.AttendanceViewHolder, item : StudentAttendance,position: Int){

        selectedList.add(position)
        isEnabled = true
        holder.adapterBinding.checkedImage.visibility = View.VISIBLE

    }

    fun makePresent(){

        for(place in selectedList){

            attendanceList[place].attendanceStatus = true

        }
        selectedList.clear()
        isEnabled = false
        notifyDataSetChanged()

    }
    fun makeAbsent(){

        for(place in selectedList){

            attendanceList[place].attendanceStatus = true

        }
        selectedList.clear()
        isEnabled = false
        notifyDataSetChanged()

    }

}