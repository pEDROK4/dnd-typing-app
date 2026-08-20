package com.example.dndtyping

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Tela inicial. Como o Android exige que o usuário conceda manualmente:
 *  1. O Serviço de Acessibilidade
 *  2. O Acesso ao Não Perturbe
 * esta Activity apenas guia o usuário até as telas de configuração corretas.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        val btnAccessibility = findViewById<Button>(R.id.btnAccessibility)
        val btnDndPermission = findViewById<Button>(R.id.btnDndPermission)

        btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnDndPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        atualizarStatus()
    }

    private fun atualizarStatus() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val dndOk = notificationManager.isNotificationPolicyAccessGranted

        // Não há forma direta de checar se o AccessibilityService específico
        // está ativo sem consultar Settings.Secure; aqui simplificamos o status.
        tvStatus.text = if (dndOk) {
            "Acesso ao Não Perturbe: concedido ✅\nConfira também se o serviço de acessibilidade está ativo."
        } else {
            "Acesso ao Não Perturbe: pendente ⚠️\nToque nos botões abaixo para configurar."
        }
    }
}
