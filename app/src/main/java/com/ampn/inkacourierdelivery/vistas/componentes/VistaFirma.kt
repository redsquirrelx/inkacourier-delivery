package com.ampn.inkacourierdelivery.vistas.componentes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.createBitmap
import androidx.core.os.BundleCompat
import com.ampn.inkacourierdelivery.vistas.comoBytes

/** Lienzo táctil para capturar la firma del receptor. Conserva los trazos al rotar la pantalla. */
class VistaFirma @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val CLAVE_ESTADO_PADRE = "estadoPadre"
        private const val CLAVE_CANTIDAD = "cantidadTrazos"
        private const val CLAVE_TRAZO = "trazo_"
        private const val PUNTOS_MINIMOS = 5
    }

    private val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 4 * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    /** Cada trazo se guarda como pares x, y consecutivos. */
    private val trazos = mutableListOf<MutableList<Float>>()
    private val ruta = Path()

    val estaVacia: Boolean get() = trazos.sumOf { it.size / 2 } < PUNTOS_MINIMOS

    fun limpiar() {
        trazos.clear()
        ruta.reset()
        invalidate()
    }

    /** Devuelve la firma como PNG, o null si no hay suficientes trazos. */
    fun obtenerImagen(): ByteArray? {
        if (estaVacia || width == 0 || height == 0) return null
        val imagen = createBitmap(width, height)
        val lienzo = Canvas(imagen)
        lienzo.drawColor(Color.WHITE)
        lienzo.drawPath(ruta, pincel)
        return imagen.comoBytes(Bitmap.CompressFormat.PNG)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(ruta, pincel)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                trazos.add(mutableListOf(event.x, event.y))
                ruta.moveTo(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                val trazo = trazos.lastOrNull() ?: return true
                for (i in 0 until event.historySize) {
                    agregarPunto(trazo, event.getHistoricalX(i), event.getHistoricalY(i))
                }
                agregarPunto(trazo, event.x, event.y)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
            }
        }
        invalidate()
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    private fun agregarPunto(trazo: MutableList<Float>, x: Float, y: Float) {
        trazo.add(x)
        trazo.add(y)
        ruta.lineTo(x, y)
    }

    override fun onSaveInstanceState(): Parcelable = Bundle().apply {
        putParcelable(CLAVE_ESTADO_PADRE, super.onSaveInstanceState())
        putInt(CLAVE_CANTIDAD, trazos.size)
        trazos.forEachIndexed { i, trazo -> putFloatArray(CLAVE_TRAZO + i, trazo.toFloatArray()) }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is Bundle) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(BundleCompat.getParcelable(state, CLAVE_ESTADO_PADRE, Parcelable::class.java))
        limpiar()
        repeat(state.getInt(CLAVE_CANTIDAD)) { i ->
            val puntos = state.getFloatArray(CLAVE_TRAZO + i) ?: return@repeat
            trazos.add(puntos.toMutableList())
            ruta.moveTo(puntos[0], puntos[1])
            for (j in 2 until puntos.size step 2) ruta.lineTo(puntos[j], puntos[j + 1])
        }
        invalidate()
    }
}
