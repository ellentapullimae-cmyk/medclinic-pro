package com.clinica.core.ui

import java.util.Locale

fun money(value: Double): String = String.format(Locale.US, "%.2f", value)
