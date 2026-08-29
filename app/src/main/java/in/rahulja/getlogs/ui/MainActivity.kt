package `in`.rahulja.getlogs.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import `in`.rahulja.getlogs.data.LogRepository
import `in`.rahulja.getlogs.ui.components.LogListScreen
import `in`.rahulja.getlogs.ui.theme.AndroidLogsTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(LogRepository.getInstance(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidLogsTheme {
                LogListScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshServiceStatus(this)
    }
}
