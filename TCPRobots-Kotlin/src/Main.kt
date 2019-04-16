import java.io.IOException
import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket

fun main(args: Array<String>) {
    var serverSocket: ServerSocket
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
        val handler = ConnectionHandler(clientSocket)
        Thread(handler).run()
    }


}