// SPDX-License-Identifier: AGPL-3.0-only
package io.element.android.appnav.antintwrk

import android.content.Context
import android.view.MotionEvent
import android.webkit.WebView
import kotlin.math.abs

/** One deliberate downward drag at the top refreshes a GET; editors and nested scrollers keep their gestures. */
internal class AntintwrkRefreshWebView(context: Context) : WebView(context) {
    var onViewportChanged: () -> Unit = {}
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (oldh > 0 && h != oldh) post { onViewportChanged() }
    }
    var canRefresh: () -> Boolean = { false }
    var onRefresh: () -> Unit = {}
    private var gesture = 0L
    private var top = false
    private var armed = false
    private var startX = 0f
    private var startY = 0f
    private val threshold get() = 96 * resources.displayMetrics.density

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val sequence = ++gesture
                top = false
                armed = false
                startX = event.x
                startY = event.y
                if (canRefresh() && !canScrollVertically(-1)) {
                    val x = event.x / resources.displayMetrics.density
                    val y = event.y / resources.displayMetrics.density
                    evaluateJavascript("""
                        (() => {
                          if (window.scrollY > 1) return false;
                          let n = document.elementFromPoint($x, $y);
                          while (n && n !== document.documentElement) {
                            if (n.isContentEditable || /^(INPUT|TEXTAREA|SELECT|VIDEO)$/.test(n.tagName) || n.closest('dialog[open]')) return false;
                            const s = getComputedStyle(n);
                            if (n.scrollTop > 1 || (/auto|scroll/.test(s.overflowY) && n.scrollHeight > n.clientHeight + 1)) return false;
                            n = n.parentElement;
                          }
                          return true;
                        })()
                    """.trimIndent()) { result -> if (gesture == sequence) top = result == "true" }
                }
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_CANCEL -> {
                gesture++
                top = false
                armed = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (top && canRefresh() && !canScrollVertically(-1) &&
                    antintwrkRefreshDrag(event.x - startX, event.y - startY, threshold)) {
                    if (!armed) {
                        val cancel = MotionEvent.obtain(event)
                        cancel.action = MotionEvent.ACTION_CANCEL
                        super.dispatchTouchEvent(cancel)
                        cancel.recycle()
                        armed = true
                    }
                    return true
                }
                if (armed) return true
            }
            MotionEvent.ACTION_UP -> {
                gesture++
                if (armed) {
                    armed = false
                    if (top && canRefresh() && !canScrollVertically(-1) &&
                        antintwrkRefreshDrag(event.x - startX, event.y - startY, threshold)) {
                            onRefresh()
                        }
                    top = false
                    return true
                }
                top = false
            }
        }
        return super.dispatchTouchEvent(event)
    }
}

internal fun antintwrkRefreshDrag(dx: Float, dy: Float, threshold: Float): Boolean = dy >= threshold && dy > abs(dx) * 1.5f
