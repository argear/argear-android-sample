package com.seerslab.argear.sample.ui.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.seerslab.argear.sample.R

class UnderLineView : FrameLayout {

    private lateinit var textView: TextView
    private lateinit var underLine: View
//    private var mViewSelected = false

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(
        context: Context,
        attrs: AttributeSet
    ) : super(context, attrs) {
        init()
        setAttrs(attrs, 0)
    }

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init()
        setAttrs(attrs, defStyleAttr)
    }

    private fun setAttrs(attrs: AttributeSet, defStyleAttr: Int) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.UnderLineView, defStyleAttr, 0)
        val textString = ta.getString(R.styleable.UnderLineView_text)

        setText(textString)
        viewSelected = ta.getBoolean(R.styleable.UnderLineView_select, false)

        ta.recycle()
    }

    private fun init() {
        textView = TextView(context)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
        textView.gravity = Gravity.CENTER
        addView(textView)

        underLine = View(context)
        addView(underLine)

        val layoutParams = underLine.layoutParams as LayoutParams
        layoutParams.width = LayoutParams.MATCH_PARENT
        layoutParams.height = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 2.0f,
            resources.displayMetrics
        ).toInt()
        layoutParams.gravity = Gravity.BOTTOM

        setUnderLineColor(Color.parseColor("#3063E6"))
    }

    fun setText(text: CharSequence?) {
        textView.text = text
    }

    fun setTextColor(color: Int) {
        textView.setTextColor(color)
    }

    fun setUnderLineColor(color: Int) {
        underLine.setBackgroundColor(color)
    }

    var viewSelected: Boolean = false
        set(isViewSelected) {
            field = isViewSelected
            if (field) {
                setTextColor(Color.parseColor("#3063E6"))
                setBottomLineVisibility(View.VISIBLE)

                if (parent != null) {
                    if (tag == "0") {
                        (parent as View).setPadding(width, 0, 0, 0)
                    } else if (tag == "1") {
                        (parent as View).setPadding(0, 0, (width + width * 0.5f).toInt(), 0)
                    }
                }
            } else {
                setTextColor(Color.parseColor("#bdbdbd"))
                setBottomLineVisibility(View.GONE)
            }
        }

    private fun setBottomLineVisibility(visibility: Int) {
        underLine.visibility = visibility
    }
}