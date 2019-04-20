import ConnectionController.Companion.CLIENT_KEY
import ConnectionController.Companion.SERVER_KEY

enum class Orientation {
    UNINITIALIZED,
    NORTH,
    EAST,
    SOUTH,
    WEST {
        override operator fun next(): Orientation {
            return values()[1]
        }
    };


    open operator fun next(): Orientation {
        return values()[ordinal + 1]
    }

    fun previous(): Orientation {
        return if (ordinal == 1) {
            WEST
        } else {
            values()[ordinal - 1]
        }
    }
}


class Robot(name: String) {
    var orientation = Orientation.UNINITIALIZED
    var coordinates = Coordinates(666, 666)
    val nameHash: Int
    val clientHash: Int
    val serverHash: Int

    init {
        var hash = 0
        for (letter in name) {
            hash += letter.toInt()
        }
        hash = (hash * 1000) % 65536
        nameHash = hash;
        serverHash = (nameHash + SERVER_KEY) % 65536
        clientHash = (nameHash + CLIENT_KEY) % 65536
    }

    fun isNextMoveRotation(): Boolean {
        if (coordinates.x < -2 && orientation != Orientation.EAST) {
            return true
        } else if (coordinates.x > -2 && orientation != Orientation.WEST) {
            return true
        }
        if (coordinates.x == -2) {
            if (coordinates.y < 2 && orientation != Orientation.NORTH) {
                return true
            } else if (coordinates.y > 2 && orientation != Orientation.SOUTH) {
                return true
            }
        }

        return false
    }

    fun rotateRight() {
        orientation = orientation.next()
    }

    fun rotateLeft() {
        orientation = orientation.previous()
    }

    fun atZoneStart(): Boolean {
        return coordinates.x == -2 && coordinates.y == 2
    }

}
