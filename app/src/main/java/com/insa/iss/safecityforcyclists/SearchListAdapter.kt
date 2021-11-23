package com.insa.iss.safecityforcyclists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SearchListAdapter :
    RecyclerView.Adapter<SearchListAdapter.ViewHolder>() {

    private var dataSet = arrayOf(1, 2, 3)

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
        viewHolder.searchListItemTitle.text = dataSet[position].toString()
        viewHolder.searchListItemContainer.setOnClickListener {
            println("pressed" + dataSet[position].toString())
        }
    }

    override fun getItemCount() = dataSet.size
}