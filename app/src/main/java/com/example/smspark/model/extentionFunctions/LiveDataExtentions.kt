package com.example.smspark.model.extentionFunctions

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

/** Uses to observe LiveData once then remove the observer*/
fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(t: T?) {
            observer.onChanged(t)
            removeObserver(this)
        }
    })
}
/** Changes the LiveData value of given value
 * instead of LiveData.variable.changeValue( newValue you can do LiveData.value.changeValue(newValue)
 *  */
fun <T> MutableLiveData<T>.changeValue(newValue: T) {
    this.value = newValue
}

fun MutableLiveData<Boolean>.toggleBoolean() {
    this.value?.let {
        this.value = !it
    }
}