package com.example.dndtyping

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Serviço de acessibilidade que:
 * 1. Detecta quando o usuário está digitando em um campo de texto
 *    localizado nos 35% superiores da tela.
 * 2. Ativa o modo Não Perturbe (DND) enquanto isso acontece.
 * 3. Desativa o DND automaticamente após um período sem digitação
 *    ou ao trocar de campo/app.
 */
class DndAccessibilityService : AccessibilityService() {

    private lateinit var notificationManager: NotificationManager
    private val handler = Handler(Looper.getMainLooper())

    // Referência para poder ser cancelada ao chegar novo evento de digitação
    private var stopTypingRunnable: Runnable? = null

    // Quanto tempo (ms) sem digitar até considerar que o usuário parou
    private val INACTIVITY_TIMEOUT_MS = 2500L

    // Fração da tela considerada "área superior" (35%)
    private val TOP_SCREEN_FRACTION = 0.35f

    private var dndAtivadoPeloApp = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Log.d(TAG, "Serviço de acessibilidade conectado")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                val source = event.source ?: rootInActiveWindow ?: return
                if (isNodeInTopArea(source)) {
                    onUserIsTyping()
                } else {
                    // Campo focado está fora da área de interesse
                    onUserStoppedTyping()
                }
            }

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // Trocou de app/tela: por segurança, desativa o DND
                onUserStoppedTyping()
            }
        }
    }

    /**
     * Verifica se o nó (campo de texto) focado está dentro dos 35%
     * superiores da tela, usando as coordenadas absolutas na tela.
     */
    private fun isNodeInTopArea(node: AccessibilityNodeInfo): Boolean {
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)

        val screenHeight = getScreenHeight()
        if (screenHeight <= 0) return false

        val limiteSuperior = screenHeight * TOP_SCREEN_FRACTION
        // Considera "dentro da área" se o topo do campo estiver acima do limite
        return rect.top <= limiteSuperior
    }

    private fun getScreenHeight(): Int {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(metrics)
        return metrics.heightPixels
    }

    /**
     * Chamado a cada evento de digitação válido dentro da área de interesse.
     * Ativa o DND (se ainda não estiver ativo) e reinicia o timer de inatividade.
     */
    private fun onUserIsTyping() {
        ativarNaoPerturbe()

        // Cancela o timer anterior e cria um novo
        stopTypingRunnable?.let { handler.removeCallbacks(it) }
        stopTypingRunnable = Runnable { onUserStoppedTyping() }
        handler.postDelayed(stopTypingRunnable!!, INACTIVITY_TIMEOUT_MS)
    }

    /**
     * Chamado quando o usuário parou de digitar, saiu do campo,
     * ou trocou de app. Desativa o DND caso tenha sido ativado por este app.
     */
    private fun onUserStoppedTyping() {
        stopTypingRunnable?.let { handler.removeCallbacks(it) }
        desativarNaoPerturbe()
    }

    private fun ativarNaoPerturbe() {
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            Log.w(TAG, "Permissão de acesso ao DND não concedida")
            return
        }
        if (!dndAtivadoPeloApp) {
            notificationManager.setInterruptionFilter(
                NotificationManager.INTERRUPTION_FILTER_PRIORITY
            )
            dndAtivadoPeloApp = true
            Log.d(TAG, "DND ativado (usuário digitando)")
        }
    }

    private fun desativarNaoPerturbe() {
        if (!notificationManager.isNotificationPolicyAccessGranted) return

        if (dndAtivadoPeloApp) {
            notificationManager.setInterruptionFilter(
                NotificationManager.INTERRUPTION_FILTER_ALL
            )
            dndAtivadoPeloApp = false
            Log.d(TAG, "DND desativado (usuário parou de digitar)")
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Serviço de acessibilidade interrompido")
    }

    companion object {
        private const val TAG = "DndAccessibilityService"
    }
}
