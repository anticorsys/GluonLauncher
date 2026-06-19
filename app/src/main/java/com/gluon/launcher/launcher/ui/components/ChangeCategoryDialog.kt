package com.gluon.launcher.launcher.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gluon.launcher.core.data.AppModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeCategoryDialog(
    app: AppModel,
    currentCategory: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(currentCategory ?: DEFAULT_CATEGORIES.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Категория: ${app.label}") },
        text = {
            Column {
                Text("Выберите новую категорию для этого приложения:")
                Spacer(Modifier.height(16.dp))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    TextField(
                        readOnly = true,
                        value = selectedCategory,
                        onValueChange = { },
                        label = { Text("Категория") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DEFAULT_CATEGORIES.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    selectedCategory = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selectedCategory) }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}