package com.example.smspark.model

import com.mapbox.geojson.Geometry
import com.mapbox.geojson.MultiPolygon
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds

class GeometryUtils {

    /** Returns a point of the given geometry */
    fun getGeometryPoint(geometry: Geometry?): Point {
        return when (geometry) {
            is Polygon -> getPolygonCenter(geometry)
            is MultiPolygon -> getMultiPolygonCenter(geometry)
            else -> geometry as Point
        }
    }

    /** Returns a middle point of a given Geometry, only used for polygons */
    private fun getPolygonCenter(geometry: Geometry): Point {
        val builder = LatLngBounds.Builder()
        val polygon = geometry as Polygon
        polygon.outer()?.coordinates()?.forEach {
            builder.include(LatLng(it.latitude(), it.longitude()))
        }
        val center = builder.build().center
        return Point.fromLngLat(center.longitude, center.latitude)
    }

    /** Returns a middle point of a given Geometry, only used for MultiPolygons */
    private fun getMultiPolygonCenter(geometry: Geometry): Point {
        val builder = LatLngBounds.Builder()
        val multiPolygon = geometry as MultiPolygon
        multiPolygon.coordinates()[0][0].forEach { point ->
            builder.include(LatLng(point.latitude(), point.longitude()))
        }
        val center = builder.build().center
        return Point.fromLngLat(center.longitude, center.latitude)
    }

}