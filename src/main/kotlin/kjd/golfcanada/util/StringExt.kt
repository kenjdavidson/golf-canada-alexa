package kjd.golfcanada.util

fun String.equalsOrThrows(expected: String, producer: (actual: String) -> Exception): Boolean {
    if (this != expected) {
        throw producer(this)
    }
    return true
}