package com.lkoskela.headlessbarcodescanner.barcode.luminancesource

import com.google.zxing.LuminanceSource

/**
 * Copyright Lauri Koskela 2017.
 */
internal class PooledLuminanceSource<TDataArray>(
        val luminanceSource: LuminanceSource,
        private val dataArray: TDataArray,
        private val releaseCallback: (TDataArray) -> Unit) : AutoCloseable {

    override fun close() {
        releaseCallback.invoke(dataArray)
    }
}