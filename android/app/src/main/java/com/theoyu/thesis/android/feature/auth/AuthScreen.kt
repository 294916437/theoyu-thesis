package com.theoyu.thesis.android.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.theoyu.thesis.android.R
import com.theoyu.thesis.android.ui.theme.BlueSkyTheme

@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onModeSelected: (AuthMode) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onCodeChanged: (String) -> Unit,
    onAgreeTermsChanged: (Boolean) -> Unit,
    onSendCode: () -> Unit,
    onSubmit: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = uiState.message
        ?: uiState.messageResId?.let { stringResource(it) }
    val phoneError = uiState.phoneErrorResId?.let { stringResource(it) }
    val codeError = uiState.codeErrorResId?.let { stringResource(it) }

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(
                        if (uiState.mode == AuthMode.Login) {
                            R.string.auth_title_login
                        } else {
                            R.string.auth_title_register
                        },
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                    text = stringResource(R.string.auth_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        AuthModeTabs(
                            selectedMode = uiState.mode,
                            onModeSelected = onModeSelected,
                        )

                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = uiState.phone,
                            onValueChange = onPhoneChanged,
                            label = { Text(stringResource(R.string.auth_phone_label)) },
                            placeholder = { Text(stringResource(R.string.auth_phone_placeholder)) },
                            isError = phoneError != null,
                            supportingText = phoneError?.let { { Text(it) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.weight(1f),
                                value = uiState.code,
                                onValueChange = onCodeChanged,
                                label = { Text(stringResource(R.string.auth_code_label)) },
                                placeholder = { Text(stringResource(R.string.auth_code_placeholder)) },
                                isError = codeError != null,
                                supportingText = codeError?.let { { Text(it) } },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                            TextButton(
                                enabled = uiState.canSendCode,
                                onClick = onSendCode,
                            ) {
                                Text(
                                    text = if (uiState.countdownSeconds > 0) {
                                        "${uiState.countdownSeconds}s"
                                    } else {
                                        stringResource(R.string.auth_send_code)
                                    },
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                        Row {
                            Checkbox(
                                checked = uiState.agreeTerms,
                                onCheckedChange = onAgreeTermsChanged,
                            )
                            Text(
                                modifier = Modifier.padding(top = 12.dp),
                                text = stringResource(R.string.auth_terms),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = uiState.canSubmit,
                            onClick = onSubmit,
                        ) {
                            Text(
                                text = stringResource(
                                    if (uiState.mode == AuthMode.Login) {
                                        R.string.auth_login
                                    } else {
                                        R.string.auth_register
                                    },
                                ),
                            )
                        }

                        if (uiState.loading || uiState.sendingCode) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.auth_new_user_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthModeTabs(
    selectedMode: AuthMode,
    onModeSelected: (AuthMode) -> Unit,
) {
    TabRow(selectedTabIndex = selectedMode.ordinal) {
        Tab(
            selected = selectedMode == AuthMode.Login,
            onClick = { onModeSelected(AuthMode.Login) },
            text = { Text(stringResource(R.string.auth_login)) },
        )
        Tab(
            selected = selectedMode == AuthMode.Register,
            onClick = { onModeSelected(AuthMode.Register) },
            text = { Text(stringResource(R.string.auth_register)) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthScreenPreview() {
    BlueSkyTheme {
        AuthScreen(
            uiState = AuthUiState(),
            onModeSelected = {},
            onPhoneChanged = {},
            onCodeChanged = {},
            onAgreeTermsChanged = {},
            onSendCode = {},
            onSubmit = {},
            onMessageShown = {},
        )
    }
}
