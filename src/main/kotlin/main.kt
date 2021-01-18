import java.util.*

fun main() {

    //val cleartext = "Text to encrypt usign DES"
    val cleartext = "Text to "
    val key = "chaveTeste"

    val dataBitSets = cleartext.toBitSets()

    val keyBitSets = key.toBitSets(iskey = true)

    //val ks = generateSubKeys()

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

private fun String.toBitSets(iskey: Boolean = false) : ArrayList<BitSet> {

    val dataBitsList = ArrayList<BitSet>()
    val byteArray = this.toByteArray(Charsets.UTF_8)
    val totalBitSet = BitSet.valueOf(byteArray)

    if (iskey) {
        dataBitsList.add(totalBitSet[0, 64])
    } else {

        // how many 8 byte (64 bit) groups
        val dataGroups = (byteArray.size + 8 - 1) / 8

        // separate into 64 bit bitsets
        for (i in 1..dataGroups) {
            dataBitsList.add(totalBitSet[(i - 1) * 64, i * 64])
        }

        // BitSet seems to be big-endian, so this validation will not be necessary
        /*if (dataBitsList.last().size() < 64) {

        }*/
    }

    // old method to convert to bitset (before finding out about BitSet.valueOf())
    /*val sb = StringBuilder()
    byteArray.forEach { byte ->
        // method get bit values with correct 0 paddings
        sb.append(Integer.toBinaryString((byte.toInt() and 0xFF) + 0x100).substring(1))
    }
    val binary = sb.toString()

    val bs = BitSet(64)
    for ((i,c) in binary.withIndex()) {
        bs[i] = (c == '1')
    }*/

    return dataBitsList

}