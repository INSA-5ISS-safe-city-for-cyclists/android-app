package com.insa.iss.safecityforcyclists

import android.annotation.SuppressLint
import android.os.Bundle
import android.transition.Fade
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class SearchFragment: Fragment(R.layout.search_fragment) {

    private val searchListAdapter = SearchListAdapter()
    private var searchView: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = Fade()
        exitTransition = Fade()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchRecyclerView = view.findViewById<RecyclerView>(R.id.searchRecyclerView)
        val searchFragmentContainer = view.findViewById<ConstraintLayout>(R.id.searchFragmentContainer)
        searchFragmentContainer?.setOnClickListener {
            onBackgroundPress()
        }
        searchRecyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(recyclerView: RecyclerView, motionEvent: MotionEvent): Boolean {
                return when {
                    motionEvent.action != MotionEvent.ACTION_UP || recyclerView.findChildViewUnder(motionEvent.x, motionEvent.y) != null -> false
                    else -> {
                        onBackgroundPress()
                        true
                    }
                }
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
            override fun onTouchEvent(recyclerView: RecyclerView, motionEvent: MotionEvent) {}
        })




        searchRecyclerView?.adapter = searchListAdapter
        searchRecyclerView.layoutManager = LinearLayoutManager(requireActivity())

        searchView = requireActivity().findViewById(R.id.searchView)
        searchView?.apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    println("Submit $query")
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    println("Change $query")
                    return true
                }
            })
        }
    }

    private fun onBackgroundPress() {
        requireActivity().supportFragmentManager.popBackStack()
        searchView?.clearFocus()
    }

}