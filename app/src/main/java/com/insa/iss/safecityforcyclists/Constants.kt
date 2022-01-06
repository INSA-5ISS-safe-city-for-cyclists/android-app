package com.insa.iss.safecityforcyclists

class Constants {
    companion object {
        private const val API_URL = "https://rasp.pikouri.fr/api/"
        const val API_ZONES_ENDPOINT = "${API_URL}zones"
        const val API_REPORTS_ENDPOINT = "${API_URL}reports"
        const val API_CRITERIA_ENDPOINT = "${API_URL}criteria"
    }
}
