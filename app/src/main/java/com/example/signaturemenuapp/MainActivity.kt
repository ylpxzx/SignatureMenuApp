package com.example.signaturemenuapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.signaturemenuapp.ui.SignatureMenuOfflineApp
import com.example.signaturemenuapp.ui.theme.SignatureMenuAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SignatureMenuAppTheme {
                SignatureMenuOfflineApp()
            }
        }
    }
}
