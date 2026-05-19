package com.pocketvalo.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ServerSocket
import java.net.SocketTimeoutException

/**
 * Local HTTP server listening on port 80 for Riot OAuth redirect.
 *
 * Riot only accepts http://localhost/redirect as redirect_uri for client_id=riot-client.
 * That means the browser will hit localhost:80 — so we must listen on port 80.
 *
 * Note: If port 80 is unavailable (permission denied on some devices),
 * start() returns -1 and login will fall back to error state.
 */
class LocalAuthServer {

    private var serverSocket: ServerSocket? = null

    fun start(): Int {
        return try {
            serverSocket = ServerSocket(80).also {
                it.soTimeout = 120_000 // 2 minute timeout
            }
            80
        } catch (e: Exception) {
            // Port 80 failed — try port 8080 as fallback (won't work with Riot redirect)
            // but at least we know the error
            android.util.Log.e("LocalAuthServer", "Failed to bind port 80: ${e.message}")
            -1
        }
    }

    suspend fun waitForCode(): String? = withContext(Dispatchers.IO) {
        try {
            val server = serverSocket ?: return@withContext null
            val socket = server.accept()

            val input = socket.getInputStream().bufferedReader()
            val requestLine = input.readLine() ?: return@withContext null
            android.util.Log.d("LocalAuthServer", "Request: $requestLine")

            val code = extractCode(requestLine)

            val responseBody = if (code != null) {
                "<html><body style='font-family:sans-serif;text-align:center;padding:40px'>" +
                        "<h2>✅ Login successful!</h2>" +
                        "<p>You can close this tab and return to Pocket Valo.</p>" +
                        "</body></html>"
            } else {
                "<html><body style='font-family:sans-serif;text-align:center;padding:40px'>" +
                        "<h2>❌ Login failed.</h2><p>Please try again.</p>" +
                        "</body></html>"
            }

            val response = buildString {
                append("HTTP/1.1 200 OK\r\n")
                append("Content-Type: text/html; charset=utf-8\r\n")
                append("Content-Length: ${responseBody.toByteArray().size}\r\n")
                append("Connection: close\r\n")
                append("\r\n")
                append(responseBody)
            }

            socket.getOutputStream().write(response.toByteArray())
            socket.getOutputStream().flush()
            socket.close()

            code
        } catch (e: SocketTimeoutException) {
            android.util.Log.d("LocalAuthServer", "Timeout waiting for redirect")
            null
        } catch (e: Exception) {
            android.util.Log.e("LocalAuthServer", "Error: ${e.message}")
            null
        }
    }

    fun stop() {
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
    }

    private fun extractCode(requestLine: String): String? {
        // "GET /redirect?code=XXXX&... HTTP/1.1"
        return try {
            val path = requestLine.split(" ").getOrNull(1) ?: return null
            val query = path.substringAfter("?", "")
            query.split("&")
                .map { it.split("=") }
                .firstOrNull { it.firstOrNull() == "code" }
                ?.getOrNull(1)
        } catch (e: Exception) {
            null
        }
    }
}