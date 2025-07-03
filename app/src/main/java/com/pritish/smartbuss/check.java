//private String getDirectionsUrl(LatLng origin, LatLng dest, String mode) {
//    String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
//    String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
//    String mode_param = "mode=" + mode;
//    String parameters = str_origin + "&" + str_dest + "&" + mode_param + "&key=" + API_KEY;
//    String output = "json";
//    return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
//}
//
//private String downloadUrl(String strUrl) {
//    String data = "";
//    InputStream iStream = null;
//    HttpURLConnection urlConnection = null;
//    try {
//        URL url = new URL(strUrl);
//        urlConnection = (HttpURLConnection) url.openConnection();
//        urlConnection.connect();
//        iStream = urlConnection.getInputStream();
//        BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
//        StringBuilder sb = new StringBuilder();
//        String line;
//        while ((line = br.readLine()) != null) {
//            sb.append(line);
//        }
//        data = sb.toString();
//        br.close();
//    } catch (Exception e) {
//        Log.d(TAG, "Error downloading URL: " + e.toString());
//    } finally {
//        try {
//            if (iStream != null) iStream.close();
//            if (urlConnection != null) urlConnection.disconnect();
//        } catch (IOException e) {
//            Log.e(TAG, "Error closing stream or disconnecting connection: " + e.toString());
//        }
//    }
//    return data;
//}
//
//private List<LatLng> decodePoly(String encoded) {
//    List<LatLng> poly = new ArrayList<>();
//    int index = 0, len = encoded.length();
//    int lat = 0, lng = 0;
//
//    while (index < len) {
//        int b, shift = 0, result = 0;
//        do {
//            b = encoded.charAt(index++) - 63;
//            result |= (b & 0x1f) << shift;
//            shift += 5;
//        } while (b >= 0x20);
//        int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
//        lat += dlat;
//
//        shift = 0;
//        result = 0;
//        do {
//            b = encoded.charAt(index++) - 63;
//            result |= (b & 0x1f) << shift;
//            shift += 5;
//        } while (b >= 0x20);
//        int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
//        lng += dlng;
//
//        LatLng p = new LatLng((((double) lat / 1E5)),
//                (((double) lng / 1E5)));
//        poly.add(p);
//    }
//    return poly;
//}
//
//private List<LatLng> parseDirections(String jsonData) {
//    List<LatLng> routePoints = new ArrayList<>();
//    try {
//        JSONObject jObject = new JSONObject(jsonData);
//        JSONArray jRoutes = jObject.optJSONArray("routes");
//        if (jRoutes != null && jRoutes.length() > 0) {
//            JSONObject jRoute = jRoutes.getJSONObject(0);
//            JSONObject jOverviewPolyline = jRoute.optJSONObject("overview_polyline");
//            if (jOverviewPolyline != null) {
//                String encodedString = jOverviewPolyline.optString("points");
//                if (encodedString != null && !encodedString.isEmpty()) {
//                    routePoints = decodePoly(encodedString);
//                }
//            }
//        }
//    } catch (Exception e) {
//        Log.e(TAG, "Error parsing directions: " + e.toString());
//    }
//    return routePoints;
//}
//
//private int dpToPx(int dp) {
//    return Math.round(dp * getResources().getDisplayMetrics().density);
//}