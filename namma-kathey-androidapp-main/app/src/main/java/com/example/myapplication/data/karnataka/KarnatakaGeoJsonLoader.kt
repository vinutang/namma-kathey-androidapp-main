package com.example.myapplication.data.karnataka

import android.content.Context
import android.graphics.Path
import android.util.Log
import androidx.compose.ui.geometry.Offset
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

/**
 * Loads [assets/karnataka_districts.geojson] (civictech-India Karnataka district boundaries)
 * and builds normalized (0–1) paths for Canvas drawing.
 *
 * GeoJSON uses legacy spellings; we map them to the canonical names used in the app and in
 * [com.example.myapplication.data.Hero.district].
 */
data class KarnatakaDistrictShape(
    val canonicalEnglish: String,
    /** Outer ring in normalized coordinates (0–1, y down). */
    val outerRing: List<Offset>,
    val pathNormalized: Path,
    val centroid: Offset,
)

object KarnatakaGeoJsonLoader {

    private const val ASSET_NAME = "karnataka_districts.geojson"
    private const val TAG = "KarnatakaGeoJson"

    /**
     * Maps GeoJSON `properties.district` strings to canonical English keys used in the app.
     */
    private fun canonicalNameFromGeoProperty(geoName: String): String = when (geoName.trim()) {
        "Bagalkot" -> "Bagalkote"
        "Belgaum" -> "Belagavi"
        "Bellary" -> "Ballari"
        "Bangalore" -> "Bengaluru Urban"
        "Bangalore Rural" -> "Bengaluru Rural"
        "Bijapur" -> "Vijayapura"
        "Chamrajnagar" -> "Chamarajanagara"
        "Chikmagalur" -> "Chikkamagaluru"
        "Gulbarga" -> "Kalaburagi"
        "Mysore" -> "Mysuru"
        "Shimoga" -> "Shivamogga"
        "Tumkur" -> "Tumakuru"
        else -> geoName.trim()
    }

    fun load(context: Context): List<KarnatakaDistrictShape> {
        return try {
            loadInternal(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load $ASSET_NAME", e)
            emptyList()
        }
    }

    private fun loadInternal(context: Context): List<KarnatakaDistrictShape> {
        val text = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        val features = root.getJSONArray("features")

        val ringsLngLat = ArrayList<Pair<String, List<Pair<Double, Double>>>>(features.length())
        for (i in 0 until features.length()) {
            val f = features.getJSONObject(i)
            val rawName = f.getJSONObject("properties").getString("district")
            val canonical = canonicalNameFromGeoProperty(rawName)
            val geom = f.getJSONObject("geometry")
            val ring = when (geom.getString("type")) {
                "MultiPolygon" -> parseOuterRingFromMultiPolygon(geom.getJSONArray("coordinates"))
                "Polygon" -> parseOuterRingFromPolygon(geom.getJSONArray("coordinates"))
                else -> emptyList()
            }
            if (ring.size >= 3) {
                ringsLngLat.add(canonical to ring)
            }
        }

        var minLon = Double.POSITIVE_INFINITY
        var maxLon = Double.NEGATIVE_INFINITY
        var minLat = Double.POSITIVE_INFINITY
        var maxLat = Double.NEGATIVE_INFINITY
        for ((_, ring) in ringsLngLat) {
            for ((lon, lat) in ring) {
                minLon = min(minLon, lon)
                maxLon = max(maxLon, lon)
                minLat = min(minLat, lat)
                maxLat = max(maxLat, lat)
            }
        }
        val lonSpan = (maxLon - minLon).coerceAtLeast(1e-6)
        val latSpan = (maxLat - minLat).coerceAtLeast(1e-6)

        fun project(lon: Double, lat: Double): Offset {
            val x = ((lon - minLon) / lonSpan).toFloat()
            val y = (1.0 - (lat - minLat) / latSpan).toFloat()
            return Offset(x, y)
        }

        val shapes = ArrayList<KarnatakaDistrictShape>()
        for ((canonical, ring) in ringsLngLat) {
            val projected = ring.map { project(it.first, it.second) }
            val path = buildClosedPath(projected)
            val centroid = centroidOf(projected)
            shapes.add(
                KarnatakaDistrictShape(
                    canonicalEnglish = canonical,
                    outerRing = projected,
                    pathNormalized = path,
                    centroid = centroid,
                ),
            )
        }

        addVijayanagaraPlaceholder(shapes)
        return shapes
    }

    private fun addVijayanagaraPlaceholder(existing: MutableList<KarnatakaDistrictShape>) {
        if (existing.any { it.canonicalEnglish == "Vijayanagara" }) return
        val ballari = existing.firstOrNull { it.canonicalEnglish == "Ballari" } ?: return
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        for (p in ballari.outerRing) {
            minX = min(minX, p.x)
            maxX = max(maxX, p.x)
            minY = min(minY, p.y)
            maxY = max(maxY, p.y)
        }
        val w = maxX - minX
        val h = maxY - minY
        val inset = Offset(minX + w * 0.02f, minY + h * 0.25f)
        val smallW = w * 0.42f
        val smallH = h * 0.45f
        val pent = listOf(
            inset,
            Offset(inset.x + smallW, inset.y),
            Offset(inset.x + smallW, inset.y + smallH * 0.55f),
            Offset(inset.x + smallW * 0.55f, inset.y + smallH),
            Offset(inset.x, inset.y + smallH * 0.65f),
        )
        val path = buildClosedPath(pent)
        val c = centroidOf(pent)
        existing.add(
            KarnatakaDistrictShape(
                canonicalEnglish = "Vijayanagara",
                outerRing = pent,
                pathNormalized = path,
                centroid = c,
            ),
        )
    }

    private fun parseOuterRingFromMultiPolygon(coordinates: JSONArray): List<Pair<Double, Double>> {
        val firstPoly = coordinates.getJSONArray(0)
        val outer = firstPoly.getJSONArray(0)
        return jsonRingToPairs(outer)
    }

    private fun parseOuterRingFromPolygon(coordinates: JSONArray): List<Pair<Double, Double>> {
        val outer = coordinates.getJSONArray(0)
        return jsonRingToPairs(outer)
    }

    private fun jsonRingToPairs(outer: JSONArray): List<Pair<Double, Double>> {
        val out = ArrayList<Pair<Double, Double>>(outer.length())
        for (i in 0 until outer.length()) {
            val pt = outer.getJSONArray(i)
            out.add(pt.getDouble(0) to pt.getDouble(1))
        }
        return out
    }

    private fun buildClosedPath(points: List<Offset>): Path {
        val p = Path()
        if (points.isEmpty()) return p
        val first = points.first()
        p.moveTo(first.x, first.y)
        for (i in 1 until points.size) {
            p.lineTo(points[i].x, points[i].y)
        }
        p.close()
        return p
    }

    private fun centroidOf(points: List<Offset>): Offset {
        if (points.isEmpty()) return Offset.Zero
        var sx = 0f
        var sy = 0f
        for (pt in points) {
            sx += pt.x
            sy += pt.y
        }
        val n = points.size.toFloat()
        return Offset(sx / n, sy / n)
    }

    /** Ray-cast point-in-polygon for a closed ring (no duplicate closing vertex). */
    fun containsNorm(point: Offset, ring: List<Offset>): Boolean {
        if (ring.size < 3) return false
        val x = point.x
        val y = point.y
        var inside = false
        var j = ring.size - 1
        for (i in ring.indices) {
            val pi = ring[i]
            val pj = ring[j]
            val intersect = ((pi.y > y) != (pj.y > y)) &&
                (x < (pj.x - pi.x) * (y - pi.y) / ((pj.y - pi.y).takeIf { it != 0f } ?: 1e-6f) + pi.x)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    /**
     * Returns the topmost district under [pointNorm] when multiple overlap (later list entries win).
     */
    fun districtAt(shapes: List<KarnatakaDistrictShape>, pointNorm: Offset): KarnatakaDistrictShape? {
        var hit: KarnatakaDistrictShape? = null
        for (d in shapes) {
            if (containsNorm(pointNorm, d.outerRing)) {
                hit = d
            }
        }
        return hit
    }
}
