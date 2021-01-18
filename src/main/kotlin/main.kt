import java.util.*

fun main() {

    // Algorithm will separate into 64 bit blocks
    val cleartext = "Text to encrypt using DES"

    // Algorithm will only use first 64 bits
    val key = "EncryptKey"

    val dataBitSets = cleartext.toBitSets()
    val keyBitSets = key.toBitSets(iskey = true)

    // generate all subKeys
    val subKeys = generateSubKeys(keyBitSets[0])

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

private fun String.toBitSets(iskey: Boolean = false): ArrayList<BitSet> {

    val dataBitsList = ArrayList<BitSet>()
    val byteArray = this.toByteArray(Charsets.UTF_8)
    val totalBitSet = BitSet()

    // BitSet.valueOf(byteArray) sets the BitSet with little-endian format... this will cause complexity for me and is prone to index errors during the DES round calculations
    //val totalBitSet = BitSet.valueOf(byteArray)

    // save bits in a big-endian format
    for ((i, c) in this.withIndex()) {
        val byte = Integer.toBinaryString(c.toInt()).padStart(8, '0')
        for (j in 0..7)
            totalBitSet[i * 8 + j] = (byte[j] == '1')
    }

    if (iskey) {
        dataBitsList.add(totalBitSet[0, 64])
    } else {

        // how many 8 byte (64 bit) groups
        val dataGroups = (byteArray.size + 8 - 1) / 8

        // separate into 64 bit bitSets
        for (i in 1..dataGroups) {
            dataBitsList.add(totalBitSet[(i - 1) * 64, i * 64])
        }

        // BitSet is now big-endian, so this validation will not be necessary
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

private fun generateSubKeys(K: BitSet): Array<BitSet> {

    // PC-1 key permutation
    val Kplus = BitSet()
    for (i in PC1.indices) {
        Kplus[i] = K[PC1[i] - 1]
    }

    // declare C and D key halves
    val C = Array(17) { BitSet() }
    val D = Array(17) { BitSet() }

    // store them correctly
    for (i in 0..27) C[0][i] = Kplus[i]
    for (i in 0..27) D[0][i] = Kplus[i + 28]

    // generate halves using left shifts
    for (i in 0..15) {
        C[i + 1].leftShift(KEY_SHIFTS[i], C[i])
        D[i + 1].leftShift(KEY_SHIFTS[i], D[i])
    }

    // declare joint CD keys
    val CDKeys = Array(17) { BitSet() }

    // join C and D
    for (i in 1..16) {
        for (j in 0..55) {
            if (j < 28) {
                CDKeys[i][j] = C[i][j]
            } else {
                CDKeys[i][j + 28] = D[i][j]
            }
        }
    }

    // declare final subKeys
    val subKeys = Array(17) { BitSet() }

    // PC-2 key permutation
    for (i in 1..16) {
        for (j in PC2.indices) {
            subKeys[i][j] = CDKeys[i][PC2[j] - 1]
        }
    }
    
    return subKeys;
}

private fun BitSet.leftShift(shifts: Int, previousBitSet: BitSet) {
    for (i in 1..shifts) {
        for (j in 0..26) {
            this[j] = previousBitSet[j+1]
        }
        this[27] = previousBitSet[0]
    }
}

private val PC1 = intArrayOf(
    57, 49, 41, 33, 25, 17, 9,
    1, 58, 50, 42, 34, 26, 18,
    10, 2, 59, 51, 43, 35, 27,
    19, 11, 3, 60, 52, 44, 36,
    63, 55, 47, 39, 31, 23, 15,
    7, 62, 54, 46, 38, 30, 22,
    14, 6, 61, 53, 45, 37, 29,
    21, 13, 5, 28, 20, 12, 4
)

private val KEY_SHIFTS = intArrayOf(1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1)

private val PC2 = intArrayOf(
    14, 17, 11, 24, 1, 5,
    3, 28, 15, 6, 21, 10,
    23, 19, 12, 4, 26, 8,
    16, 7, 27, 20, 13, 2,
    41, 52, 31, 37, 47, 55,
    30, 40, 51, 45, 33, 48,
    44, 49, 39, 56, 34, 53,
    46, 42, 50, 36, 29, 32
)