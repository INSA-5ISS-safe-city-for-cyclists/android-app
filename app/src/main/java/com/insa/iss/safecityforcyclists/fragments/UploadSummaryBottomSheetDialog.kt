package com.insa.iss.safecityforcyclists.fragments

import android.app.Application
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.RetryPolicy
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.insa.iss.safecityforcyclists.R
import com.insa.iss.safecityforcyclists.reports.DangerReportsViewModel
import com.insa.iss.safecityforcyclists.reports.DangerZonesViewModel
import com.insa.iss.safecityforcyclists.upload.UploadListAdapter
import com.mapbox.geojson.Feature
import org.json.JSONObject
import org.json.JSONTokener
import java.nio.charset.Charset


class UploadSummaryBottomSheetDialog(private val onItemPressedCallback: (bottomSheet: UploadSummaryBottomSheetDialog, item: Feature, position: Int) -> Unit) :
    BottomSheetDialogFragment() {

    private val uploadListAdapter = UploadListAdapter()
    private val dangerReportsViewModel: DangerReportsViewModel by activityViewModels()
    private val dangerZonesViewModel: DangerZonesViewModel by activityViewModels()

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
        dangerReportsViewModel.getFeatures().value?.features()?.let {
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
            uploadReports()
        }

        uploadRecyclerView.adapter = uploadListAdapter
        updateDataset()
        println(uploadListAdapter.dataSet)
        uploadListAdapter.onItemPressedCallback = { item, position ->
            onItemPressedCallback(this, item, position)
        }

        dangerReportsViewModel.getFeatures().observe(viewLifecycleOwner, {
            updateDataset()
        })
        uploadRecyclerView.layoutManager = LinearLayoutManager(requireActivity())
    }

    private fun uploadReports() {
        Toast.makeText(requireActivity(), R.string.uploading_reports, Toast.LENGTH_SHORT).show()

        val tmpActivity = requireActivity()
        val tmpDangerReportsViewModel = dangerReportsViewModel
        val tmpDangerZonesViewModel = dangerZonesViewModel

        val queue = Volley.newRequestQueue(requireActivity().applicationContext)

        // HTTP POST

        val urlPost: String =
            requireActivity().resources.getString(R.string.server_uri) + "reports/geojson"

        val requestBody = dangerReportsViewModel.getLocalFeaturesAsJson()
        Log.d("POST", requestBody)
        // Request a string response from the provided URL.
        val stringRequest = object : StringRequest(
            Method.POST, urlPost,
            { response ->
                println("POST: Response is: $response")

                // Set sync to all reports in the local database
                var ids: List<Int> = listOf()
                val json = JSONTokener(requestBody).nextValue() as JSONObject
                val features = json.getJSONArray("features")
                for (i in 0 until features.length()) {
                    ids = ids + features.getJSONObject(i).getJSONObject("properties").getInt("id")
                }
                Log.d("POST", "ids = $ids")
                tmpDangerZonesViewModel.initData()
                tmpDangerReportsViewModel.syncLocalReportsById(ids)
                dismiss()
            },
            { error ->
                val msg: String = if (error.message == null) {
                    error.toString()
                } else {
                    error.message!!
                }
                Log.d("POST_ERROR", msg)
                Toast.makeText(
                    tmpActivity,
                    tmpActivity.resources.getString(
                        R.string.error_uploading_reports,
                        msg
                    ), Toast.LENGTH_SHORT
                ).show()
                dismiss()
            }) {
            override fun getBody(): ByteArray {
                return requestBody.toByteArray(Charset.defaultCharset())
            }

            override fun getBodyContentType(): String {
                return "text/plain"
            }
        }
        // Add the request to the RequestQueue.
        queue.add(stringRequest)
        dismiss()
    }
}