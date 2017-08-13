package com.lkoskela.headlessbarcodescanner;

import com.lkoskela.headlessbarcodescanner.barcode.BarcodeResult;

/**
 * Interface definition for a callback to be invoked when a barcode is read.
 */
public interface OnBarcodeReadListener {
    /**
     * Called when a barcode is read.
     * @param result Information about the read barcode.
     */
    void onBarcodeRead(BarcodeResult result);
}
