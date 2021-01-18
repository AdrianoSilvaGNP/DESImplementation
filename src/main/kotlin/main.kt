import java.lang.StringBuilder
import java.util.*
import kotlin.experimental.and

fun main() {

    val cleartext = "Text toee"
    val byteArray = cleartext.toByteArray()
    cleartext.toBitSet()

    // 8*8 = 64 bits data
    /*if (byteArray.size <= 8) {
        var st = ""
        for (b in byteArray) {
            // convert to hexadecimal
            st += String.format("%02X", b)
        }
        print(st)
    }*/


    //TODO: Generate Subkeys

    //TODO: Implement DES Round including feistel function

}

private fun String.toBitSet() : BitSet {

    val byteArray = this.toByteArray()

    if (byteArray.size > 8) {

        val datagroups = byteArray.size / 8f

        //val secondBA = byteArray.sliceArray(IntRange(9, 16))
        //val secondBA = byteArray.
    }

    val sb = StringBuilder()
    byteArray.forEach { byte ->
        // method get bit values with correct 0 paddings
        sb.append(Integer.toBinaryString((byte.toInt() and 0xFF) + 0x100).substring(1))
    }
    val binary = sb.toString()

    val bs = BitSet(64)
    for ((i,c) in binary.withIndex()) {
        bs[i] = (c == '1')
    }

    return bs

}