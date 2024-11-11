package com.devidea.chevy.ui.screen.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.devidea.chevy.ui.theme.DarkShadow
import com.devidea.chevy.ui.theme.LightShadow
import com.devidea.chevy.ui.theme.MainBackground

@Composable
fun SearchBar(
    modifier: Modifier,
    searchText: TextFieldValue,
    onSearchTextChange: (TextFieldValue) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onSearch: () -> Unit,
    isSearchResult: Boolean // 새로운 파라미터 추가
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier.background(
            when {
                isSearchResult -> Color.White // uiState가 SearchResult일 때 흰색 배경
                isFocused -> Color.White // 포커스가 있을 때도 흰색 배경
                else -> Color.Transparent // 그 외에는 투명
            }
        ).animateContentSize() // 크기 변화 애니메이션
    ) {
        TextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            label = { Text("검색어 입력") },
            modifier = modifier
                .padding(16.dp)
                .fillMaxWidth()
                .onFocusChanged { state ->
                    onFocusChanged(state.isFocused)
                    isFocused = state.isFocused
                },
            singleLine = true,
            colors = TextFieldDefaults.colors().copy(
                focusedContainerColor  = MainBackground,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Search
            ),
            shape = RoundedCornerShape(8.dp),
            keyboardActions = KeyboardActions(
                onSearch = { onSearch() }
            )
        )
    }
}