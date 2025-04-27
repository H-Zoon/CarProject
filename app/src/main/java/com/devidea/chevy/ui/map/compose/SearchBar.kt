package com.devidea.chevy.ui.map.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devidea.chevy.ui.map.MapViewModel

@Composable
fun SearchBar(
    modifier: Modifier,
    viewModel: MapViewModel,
    onSearch: (String) -> Unit,
    onSafety: () -> Unit,
    drawBackground: Boolean
) {
    val focusManager = LocalFocusManager.current
    var isTextFieldFocused by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState){
        when(uiState){
            MapViewModel.UiState.IsSearching -> {
                isTextFieldFocused = (true)
            }
            else -> {
                focusManager.clearFocus()
                isTextFieldFocused = (false)
            }
        }
    }


    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(
                when {
                    drawBackground -> Color.White
                    //isTextFieldFocused -> Color.White
                    else -> Color.Transparent
                }
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)
        ) {
            TextField(value = inputText,
                onValueChange = { inputText = it },
                label = { Text("목적지") }, modifier = Modifier
                .weight(1f)
                .padding(10.dp)
                .onFocusChanged { state -> if(state.isFocused) viewModel.setState(MapViewModel.UiState.IsSearching) },
                singleLine = true,
                colors = TextFieldDefaults.colors().copy(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(8.dp),
                keyboardActions = KeyboardActions(onSearch = { onSearch(inputText) }))

            AnimatedVisibility(
                visible = !isTextFieldFocused, enter = fadeIn(animationSpec = tween(durationMillis = 100)) + slideInHorizontally(
                    initialOffsetX = { it / 2 }, animationSpec = tween(durationMillis = 100)
                ), exit = fadeOut(animationSpec = tween(durationMillis = 100)) + slideOutHorizontally(
                    targetOffsetX = { it / 2 }, animationSpec = tween(durationMillis = 100)
                )
            ) {
                Spacer(modifier = Modifier.width(10.dp))

                // 커스텀 버튼 사용
                CustomSearchButton {
                    onSafety()
                }
            }
        }
    }
}

@Composable
fun CustomSearchButton(onClick: () -> Unit) {
    Button(
        onClick = onClick, colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFF722B), contentColor = Color.White
        ), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(0.dp), // 기본 패딩 제거,
        modifier = Modifier.size(58.dp) // 필요에 따라 크기 조정
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Directions, // 원하는 아이콘으로 변경
                contentDescription = "SafeDrive", modifier = Modifier.size(30.dp)
            )
            Text(
                text = "안전운전", fontSize = 10.sp
            )
        }
    }
}
