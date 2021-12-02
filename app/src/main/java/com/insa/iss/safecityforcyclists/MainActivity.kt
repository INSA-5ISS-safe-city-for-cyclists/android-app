package com.insa.iss.safecityforcyclists

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.add
import androidx.fragment.app.commit
import com.insa.iss.safecityforcyclists.fragments.MapFragment
import com.insa.iss.safecityforcyclists.fragments.SearchFragment


class MainActivity : AppCompatActivity() {
    companion object {
        const val MARKER_ICON = "MARKER_ICON"
        const val LOCAL_MARKER_ICON = "LOCAL_MARKER_ICON"
        const val LOCAL_MARKER_UNSYNC_ICON = "LOCAL_MARKER_UNSYNC_ICON"
        const val WARNING_ICON = "WARNING_ICON"
        const val WAYPOINT_ICON = "WAYPOINT_ICON"
        const val DESTINATION_ICON = "DESTINATION_ICON"
    }

    private var mapFragment: MapFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        mapFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view) as MapFragment

        findViewById<SearchView>(R.id.searchView).apply {
            setIconifiedByDefault(false)
            queryHint = getString(R.string.search_hint)
            setOnQueryTextFocusChangeListener { _, focused ->
                if (focused) {
                    supportFragmentManager.commit {
                        setReorderingAllowed(true)
                        add<SearchFragment>(R.id.fragment_container_view)
                        addToBackStack("search")
                    }

                }
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mapFragment?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

}