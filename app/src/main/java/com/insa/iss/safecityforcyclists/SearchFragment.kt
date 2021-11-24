package com.insa.iss.safecityforcyclists

import android.annotation.SuppressLint
import android.os.Bundle
import android.transition.Fade
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.geojson.FeatureCollection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread


class SearchFragment: Fragment(R.layout.search_fragment) {

    private val searchListAdapter = SearchListAdapter()
    private var searchView: SearchView? = null
    private val viewModel: SearchResultsViewModel by activityViewModels()


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
            exitFragment()
        }
        searchRecyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(recyclerView: RecyclerView, motionEvent: MotionEvent): Boolean {
                return when {
                    motionEvent.action != MotionEvent.ACTION_UP || recyclerView.findChildViewUnder(motionEvent.x, motionEvent.y) != null -> false
                    else -> {
                        exitFragment()
                        true
                    }
                }
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
            override fun onTouchEvent(recyclerView: RecyclerView, motionEvent: MotionEvent) {}
        })

        searchRecyclerView?.adapter = searchListAdapter
        searchListAdapter.dataSet = viewModel.dataSet.value
        searchListAdapter.onItemPressedCallback = { _, position ->
            viewModel.selected.value = position
            exitFragment()
        }

        viewModel.dataSet.observe(viewLifecycleOwner, { list ->
            searchListAdapter.dataSet = list
        })

        searchRecyclerView.layoutManager = LinearLayoutManager(requireActivity())

        searchView = requireActivity().findViewById(R.id.searchView)
        searchView?.apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    println("Submit $query")
                    if (query != null) {
                        getGeoJsonData(query)
                    }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    println("Change $query")
                    return true
                }
            })
        }
    }

    private fun getApiCall(search: String): String {
        return "${requireActivity().getString(R.string.geoapify_autocomplete_url)}?text=${
            URLEncoder.encode(
                search,
                "utf-8"
            )
        }&apiKey=${requireActivity().getString(R.string.geoapify_access_token)}"
    }

    private fun getGeoJsonData(search: String) {
        thread {
            val response = URL(getApiCall(search)).readText()
            val responseGeoJson = FeatureCollection.fromJson(response)
            requireActivity().runOnUiThread {
                showSearchResults(responseGeoJson)
            }
        }
    }

    private fun showSearchResults(results: FeatureCollection) {
        println(results)
        viewModel.selected.value = null
        viewModel.dataSet.value = results.features()
    }


    private fun exitFragment() {
        requireActivity().supportFragmentManager.popBackStack()
        searchView?.clearFocus()
    }

}