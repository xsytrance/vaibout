package com.xsytrance.vaib.vaib

import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun VaibCard(vaib: Vaib) {
    Card {
        Text(text = vaib.name)
    }
}
