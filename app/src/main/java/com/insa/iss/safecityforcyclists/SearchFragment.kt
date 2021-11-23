package com.insa.iss.safecityforcyclists

import android.os.Bundle
import android.transition.Fade
import android.view.View
import androidx.fragment.app.Fragment

class SearchFragment: Fragment(R.layout.search_fragment) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = Fade()
        exitTransition = Fade()

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        println("search fragment created")
    }

}