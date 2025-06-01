package com.icanhear.icanhear.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.icanhear.icanhear.R


class InputView : LinearLayout {

    private var titleView: AppCompatTextView? = null
    var contentEditText: AppCompatEditText? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {

        inflate(context, R.layout.input_view, this)

        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        titleView = findViewById(R.id.title_view)
        contentEditText = findViewById(R.id.content_view)
    }

    fun setTitle(title: String?) {
        titleView?.text = title
    }
}