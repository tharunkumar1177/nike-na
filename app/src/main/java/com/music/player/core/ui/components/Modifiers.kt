/*
 * Small shared modifiers.
 */
package com.music.player.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/** Clickable with the button role for accessibility services. */
fun Modifier.clickableButton(onClick: () -> Unit): Modifier = clickable(role = Role.Button, onClick = onClick)
