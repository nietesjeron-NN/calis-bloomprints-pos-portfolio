package com.calisbloomprints.pos.printer

sealed interface PrinterResult<out T> {
    data class Success<T>(val value: T) : PrinterResult<T>
    data object PermissionRequired : PrinterResult<Nothing>
    data class Error(val message: String, val throwable: Throwable? = null) : PrinterResult<Nothing>
}
