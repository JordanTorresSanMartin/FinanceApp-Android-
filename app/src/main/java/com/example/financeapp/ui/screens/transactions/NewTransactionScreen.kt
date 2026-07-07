package com.example.financeapp.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.financeapp.data.model.TxType
import com.example.financeapp.ui.components.RoundIconButton
import com.example.financeapp.ui.theme.FinanceTheme
import com.example.financeapp.ui.viewmodel.transactions.NewTransactionViewModel
import com.example.financeapp.util.LocalAppHaptics

@Composable
fun NewTransactionScreen(
    initialType: String?,
    onBack: () -> Unit,
    viewModel: NewTransactionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val haptics = LocalAppHaptics.current

    LaunchedEffect(Unit) { viewModel.setInitialType(initialType) }
    LaunchedEffect(state.isSuccess) { if (state.isSuccess) { haptics?.success(); onBack() } }
    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it); viewModel.consumeError() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TransactionTopBar(
                title = "Nueva transacción",
                type = state.type,
                saving = state.isLoading,
                onClose = onBack,
                onSave = viewModel::save,
            )
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                TransactionFormBody(
                    type = state.type,
                    onTypeChange = viewModel::onTypeChange,
                    amount = state.amount,
                    onAmountChange = viewModel::onAmountChange,
                    description = state.description,
                    onDescriptionChange = viewModel::onDescriptionChange,
                    dateText = state.dateText,
                    onDateChange = viewModel::onDateChange,
                    onToday = viewModel::setToday,
                    onYesterday = viewModel::setYesterday,
                    categories = state.categories,
                    selectedCategoryId = state.selectedCategoryId,
                    onCategorySelected = viewModel::onCategorySelected,
                    notes = state.notes,
                    onNotesChange = viewModel::onNotesChange,
                )
            }
        }
    }
}

@Composable
internal fun TransactionTopBar(
    title: String,
    type: String,
    saving: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
    saveLabel: String = "Guardar",
) {
    val finance = FinanceTheme.colors
    val haptics = LocalAppHaptics.current
    val activeColor = if (type == TxType.GASTO) finance.expense else finance.income
    val onActive = if (type == TxType.GASTO) finance.onExpense else finance.onIncome

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(Icons.Filled.Close, onClose, size = 40.dp)
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(activeColor)
                .clickable(enabled = !saving) { haptics?.medium(); onSave() }
                .padding(horizontal = 18.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (saving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = onActive, strokeWidth = 2.dp)
            } else {
                Text(saveLabel, color = onActive, fontWeight = FontWeight.Bold)
            }
        }
    }
}
