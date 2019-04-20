import java.util.regex.Pattern

class Coordinates constructor(var x: Int, var y: Int) {

    companion object {

        fun parse(input: String): Coordinates? {
            if (input.length > 10)
                return null

            val p = Pattern.compile("OK\\s(-?\\d{1,3})\\s(-?\\d{1,3})")
            val m = p.matcher(input)
            return if (!m.matches()) {
                null
            } else Coordinates(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)))

        }
    }

    override fun toString(): String {
        return "x: $x, y: $y"
    }
}