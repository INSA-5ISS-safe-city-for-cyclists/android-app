package com.insa.iss.safecityforcyclists.fragments

import android.util.Log
import android.widget.Toast
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.insa.iss.safecityforcyclists.Constants
import com.insa.iss.safecityforcyclists.R
import com.mapbox.geojson.Feature
import org.json.JSONObject
import org.json.JSONTokener
import java.nio.charset.Charset


class UploadSummaryBottomSheetDialog(onItemPressedCallback: (bottomSheet: FeatureListBottomSheetDialog, item: Feature, position: Int) -> Unit) :
    FeatureListBottomSheetDialog(onItemPressedCallback) {

    override fun getTitle(): String {
        return "Upload Summary"
    }

    override fun onActionButtonClicked() {
        Toast.makeText(requireActivity(), R.string.uploading_reports, Toast.LENGTH_SHORT).show()

        val tmpActivity = requireActivity()
        val tmpDangerReportsViewModel = dangerReportsViewModel
        val tmpDangerZonesViewModel = dangerZonesViewModel

        val queue = Volley.newRequestQueue(requireActivity().applicationContext)

        // HTTP POST
        val urlPost: String = Constants.API_REPORTS_ENDPOINT

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
                return "application/json"
            }
        }
        // Add the request to the RequestQueue.
        queue.add(stringRequest)
        dismiss()
    }

    override fun getActionButtonText(): String {
        return "Confirm Upload"
    }

    override fun updateDataset() {
        dangerReportsViewModel.getFeatures().value?.features()?.let {
            uploadListAdapter.dataSet = it.filter { feature ->
                feature.properties()?.get("sync")?.asBoolean == false
            }
        }
    }
}