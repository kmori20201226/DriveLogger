package com.kmoriproj.drivelogger.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kmoriproj.drivelogger.R
import com.kmoriproj.drivelogger.databinding.ItemTripBinding
import com.kmoriproj.drivelogger.db.Trip
import java.text.SimpleDateFormat
import java.util.*

class TripsAdapter(val clickListener: (Int,Trip)->Unit) : RecyclerView.Adapter<TripsAdapter.TripsViewHolder>()  {

    companion object {
        val subActionDefault = 0   // Item click
        val subActionUpload = 1    // Upload click
    }

    private val diffCallback = object : DiffUtil.ItemCallback<Trip>() {
        override fun areItemsTheSame(oldItem: Trip, newItem: Trip): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Trip, newItem: Trip): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    // ListDiffer to efficiently deal with changes in the RecyclerView
    val differ = AsyncListDiffer(this, diffCallback)

    class TripsViewHolder(itemView: View, clickAtPosition: (Int)->Unit) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                clickAtPosition(adapterPosition)
            }
        }
    }

    fun submitList(list: List<Trip>) = differ.submitList(list)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripsViewHolder {
        return TripsViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_trip,
                parent,
                false
            ), {
                clickListener(subActionDefault, differ.currentList[it])
            }
        )
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    private lateinit var binding: ItemTripBinding

    override fun onBindViewHolder(holder: TripsViewHolder, position: Int) {
        val trip = differ.currentList[position]
        // set item data
        binding = ItemTripBinding.bind(holder.itemView)
        holder.itemView.apply {
            binding.tvComment.text = trip.caption
            //Glide.with(this).load(run.img).into(ivRunImage)
            val dateFormat = SimpleDateFormat("yy/MM/dd", Locale.getDefault())
            val t = (trip.endTime - trip.startTime) / (60L * 1000L)
            val travelMin = "%dmin".format(t % 60)
            val h = t / 60
            val travelHour =  if (h > 0) "%dhr ".format(h) else ""
            binding.tvDate.text = dateFormat.format(trip.startTime)
            val timeFormat = SimpleDateFormat("hh:mm", Locale.getDefault())
            binding.tvTime.text = travelHour + travelMin
            "%.1fkm".format(trip.distanceFromStart / 1000.0).also {
                binding.tvDistance.text = it
            }
            binding.buttonUpload.setOnClickListener {
                clickListener(subActionUpload, differ.currentList[position])
            }
        }
    }
}