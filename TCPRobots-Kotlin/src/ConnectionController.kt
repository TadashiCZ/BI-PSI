class ConnectionController {

    var charging: Boolean = false

    companion object {
        const val TIMEOUT_NORMAL = 1
        const val TIMEOUT_CHARGING = 5

    }

    fun checkLength(tmpInput: String): Boolean {
        return tmpInput.length < 10
    }

    fun getTimetout(): Int {
        return if (charging) TIMEOUT_CHARGING else TIMEOUT_NORMAL
    }

    fun createNextStep(inputMessage: String): Response {
        return Response(inputMessage)
    }

}
