package tk.luryus.headlessbarcodescanner;

import tk.luryus.headlessbarcodescanner.barcode.BarcodeResult;

/**
 * Copyright Lauri Koskela 2017.
 */

public interface OnBarcodeReadListener {
    void onBarcodeRead(BarcodeResult result);
}
