import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.Socket
import java.util.*

class ConnectionHandler(val clientSocket: Socket) : Runnable {

    val inputScanner = Scanner(BufferedReader(InputStreamReader(clientSocket.getInputStream()))).useDelimiter("")
    val outputStream = DataOutputStream(clientSocket.getOutputStream())
    val controller = ConnectionController()
    var tmpInput = String()
    var firstMessage = String()

    override fun run() {
        clientSocket.soTimeout = controller.getTimetout()

        while (true) {
            clientSocket.soTimeout = controller.getTimetout()

            if (inputScanner.hasNext()) {
                tmpInput += inputScanner.next()
            }

            if (hasAtLeastOneMessage(tmpInput)) {
                val response = controller.createNextStep (getFirstMessage(tmpInput))
                println(response.content)
                if (response.isLast){
                    close()
                    break
                }
            } else {
                val response = controller.prevalidate(tmpInput)
            }

        }

    }

    // TODO zamyslet se nad rozumnem rozdeleni logiky a handleru connection, co dela kdo
    // TODO handler zpracovava vstup a preda ho dal, ale mel by predat jen jednu zpravu, takze musi umet i optimalizovat, ale delka zpravy je vec logiky
    // TODO resi se jen situace, kdy nestihne dojit \a\b, zbytek muze v pohode resit logika

    private fun hasAtLeastOneMessage(tmpInput: String): Boolean {
        return tmpInput.contains("\u0007\b")
    }


    private fun getFirstMessage(tmpInput: String): String {
        if (!tmpInput.contains("\u0007\b")) {
            if (!controller.checkLength(tmpInput) && !controller.checkLength(tmpInput)) {
                throw Error("Fail") // TODO
            }
            return ""
        }
        return ""
    }

    private fun close(){
        inputScanner.close()
        outputStream.close()
        clientSocket.close()
    }


}
