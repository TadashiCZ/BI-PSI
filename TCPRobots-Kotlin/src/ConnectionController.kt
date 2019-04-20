import java.lang.NumberFormatException

enum class State {
    USERNAME,
    CONFIRMATION,
    FIRST_MOVE,
    SECOND_MOVE,
    NAVIGATING_TO_ZONE,
    SEARCH_ZONE,
    PICK_UP
}


class ConnectionController {

    private var charging: Boolean = false
    var state = State.USERNAME
    var robot: Robot? = null
    var searched = 0


    companion object {
        const val TIMEOUT_NORMAL = 1
        const val TIMEOUT_CHARGING = 5

        const val SERVER_MOVE = "102 MOVE\u0007\b"                 // Příkaz pro pohyb o jedno pole vpřed
        const val SERVER_TURN_LEFT = "103 TURN LEFT\u0007\b"       // Příkaz pro otočení doleva
        const val SERVER_TURN_RIGHT = "104 TURN RIGHT\u0007\b"     // Příkaz pro otočení doprava
        const val SERVER_PICK_UP = "105 GET MESSAGE\u0007\b"       // Příkaz pro vyzvednutí zprávy
        const val SERVER_LOGOUT = "106 LOGOUT\u0007\b"             // Příkaz pro ukončení spojení po úspěchu
        const val SERVER_OK = "200 OK\u0007\b"                     // Kladné potvrzení
        const val SERVER_LOGIN_FAILED = "300 LOGIN FAILED\u0007\b" // Nezdařená autentizace
        const val SERVER_SYNTAX_ERROR = "301 SYNTAX ERROR\u0007\b" // Chybná syntaxe zprávy
        const val SERVER_LOGIC_ERROR = "302 LOGIC ERROR\u0007\b"   // Zpráva odeslaná ve špatné situaci

        const val CLIENT_FULL_POWER = "FULL POWER"
        const val CLIENT_RECHARGING = "RECHARGING"

        const val CLIENT_USERNAME_LENGTH = 10
        const val CLIENT_CONFIRMATION_LENGTH = 5
        const val CLIENT_OK_LENGTH = 10
        const val CLIENT_RECHARGING_LENGTH = 10
        const val CLIENT_FULL_POWER_LENGTH = 10
        const val CLIENT_MESSAGE_LENGTH = 98

        const val SERVER_KEY = 54621
        const val CLIENT_KEY = 45328

    }

    fun checkLength(length: Int): Boolean {


        return when (state) {
            State.USERNAME -> length <= CLIENT_USERNAME_LENGTH
            State.PICK_UP -> length <= CLIENT_MESSAGE_LENGTH
            State.CONFIRMATION -> length <= CLIENT_CONFIRMATION_LENGTH
            State.NAVIGATING_TO_ZONE, State.FIRST_MOVE, State.SECOND_MOVE, State.SEARCH_ZONE -> length <= CLIENT_OK_LENGTH
        }
    }

    fun getTimeout(): Int {
        return 1000 * if (charging) TIMEOUT_CHARGING else TIMEOUT_NORMAL
    }

    fun createNextStep(inputMessage: String): Response {
        if (charging) {
            println("Charging: $inputMessage")
            if (inputMessage != CLIENT_FULL_POWER) {
                println("Y U charging")
                return Response(SERVER_LOGIC_ERROR, true)
            }
            charging = false
            return Response("")
        }

        if (inputMessage == CLIENT_RECHARGING) {
            charging = true
            return Response("")
        }


        return when (state) {
            State.USERNAME -> checkUsernameAndSendHash(inputMessage)
            State.CONFIRMATION -> checkClientHashAndSendAnswer(inputMessage)
            State.FIRST_MOVE -> firstMove(inputMessage)
            State.SECOND_MOVE -> secondMove(inputMessage)
            State.NAVIGATING_TO_ZONE -> navigatingToZone(inputMessage)
            State.SEARCH_ZONE -> searchTheZone(inputMessage)
            State.PICK_UP -> pickUp(inputMessage)
        }


    }

    private fun checkUsernameAndSendHash(inputMessage: String): Response {
        if (!checkLength(inputMessage.length)) {
            return Response(SERVER_SYNTAX_ERROR, true)
        }
        robot = Robot(inputMessage)
        state = State.CONFIRMATION
        return Response("${robot!!.serverHash}\u0007\b")
    }

    private fun checkClientHashAndSendAnswer(inputMessage: String): Response {
        if (!checkLength(inputMessage.length)) {
            return Response(SERVER_SYNTAX_ERROR, true)
        }

        val key: Int
        try {
            key = inputMessage.toInt()
        } catch (ex: NumberFormatException) {
            return Response(SERVER_SYNTAX_ERROR, true)
        }

        return if (robot!!.clientHash == key) {
            state = State.FIRST_MOVE
            Response(SERVER_OK + SERVER_MOVE)
        } else {
            Response(SERVER_LOGIN_FAILED, true)
        }
    }

    private fun firstMove(inputMessage: String): Response {
        if (!checkLength(inputMessage.length)) {
            return Response(SERVER_SYNTAX_ERROR, true)
        }

        val newCoordinates = Coordinates.parse(inputMessage) ?: return Response(SERVER_SYNTAX_ERROR, true)
        // FIXME: Is Elvis operator above correct?

        println("New coordinates are: $newCoordinates")
        state = State.SECOND_MOVE
        robot!!.coordinates.x = newCoordinates.x
        robot!!.coordinates.y = newCoordinates.y
        return Response(SERVER_MOVE)
    }

    private fun secondMove(inputMessage: String): Response {
        if (!checkLength(inputMessage.length)) {
            return Response(SERVER_SYNTAX_ERROR, true)
        }

        val newCoordinates = Coordinates.parse(inputMessage) ?: return Response(SERVER_SYNTAX_ERROR, true)
        if (newCoordinates.x == robot!!.coordinates.x && newCoordinates.y == robot!!.coordinates.y) {
            // didn't move, try again
            return Response(SERVER_MOVE)
        }

        when {
            newCoordinates.x < robot!!.coordinates.x -> robot!!.orientation = Orientation.WEST
            newCoordinates.x > robot!!.coordinates.x -> robot!!.orientation = Orientation.EAST
            newCoordinates.y < robot!!.coordinates.y -> robot!!.orientation = Orientation.SOUTH
            newCoordinates.y > robot!!.coordinates.y -> robot!!.orientation = Orientation.NORTH
            else -> println("Which why are you looking my god?")
        }

        println("State is: $state, Robot current coordinates: ${robot!!.coordinates} and the new one are $newCoordinates, the orientation is: ${robot!!.orientation} ")

        robot!!.coordinates = newCoordinates
        state = State.NAVIGATING_TO_ZONE
        return if (robot!!.isNextMoveRotation()) {
            robot!!.rotateRight()
            Response(SERVER_TURN_RIGHT)
        } else {
            Response(SERVER_MOVE)
        }

    }

    private fun navigatingToZone(inputMessage: String): Response {
        if (!checkLength(inputMessage.length)) {
            return Response(SERVER_SYNTAX_ERROR, true)
        }
        robot!!.coordinates = Coordinates.parse(inputMessage) ?: return Response(SERVER_SYNTAX_ERROR, true)

        println("State is: $state, Robot current coordinates: ${robot!!.coordinates} and the orientation is: ${robot!!.orientation}")

        if (robot!!.atZoneStart()) {
            state = State.SEARCH_ZONE
            robot!!.rotateLeft()
            return Response(SERVER_TURN_LEFT)
        }
        return if (robot!!.isNextMoveRotation()) {
            robot!!.rotateRight()
            Response(SERVER_TURN_RIGHT)
        } else {
            Response(SERVER_MOVE)
        }

    }

    private fun searchTheZone(inputMessage: String): Response {
        if (!checkLength(inputMessage.length)) {
            return Response(SERVER_SYNTAX_ERROR, true)
        }
        robot!!.coordinates = Coordinates.parse(inputMessage) ?: return Response(SERVER_SYNTAX_ERROR, true)

        // TODO needs changing if robot stalling in the zone

        println("State is: $state, searched is $searched, Robot current coordinates: ${robot!!.coordinates} and the orientation is: ${robot!!.orientation}")

        when (searched) {
            0, 1, 2, 3, 10, 11, 12, 13, 20, 21, 22, 23, 24 ->
                if (robot!!.orientation == Orientation.SOUTH) {
                    searched++
                    state = State.PICK_UP
                    return Response(SERVER_PICK_UP)
                } else {
                    robot!!.rotateRight()
                    return Response(SERVER_TURN_RIGHT)
                }
            5, 6, 7, 8, 15, 16, 17, 18 ->
                if (robot!!.orientation == Orientation.NORTH) {
                    searched++
                    state = State.PICK_UP
                    return Response(SERVER_PICK_UP)
                } else {
                    robot!!.rotateRight()
                    return Response(SERVER_TURN_RIGHT)
                }
            4, 9, 14, 19 ->
                if (robot!!.orientation == Orientation.EAST) {
                    searched++
                    state = State.PICK_UP
                    return Response(SERVER_PICK_UP)
                } else {
                    robot!!.rotateRight()
                    return Response(SERVER_TURN_RIGHT)
                }
            else -> {
                println("No message found in the zone!!")
                return Response(SERVER_LOGIC_ERROR, true)
            }
        }
    }

    private fun pickUp(inputMessage: String): Response {
        if (!checkLength(inputMessage.length)) {
            return Response(SERVER_SYNTAX_ERROR, true)
        }

        if (inputMessage.isNotEmpty()) {
            return Response(SERVER_LOGOUT, true)
        } else {
            state = State.SEARCH_ZONE

            when (searched) {
                1, 2, 3, 4, 11, 12, 13, 14, 21, 22, 23, 24, 25 ->
                    if (robot!!.orientation == Orientation.SOUTH) {
                        return Response(SERVER_MOVE)
                    } else {
                        robot!!.rotateRight()
                        return Response(SERVER_TURN_RIGHT)
                    }
                6, 7, 8, 9, 16, 17, 18, 19 ->
                    if (robot!!.orientation == Orientation.NORTH) {
                        return Response(SERVER_MOVE)
                    } else {
                        robot!!.rotateRight()
                        return Response(SERVER_TURN_RIGHT)
                    }
                5, 10, 15, 20 ->
                    if (robot!!.orientation == Orientation.EAST) {
                        return Response(SERVER_MOVE)
                    } else {
                        robot!!.rotateRight()
                        return Response(SERVER_TURN_RIGHT)
                    }
                else -> {
                    println("No message found in the zone!!")
                    return Response(SERVER_LOGIC_ERROR, true)
                }


            }


        }
    }


    fun prevalidate(tmpInput: String): Response {
        if (tmpInput.contains("\u0007")) {
            if (!checkLength(tmpInput.length - 1)) {
                return Response(SERVER_SYNTAX_ERROR, true)
            }
        } else {
            if (!checkLength(tmpInput.length)) {
                return Response(SERVER_SYNTAX_ERROR, true)
            }
        }
        return Response("")
    }

}
