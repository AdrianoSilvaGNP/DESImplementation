import java.util.*

/**
 * Created by Adriano Silva (17/01/21)
 */

fun main() {

    // Algorithm will separate into 64 bit blocks
    val cleartext = "Text to encrypt using DES"

    // Algorithm will only use first 64 bits
    val key = "EncryptKey"

    val dataBitSets = cleartext.toBitSets()
    val keyBitSets = key.toBitSets(iskey = true)

    // generate all subKeys
    val subKeys = generateSubKeys(keyBitSets[0])

    val result = DESRounds(dataBitSets, subKeys)

    // 8*8 = 64 bits data
    /*if (byteArray.size <= 8) {
        var st = ""
        for (b in byteArray) {
            // convert to hexadecimal
            st += String.format("%02X", b)
        }
        print(st)
    }*/

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
                CDKeys[i][j] = D[i][j - 28]
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
    val tempBitSet = BitSet()
    for (i in 1..shifts) {

        if (i > 1) {
            val temp = tempBitSet[0]
            for (j in 0..26) {
                tempBitSet[j] = tempBitSet[j+1]
            }
            tempBitSet[27] = temp
        } else {
            tempBitSet[27] = previousBitSet[0]
            for (j in 0..26) {
                tempBitSet[j] = previousBitSet[j+1]
            }
        }
    }

    for (i in 0..27) {
        this[i] = tempBitSet[i]
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

private fun DESRounds(dataBitSets: ArrayList<BitSet>, subKeys: Array<BitSet>) {
    dataBitSets.forEach { message ->

        val IPmessage = BitSet()
        // IP box permutation
        for (i in IP.indices) {
            IPmessage[i] = message[IP[i] - 1]
        }

        // declare L and R data halves
        val L = Array(17) { BitSet() }
        val R = Array(17) { BitSet() }

        // store them correctly
        for (i in 0..31) L[0][i] = IPmessage[i]
        for (i in 0..31) R[0][i] = IPmessage[i + 32]

        // DES round iteration. XOR with feistel cypher
        for (i in 1..16) {
            L[i] = R[i - 1]
            L[i - 1].xor(feistel(R[i - 1], subKeys[i]))
            R[i] = L[i - 1]
        }

    }
}

private fun feistel(RBitSet: BitSet, subKey: BitSet): BitSet {

    // R BitSet expansion through E table
    val ERBitSet = BitSet()
    for (i in E.indices) {
        ERBitSet[i] = RBitSet[E[i] - 1]
    }

    // XOR
    ERBitSet.xor(subKey)

    // separate into 6 bit groups
    val B = Array(9) { BitSet() }
    for (i in 0..47) {
        for (j in 1..8) {
            for (k in 0..5) {
                B[j][k] = ERBitSet[i]
            }
        }
    }

    val teste = ""

    return ERBitSet

}

private val IP = intArrayOf(
    58, 50, 42, 34, 26, 18, 10, 2,
    60, 52, 44, 36, 28, 20, 12, 4,
    62, 54, 46, 38, 30, 22, 14, 6,
    64, 56, 48, 40, 32, 24, 16, 8,
    57, 49, 41, 33, 25, 17, 9, 1,
    59, 51, 43, 35, 27, 19, 11, 3,
    61, 53, 45, 37, 29, 21, 13, 5,
    63, 55, 47, 39, 31, 23, 15, 7
)

private val E = intArrayOf(
    32, 1, 2, 3, 4, 5,
    4, 5, 6, 7, 8, 9,
    8, 9, 10, 11, 12, 13,
    12, 13, 14, 15, 16, 17,
    16, 17, 18, 19, 20, 21,
    20, 21, 22, 23, 24, 25,
    24, 25, 26, 27, 28, 29,
    28, 29, 30, 31, 32, 1
)

private val S = arrayOf(
    intArrayOf(
        14, 4, 13, 1, 2, 15, 11, 8, 3, 10, 6, 12, 5, 9, 0, 7,
        0, 15, 7, 4, 14, 2, 13, 1, 10, 6, 12, 11, 9, 5, 3, 8,
        4, 1, 14, 8, 13, 6, 2, 11, 15, 12, 9, 7, 3, 10, 5, 0,
        15, 12, 8, 2, 4, 9, 1, 7, 5, 11, 3, 14, 10, 0, 6, 13
    ),

    intArrayOf(
        15, 1, 8, 14, 6, 11, 3, 4, 9, 7, 2, 13, 12, 0, 5, 10,
        3, 13, 4, 7, 15, 2, 8, 14, 12, 0, 1, 10, 6, 9, 11, 5,
        0, 14, 7, 11, 10, 4, 13, 1, 5, 8, 12, 6, 9, 3, 2, 15,
        13, 8, 10, 1, 3, 15, 4, 2, 11, 6, 7, 12, 0, 5, 14, 9
    ),

    intArrayOf(
        10, 0, 9, 14, 6, 3, 15, 5, 1, 13, 12, 7, 11, 4, 2, 8,
        13, 7, 0, 9, 3, 4, 6, 10, 2, 8, 5, 14, 12, 11, 15, 1,
        13, 6, 4, 9, 8, 15, 3, 0, 11, 1, 2, 12, 5, 10, 14, 7,
        1, 10, 13, 0, 6, 9, 8, 7, 4, 15, 14, 3, 11, 5, 2, 12
    ),

    intArrayOf(
        7, 13, 14, 3, 0, 6, 9, 10, 1, 2, 8, 5, 11, 12, 4, 15,
        13, 8, 11, 5, 6, 15, 0, 3, 4, 7, 2, 12, 1, 10, 14, 9,
        10, 6, 9, 0, 12, 11, 7, 13, 15, 1, 3, 14, 5, 2, 8, 4,
        3, 15, 0, 6, 10, 1, 13, 8, 9, 4, 5, 11, 12, 7, 2, 14
    ),

    intArrayOf(
        2, 12, 4, 1, 7, 10, 11, 6, 8, 5, 3, 15, 13, 0, 14, 9,
        14, 11, 2, 12, 4, 7, 13, 1, 5, 0, 15, 10, 3, 9, 8, 6,
        4, 2, 1, 11, 10, 13, 7, 8, 15, 9, 12, 5, 6, 3, 0, 14,
        11, 8, 12, 7, 1, 14, 2, 13, 6, 15, 0, 9, 10, 4, 5, 3
    ),

    intArrayOf(
        12, 1, 10, 15, 9, 2, 6, 8, 0, 13, 3, 4, 14, 7, 5, 11,
        10, 15, 4, 2, 7, 12, 9, 5, 6, 1, 13, 14, 0, 11, 3, 8,
        9, 14, 15, 5, 2, 8, 12, 3, 7, 0, 4, 10, 1, 13, 11, 6,
        4, 3, 2, 12, 9, 5, 15, 10, 11, 14, 1, 7, 6, 0, 8, 13
    ),

    intArrayOf(
        4, 11, 2, 14, 15, 0, 8, 13, 3, 12, 9, 7, 5, 10, 6, 1,
        13, 0, 11, 7, 4, 9, 1, 10, 14, 3, 5, 12, 2, 15, 8, 6,
        1, 4, 11, 13, 12, 3, 7, 14, 10, 15, 6, 8, 0, 5, 9, 2,
        6, 11, 13, 8, 1, 4, 10, 7, 9, 5, 0, 15, 14, 2, 3, 12
    ),

    intArrayOf(
        13, 2, 8, 4, 6, 15, 11, 1, 10, 9, 3, 14, 5, 0, 12, 7,
        1, 15, 13, 8, 10, 3, 7, 4, 12, 5, 6, 11, 0, 14, 9, 2,
        7, 11, 4, 1, 9, 12, 14, 2, 0, 6, 10, 13, 15, 3, 5, 8,
        2, 1, 14, 7, 4, 10, 8, 13, 15, 12, 9, 0, 3, 5, 6, 11
    )
)