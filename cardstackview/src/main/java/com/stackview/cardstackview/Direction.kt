package com.stackview.cardstackview

import java.util.*

enum class Direction {
    Left, Right, Top, Bottom;

    companion object {
        val HORIZONTAL = listOf(Left, Right)
        val VERTICAL = listOf(Top, Bottom)
        val FREEDOM = listOf(*values())
    }
}