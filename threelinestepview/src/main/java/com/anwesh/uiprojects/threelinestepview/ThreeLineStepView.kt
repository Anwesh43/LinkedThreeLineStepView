package com.anwesh.uiprojects.threelinestepview

/**
 * Created by anweshmishra on 06/10/18.
 */


import android.app.Activity
import android.view.View
import android.view.MotionEvent
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.content.Context
import android.util.Log

val nodes : Int = 5

val k : Float = 3f

fun Canvas.drawTLSNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = w / (nodes + 1)
    val size : Float = gap/3
    val osc : Float = 1f / k
    paint.strokeWidth = Math.min(w, h) / 60
    paint.strokeCap = Paint.Cap.ROUND
    paint.color = Color.parseColor("#01579B")
    save()
    translate(gap + i * gap - size, h/2 - size/2)
    for (j in 0..(k.toInt() - 1)) {
        val sc : Float = Math.min(osc, Math.max(0f, scale - j * osc)) * k
        drawLine(0f, j * size/2, 2 * size * sc, j * size/2, paint)
    }
    restore()
}

class ThreeLineStepView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var prevScale : Float = 0f, var dir : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += (0.1f/3f) * dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class TLSNode(var i : Int, val state : State = State()) {
        private var next : TLSNode? = null
        private var prev : TLSNode? = null

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = TLSNode(i + 1)
                next?.prev = this
            }
        }

        init {
            addNeighbor()
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawTLSNode(i, state.scale, paint)
            prev?.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : TLSNode {
            var curr : TLSNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class ThreeLineStep(var i : Int) {
        private var curr : TLSNode = TLSNode(0)
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            curr.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
                Log.d("curr ->","${curr.i}")
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : ThreeLineStepView) {

        private val tls : ThreeLineStep = ThreeLineStep(0)
        private val animator : Animator = Animator(view)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(Color.parseColor("#BDBDBD"))
            tls.draw(canvas, paint)
            animator.animate {
                tls.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            tls.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : ThreeLineStepView {
            val view : ThreeLineStepView = ThreeLineStepView(activity)
            activity.setContentView(view)
            return view
        }
    }
}