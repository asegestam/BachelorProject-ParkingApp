package com.example.smspark.model.extentionFunctions

import android.view.View

fun View.toggleVisibility() {
    if(this.visibility == View.GONE) this.visibility = View.VISIBLE
    else this.visibility = View.GONE
}
fun View.changeVisibility(visibility: Int) {
    this.visibility = visibility
}