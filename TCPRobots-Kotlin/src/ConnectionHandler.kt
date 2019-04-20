import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.*

class ConnectionHandler(val clientSocket: Socket) : Runnable {

    val inputReader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
    val outputStream = DataOutputStream(clientSocket.getOutputStream())
    val controller = ConnectionController()
    var tmpInput = String()
    var cnt = 0

    override fun run() {
        try {
            while (true) {
                clientSocket.soTimeout = controller.getTimeout()
                //  println("${clientSocket.soTimeout}")

                while (inputReader.ready()) {
                    val int = inputReader.read()
                    //  println(int)
                    tmpInput += int.toChar()
                }

                if (tmpInput.isNotEmpty()) {
                    print("TMP: $tmpInput\nTMP in ASCII:")
                    for (letter in tmpInput) {
                        print(" ${letter.toInt()},")
                    }
                    println("")

                }


                val response: Response = if (hasAtLeastOneMessage(tmpInput)) {
                    controller.createNextStep(getFirstMessage())
                } else {
                    controller.prevalidate(tmpInput)
                }

                if (response.content != "") {
                    println("Sending message: ${response.content}\n")
                    outputStream.write(response.content.toByteArray())
                    outputStream.flush()
                }

                if (response.isLast) {
                    println("Closing connection with ${clientSocket.inetAddress}")
                    break
                }
            }
        } catch (ex: Exception) {
            println("Error: $ex")
        }

        close()

    }

    private fun hasAtLeastOneMessage(tmpInput: String): Boolean {
        return tmpInput.contains("\u0007\b") || ((controller.state == State.PICK_UP || controller.state == State.SEARCH_ZONE) && tmpInput == "\u0007\b")
    }


    private fun getFirstMessage(): String {
        if ((controller.state == State.PICK_UP || controller.state == State.SEARCH_ZONE) && tmpInput == "\u0007\b") {
            tmpInput = ""
            return ""
        } else {
            if (!tmpInput.contains("\u0007\b")) {
                if (!controller.checkLength(tmpInput.length)) {
                    throw Error("Fail") // TODO
                }

                throw Error("Fail 2") // TODO
            }
            val retString = tmpInput.substringBefore("\u0007\b")
            tmpInput = tmpInput.substringAfter("\u0007\b")
            return retString
        }
    }

    private fun close() {
        clientSocket.close()
        inputReader.close()
        outputStream.close()
    }


}
