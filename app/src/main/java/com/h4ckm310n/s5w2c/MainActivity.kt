package com.h4ckm310n.s5w2c

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.h4ckm310n.s5w2c.ui.theme.S5W2CTheme
import kotlinx.coroutines.launch

val Context.configDataStore by preferencesDataStore(name="s5w2c_configs")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch { ConfigManager.initConfig(configDataStore) }
        setContent {
            S5W2CTheme {
                Column(modifier = Modifier.padding(10.dp)) {
                    val enabled = remember {
                        mutableStateOf(false)
                    }
                    Row(modifier = Modifier.padding(10.dp)) {
                        Button(onClick = {
                            if (enabled.value)
                                disableService(applicationContext)
                            else
                                enableService(applicationContext)
                            enabled.value = !enabled.value
                        }) {
                            Text(text = if (enabled.value) "关闭" else "开启")
                        }
                        Spacer(modifier = Modifier.padding(10.dp))
                        Button(onClick = { Logger.clearLogs() }) {
                            Text(text = "清空日志")
                        }
                        Spacer(modifier = Modifier.padding(10.dp))
                        Button(onClick = {
                            val intent = Intent(baseContext, SettingsActivity::class.java)
                            startActivity(intent)
                        }) {
                            Text(text = "设置")
                        }
                    }

                    Text(text = "运行日志")
                    Spacer(modifier = Modifier.padding(5.dp))
                    LazyColumn {
                        items(Logger.items.size) {
                            LogUIItem(type = Logger.items[it].type, content = Logger.items[it].content)
                        }
                    }
                }
            }
        }
    }

    private fun enableService(context: Context) {
        context.startForegroundService(Intent(context, ProxyService::class.java))
    }

    private fun disableService(context: Context) {
        context.stopService(Intent(context, ProxyService::class.java))
    }
}

@Composable
fun LogUIItem(type: Int, content: String) {
    Row(modifier = Modifier.padding(10.dp)) {
        Text(text = if (type == 1) "Debug" else "Error")
        Spacer(modifier = Modifier.padding(10.dp))
        Text(text = content)
    }
}


