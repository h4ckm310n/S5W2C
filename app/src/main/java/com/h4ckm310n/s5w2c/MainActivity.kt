package com.h4ckm310n.s5w2c

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.h4ckm310n.s5w2c.ui.theme.S5W2CTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            S5W2CTheme {
                // A surface container using the 'background' color from the theme
                /*Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                }*/
                Column(modifier = Modifier.padding(10.dp)) {
                    val enabled = remember {
                        mutableStateOf(false)
                    }
                    val context = LocalContext.current
                    Row(modifier = Modifier.padding(10.dp)) {
                        Button(onClick = {
                            if (enabled.value)
                                disableService(context)
                            else
                                enableService(context)
                            enabled.value = !enabled.value
                        }) {
                            Text(text = if (enabled.value) "关闭" else "开启")
                        }
                        Spacer(modifier = Modifier.padding(10.dp))
                        Button(onClick = { Logger.clearLogs() }) {
                            Text(text = "清空日志")
                        }
                    }

                    Text(text = "运行日志")
                    Spacer(modifier = Modifier.padding(5.dp))
                    LazyColumn() {
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


