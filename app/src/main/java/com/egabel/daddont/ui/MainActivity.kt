package com.egabel.daddont.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.egabel.daddont.ui.navigation.DadDontNavGraph
import com.egabel.daddont.ui.theme.DadDontTheme
import com.egabel.daddont.worker.ClassificationWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ClassificationWorker.enqueue(applicationContext)

        setContent {
            DadDontTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    DadDontNavGraph(navController = navController)
                }
            }
        }
    }
}
