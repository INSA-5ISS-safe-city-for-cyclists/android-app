package com.insa.iss.safecityforcyclists.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.insa.iss.safecityforcyclists.R
import com.insa.iss.safecityforcyclists.reports.DangerReportsViewModel
import com.insa.iss.safecityforcyclists.upload.UploadListAdapter
import com.mapbox.geojson.Feature


class UploadSummaryBottomSheetDialog(private val onItemPressedCallback: (item: Feature, position: Int) -> Unit) : BottomSheetDialogFragment() {

    private val uploadListAdapter = UploadListAdapter()
    private val viewModel: DangerReportsViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        // Setup dialog full height
        dialog.setOnShowListener {
            val bottomSheetDialog = it as BottomSheetDialog
            val parentLayout =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            parentLayout?.let { layout ->
                val behaviour = BottomSheetBehavior.from(layout)
                val layoutParams = layout.layoutParams
                layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
                layout.layoutParams = layoutParams
                behaviour.state = BottomSheetBehavior.STATE_EXPANDED
                behaviour.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.upload_summary_bottom_sheet_layout,
            container, false
        )
    }

    private fun updateDataset() {
        viewModel.getLocalFeatures().value?.features()?.let {
            uploadListAdapter.dataSet = it.filter { feature ->
                feature.properties()?.get("sync")?.asBoolean == false
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uploadRecyclerView: RecyclerView = view.findViewById(R.id.uploadRecyclerView)
        val uploadConfirmButton: Button = view.findViewById(R.id.uploadConfirmButton)

        uploadConfirmButton.setOnClickListener {
            println("confirmed upload")
        }

        uploadRecyclerView.adapter = uploadListAdapter
        updateDataset()
        println(uploadListAdapter.dataSet)
        uploadListAdapter.onItemPressedCallback = { item, position ->
            onItemPressedCallback(item, position)
        }

        viewModel.getLocalFeatures().observe(viewLifecycleOwner, { list ->
            updateDataset()
        })
        uploadRecyclerView.layoutManager = LinearLayoutManager(requireActivity())
    }
}