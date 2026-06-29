package com.example.financeapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.financeapp.ui.navigation.FinanceNavHost
import com.example.financeapp.ui.theme.FinanceAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Tipo inicial cuando se abre desde el widget QuickAdd (?tx_type=gasto|ingreso).
        val initialType = intent?.getStringExtra(EXTRA_TX_TYPE)
        setContent {
            FinanceAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    FinanceNavHost(initialNewTxType = initialType)
                }
            }
        }
    }

    companion object {
        const val EXTRA_TX_TYPE = "tx_type"
    }
}
