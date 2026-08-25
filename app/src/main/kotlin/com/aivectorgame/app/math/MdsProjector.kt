package com.aivectorgame.app.math

import kotlin.math.max
import kotlin.math.sqrt

object MdsProjector {
    data class Point3(val x: Float, val y: Float, val z: Float)

    fun cosine(a: FloatArray, b: FloatArray): Float {
        val n = minOf(a.size, b.size)
        if (n == 0) return 0f
        var dot = 0.0
        var aa = 0.0
        var bb = 0.0
        for (i in 0 until n) {
            val x = a[i].toDouble()
            val y = b[i].toDouble()
            dot += x * y
            aa += x * x
            bb += y * y
        }
        val denom = sqrt(aa) * sqrt(bb)
        return if (denom <= 1e-12) 0f else (dot / denom).toFloat().coerceIn(-1f, 1f)
    }

    fun project(vectors: List<FloatArray>): List<Point3> {
        val n = vectors.size
        if (n == 0) return emptyList()
        if (n == 1) return listOf(Point3(0f, 0f, 0f))

        val d2 = Array(n) { DoubleArray(n) }
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val c = cosine(vectors[i], vectors[j]).toDouble()
                val squaredEuclideanOnUnitSphere = max(0.0, 2.0 - 2.0 * c)
                d2[i][j] = squaredEuclideanOnUnitSphere
                d2[j][i] = squaredEuclideanOnUnitSphere
            }
        }

        val rowMean = DoubleArray(n) { i -> d2[i].average() }
        val totalMean = rowMean.average()
        val gram = Array(n) { i ->
            DoubleArray(n) { j ->
                -0.5 * (d2[i][j] - rowMean[i] - rowMean[j] + totalMean)
            }
        }

        val working = Array(n) { i -> gram[i].copyOf() }
        val components = mutableListOf<Pair<Double, DoubleArray>>()
        repeat(minOf(3, n)) { component ->
            var v = DoubleArray(n) { i -> ((i + 1) * (component + 2)).toDouble() }
            normalize(v)
            repeat(96) {
                val w = multiply(working, v)
                val norm = length(w)
                if (norm > 1e-12) {
                    for (i in w.indices) w[i] /= norm
                    v = w
                }
            }
            val av = multiply(working, v)
            val eigen = dot(v, av)
            components += eigen to v.copyOf()
            for (i in 0 until n) {
                for (j in 0 until n) {
                    working[i][j] -= eigen * v[i] * v[j]
                }
            }
        }

        val raw = Array(n) { DoubleArray(3) }
        for (axis in components.indices) {
            val (eigen, vector) = components[axis]
            val scale = sqrt(max(0.0, eigen))
            for (i in 0 until n) raw[i][axis] = vector[i] * scale
        }

        var maxAbs = 1e-9
        for (row in raw) for (value in row) maxAbs = max(maxAbs, kotlin.math.abs(value))
        val s = 1.25 / maxAbs
        return raw.map { Point3((it[0] * s).toFloat(), (it[1] * s).toFloat(), (it[2] * s).toFloat()) }
    }

    fun demoVectors(scores: List<Float>): List<FloatArray> {
        val target = floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f)
        val result = mutableListOf(target)
        scores.forEachIndexed { i, score ->
            val s = score.coerceIn(-0.99f, 0.99f)
            val radial = sqrt((1f - s * s).coerceAtLeast(0f))
            val angle = (i + 1) * 1.37
            result += floatArrayOf(
                s,
                (radial * kotlin.math.cos(angle)).toFloat(),
                (radial * kotlin.math.sin(angle)).toFloat(),
                (radial * kotlin.math.cos(angle * 0.47)).toFloat(),
                (radial * kotlin.math.sin(angle * 0.71)).toFloat(),
                (radial * 0.2f),
            )
        }
        return result
    }

    private fun multiply(a: Array<DoubleArray>, v: DoubleArray): DoubleArray =
        DoubleArray(v.size) { i -> a[i].indices.sumOf { j -> a[i][j] * v[j] } }

    private fun dot(a: DoubleArray, b: DoubleArray): Double = a.indices.sumOf { a[it] * b[it] }
    private fun length(v: DoubleArray): Double = sqrt(dot(v, v))
    private fun normalize(v: DoubleArray) {
        val len = length(v).coerceAtLeast(1e-12)
        for (i in v.indices) v[i] /= len
    }
}
