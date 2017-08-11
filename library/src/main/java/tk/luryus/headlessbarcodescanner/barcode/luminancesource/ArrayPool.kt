package tk.luryus.headlessbarcodescanner.barcode.luminancesource

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Copyright Lauri Koskela 2017.
 */
class ArrayPool<TArray>(private val creator: (Int) -> TArray) {

    private val pool: MutableMap<Int, ConcurrentLinkedQueue<TArray>> = mutableMapOf()

    fun get(size: Int): TArray {
        val fromPool = pool[size]?.poll()
        if (fromPool != null) {
            return fromPool
        }

        return creator.invoke(size)
    }

    fun put(size: Int, arr: TArray) {
        val queue = pool.getOrPut(size, { ConcurrentLinkedQueue() })
        queue.add(arr)
    }
}