package com.insa.iss.safecityforcyclists.upload

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.insa.iss.safecityforcyclists.R
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import java.util.*

class UploadListAdapter: RecyclerView.Adapter<UploadListAdapter.ViewHolder>() {

        var dataSet: List<Feature>? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

        var onItemPressedCallback: ((item: Feature, position: Int) -> Unit)? = null


        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val uploadItemTitle: TextView = view.findViewById(R.id.uploadItemTitle)
            val uploadItemSubtitle: TextView = view.findViewById(R.id.uploadItemSubtitle)
            val uploadItemContainer: ConstraintLayout = view.findViewById(R.id.uploadItemContainer)
            var context: Context = view.context
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.upload_summary_list_item_layout, viewGroup, false)
            return ViewHolder(view)
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            if (dataSet != null) {
                val item = dataSet?.get(position)
                if (item != null) {
                    println("item: $item")
                    val p = (item.geometry() as Point)
                    val prop = item.properties()

                    viewHolder.uploadItemTitle.text = "ERROR: Could not retrieve time"
                    prop?.get("timestamp")?.asLong?.let {
                        val date = Date()
                        date.time = it * 1000
                        viewHolder.uploadItemTitle.text = DateFormat.getLongDateFormat(viewHolder.context).format(date) + ", " + DateFormat.getTimeFormat(viewHolder.context).format(date)
                    }
                    viewHolder.uploadItemSubtitle.text = "[${p.latitude()}, ${p.longitude()}]"
                    viewHolder.uploadItemContainer.setOnClickListener {
                        println("pressed $item")
                        onItemPressedCallback?.invoke(item, position)
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return if (dataSet != null) {
                dataSet!!.size
            } else {
                0
            }
        }
}