package com.insa.iss.safecityforcyclists.search

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.insa.iss.safecityforcyclists.R
import com.mapbox.geojson.Feature

class SearchListAdapter :
    RecyclerView.Adapter<SearchListAdapter.ViewHolder>() {

    var dataSet: List<Feature>? = null
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var onItemPressedCallback: ((item: Feature, position: Int) -> Unit)? = null


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val searchListItemTitle: TextView = view.findViewById(R.id.searchListItemTitle)
        val searchListItemContainer: LinearLayout = view.findViewById(R.id.searchListItemContainer)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.search_list_item, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        if (dataSet != null) {
            val item = dataSet?.get(position)
            if (item != null) {
                viewHolder.searchListItemTitle.text = item.properties()?.get("formatted")?.asString ?: "ERROR"
                viewHolder.searchListItemContainer.setOnClickListener {
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