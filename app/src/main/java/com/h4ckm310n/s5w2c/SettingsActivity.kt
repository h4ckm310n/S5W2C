package com.h4ckm310n.s5w2c

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import com.h4ckm310n.s5w2c.ui.theme.S5W2CTheme
import kotlinx.coroutines.runBlocking

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            S5W2CTheme {
                val configs = remember { ConfigManager.getConfigs() }
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(modifier = Modifier.padding(10.dp)) {
                        Button(onClick = {
                            runBlocking { ConfigManager.update(configs) }
                            applicationContext.stopService(Intent(applicationContext, ProxyService::class.java))
                        }) { Text("保存") }
                        Spacer(modifier = Modifier.padding(10.dp))
                        Button(onClick = { finish() }) { Text("取消") }
                        Spacer(modifier = Modifier.padding(10.dp))
                        Button(onClick = {
                            configs.clear()
                            configs.addAll(ConfigManager.getDefaultConfigs())
                        }) { Text("恢复默认") }
                    }
                    Spacer(modifier = Modifier.padding(5.dp))
                    LazyColumn {
                        items(configs.size) {
                            SettingInputItem(configs[it])
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingInputItem(item: MutableState<SettingItem>) {
    Row(modifier = Modifier.padding(10.dp)) {
        Text(ConfigManager.getDisplayName(item.value.name))
        Spacer(modifier = Modifier.padding(5.dp))
        OutlinedTextField(
            value = item.value.value as String,
            onValueChange = { newValue ->
                if (newValue.isDigitsOnly())
                    item.value = item.value.copy(value=newValue)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
    }
}