package com.example.financeapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.financeapp.ui.lock.AppLockGate
import com.example.financeapp.ui.navigation.FinanceNavHost
import com.example.financeapp.ui.navigation.LaunchTarget
import com.example.financeapp.ui.theme.FinanceAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    /** Destino pedido por un widget; se consume una vez que la navegación lo abre. */
    private val launchTarget = MutableStateFlow<LaunchTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Solo en arranque "fresco": al recrear (rotación) el intent ya se procesó.
        if (savedInstanceState == null) launchTarget.value = intent.toLaunchTarget()
        setContent {
            FinanceAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppLockGate(activity = this) {
                        val target by launchTarget.collectAsStateWithLifecycle()
                        FinanceNavHost(
                            launchTarget = target,
                            onLaunchConsumed = { launchTarget.value = null },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.toLaunchTarget()?.let { launchTarget.value = it }
    }

    private fun Intent?.toLaunchTarget(): LaunchTarget? {
        this ?: return null
        if (getStringExtra(EXTRA_OPEN) == OPEN_UPCOMING) return LaunchTarget.Upcoming
        return getStringExtra(EXTRA_TX_TYPE)?.let { LaunchTarget.NewTransaction(it) }
    }

    companion object {
        const val EXTRA_TX_TYPE = "tx_type"
        const val EXTRA_OPEN = "open"
        const val OPEN_UPCOMING = "upcoming"
    }
}
