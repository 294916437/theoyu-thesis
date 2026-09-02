package com.theoyu.thesis.android.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VideoChat
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.theoyu.thesis.android.R
import com.theoyu.thesis.android.ui.theme.BlueSkyTheme

@Composable
fun AuthScreen(
    uiState: AuthUiState,
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
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.VideoChat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxSize(),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.auth_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    modifier = Modifier.padding(top = 6.dp, bottom = 28.dp),
                    text = stringResource(R.string.auth_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = uiState.phone,
                            onValueChange = onPhoneChanged,
                            label = { Text(stringResource(R.string.auth_phone_label)) },
                            placeholder = { Text(stringResource(R.string.auth_phone_placeholder)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            isError = phoneError != null,
                            supportingText = phoneError?.let { { Text(it) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                modifier = Modifier.weight(1f),
                                value = uiState.code,
                                onValueChange = onCodeChanged,
                                label = { Text(stringResource(R.string.auth_code_label)) },
                                placeholder = { Text(stringResource(R.string.auth_code_placeholder)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                isError = codeError != null,
                                supportingText = codeError?.let { { Text(it) } },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done,
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { if (uiState.canSubmit) onSubmit() },
                                ),
                                shape = RoundedCornerShape(12.dp),
                            )
                            FilledTonalButton(
                                enabled = uiState.canSendCode,
                                onClick = onSendCode,
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(
                                    text = if (uiState.countdownSeconds > 0) {
                                        "${uiState.countdownSeconds}s"
                                    } else {
                                        stringResource(R.string.auth_send_code)
                                    },
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = uiState.agreeTerms,
                                onCheckedChange = onAgreeTermsChanged,
                            )
                            Text(
                                text = stringResource(R.string.auth_terms),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = uiState.canSubmit,
                            shape = RoundedCornerShape(12.dp),
                            onClick = onSubmit,
                        ) {
                            Text(
                                text = stringResource(R.string.auth_login),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }

                        if (uiState.loading || uiState.sendingCode) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                            )
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

@Preview(showBackground = true)
@Composable
private fun AuthScreenPreview() {
    BlueSkyTheme {
        AuthScreen(
            uiState = AuthUiState(),
            onPhoneChanged = {},
            onCodeChanged = {},
            onAgreeTermsChanged = {},
            onSendCode = {},
            onSubmit = {},
            onMessageShown = {},
        )
    }
}
