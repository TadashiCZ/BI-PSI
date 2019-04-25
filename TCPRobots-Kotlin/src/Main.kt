import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

fun main(args: Array<String>) {
    val serverSocket: ServerSocket
    try {
        serverSocket = ServerSocket(args[0].toInt())
    } catch (ex: IOException) {
        println("Couldn't open a socket " + ex.printStackTrace())
        return
    }

    println("Starting server, waiting for clients.")


    while (true) {
        val clientSocket: Socket
        try {
            clientSocket = serverSocket.accept()
        } catch (ex: Exception) {
            println("Couldn't accept connection " + ex.printStackTrace())
            return
        }

        println("Accepted connection from robot: " + clientSocket.inetAddress)
        var handler: ConnectionHandler? = null
        try {
            handler = ConnectionHandler(clientSocket)
        } catch (e: Exception) {
            println("Can't initialize Handler: $e")
        }

        Thread(handler).start()
    }


}