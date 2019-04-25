import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.Socket


class ConnectionHandler(private val clientSocket: Socket) : Runnable {

    private val inputReader = InputStreamReader(clientSocket.getInputStream())
    private val outputStream = DataOutputStream(clientSocket.getOutputStream())
    private val controller = ConnectionController()
    var tmpInput = String()

    override fun run() {

        try {
            while (true) {
                clientSocket.soTimeout = controller.getTimeout()
                val tmpBuffer = "čččččččččččč".toCharArray()
                val ret = inputReader.read(tmpBuffer, 0, 12)
                if (ret == -1) {
                    break
                }

                val inp = StringBuilder()
                for (i in 0 until tmpBuffer.size) {
                    if (tmpBuffer[i] == 'č') {
                        break
                    }
                    inp.append(tmpBuffer[i])
                }
                tmpInput += inp.toString()

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
