package de.stefan_oltmann.xmp.internal

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * A single-slot reference whose updates are atomic with respect to concurrent writers,
 * used to publish immutable snapshots of the schema registry state.
 *
 * Readers always observe a fully written value; [compareAndSet] succeeds exactly once
 * per slot transition from the expected value.
 */
@OptIn(ExperimentalAtomicApi::class)
internal class SnapshotRef<T>(initialValue: T) {

    private val ref = AtomicReference(initialValue)

    /**
     * The currently published value.
     */
    val value: T
        get() = ref.load()

    /**
     * Atomically replaces the value if it still equals [expected].
     *
     * @return Returns true if the swap happened, false if another writer was faster.
     */
    fun compareAndSet(expected: T, newValue: T): Boolean =
        ref.compareAndSet(expected, newValue)
}
