package com.romagmir.biketrack.utils

import androidx.lifecycle.MutableLiveData

/**
 * Adds an item to the [ArrayList] and notifies his observers that the underlying data has changed
 * even if the reference is the same.
 *
 * Thanks to [Md. Yamin Mollah](https://stackoverflow.com/a/61835245)
 */
fun <T> MutableLiveData<ArrayList<T>>.add(item: T) {
    val value = this.value ?: ArrayList()
    value.add(item)
    this.value = value
}

/**
 * Appends all of the elements in the specified collection to the end of this list and notifies his
 * observers that the underlying data has changed even if the reference is the same.
 *
 * Thanks to [Md. Yamin Mollah](https://stackoverflow.com/a/61835245)
 *
 * @see java.util.ArrayList.addAll
 *
 */
fun <T> MutableLiveData<ArrayList<T>>.addAll(items: Collection<T>) {
    val value = this.value ?: ArrayList()
    value.addAll(items)
    this.value = value
}

/**
 * Removes an item from the [ArrayList] and notifies his observers that the underlying data has changed
 * even if the reference is the same.
 *
 * Thanks to [Md. Yamin Mollah](https://stackoverflow.com/a/61835245)
 */
fun <T> MutableLiveData<ArrayList<T>>.remove(item: T) {
    val value = this.value ?: ArrayList()
    value.remove(item)
    this.value = value
}

/**
 * Workaround to notify the observers of a [MutableLiveData] that the underlying data has changed
 * even if the reference is the same.
 *
 * Thanks to [Md. Yamin Mollah](https://stackoverflow.com/a/61835245)
 */
fun <T> MutableLiveData<T>.notifyObserver() {
    this.value = this.value
}