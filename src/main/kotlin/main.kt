import java.util.*

/**
 * Created by Adriano Silva (17/01/21)
 */

fun main() {

    // Algorithm will separate into 64 bit blocks
    val cleartext = "Text to encrypt using DES"
    //val cleartext = "Text to "

    // Algorithm will only use first 64 bits
    val key = "EncryptKey"

    val dataBitSets = cleartext.toBitSets()
    val keyBitSets = key.toBitSets(iskey = true)

    // generate all subKeys
    val subKeys = generateSubKeys(keyBitSets[0])

    // run 16 DES Rounds (encrypt)
    val encryptionResult = DESRounds(dataBitSets, subKeys)

    val encryptedText = encryptionResult.toBinaryString()
    println(encryptedText)

    // reversing subkeys positions for decryption
    for (i in 1..8) {
        val temp = subKeys[i]
        subKeys[i] = subKeys[17 - i]
        subKeys[17 - i] = temp
    }

    //subKeys.reversed()
    //subKeys.reverse(1, 16)

    // run 16 DES Rounds (decrypt)
    val decryptionResult = DESRounds(encryptionResult, subKeys)

    val decryptedBinaryString = decryptionResult.toBinaryString()

    val byteArray = decryptedBinaryString.fromBinaryToByteArray(numberOfBytes = decryptedBinaryString.length / 8)

    val resultText = byteArray.toString(Charsets.UTF_8)

    println(resultText)

    // 8*8 = 64 bits data
    /*if (byteArray.size <= 8) {
        var st = ""
        for (b in byteArray) {
            // convert to hexadecimal
            st += String.format("%02X", b)
        }
        print(st)
    }*/
}

/**
 * Creates an Array of BitSets
 * @param iskey is this flag is true, only 1 set of 64 bits will be returned
 */
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

/**
 * returns the binary representation in a string of all BitSets contained in the ArrayList
 */
private fun ArrayList<BitSet>.toBinaryString(): String {
    var text = String()
    this.forEach { bitSet ->
        // separate bytes
        var binary = String()
        for (j in 0..63) {
            // to add spaces between bytes
            /*if (j > 0 && j % 8 == 0) {
                binary += " "
            }*/
            binary += (if (bitSet[j]) "1" else "0")
        }
        text += binary //"$binary "
    }

    return text
}

/**
 * Separates the binary string representation in bytes and includes them into a ByteArray for easier UTF-8 encoding
 */
private fun String.fromBinaryToByteArray(numberOfBytes : Int) : ByteArray {
    val byteArray = ByteArray(numberOfBytes)
    for ((counter, i) in (this.indices step 8).withIndex()){

        // extract 8 bit and get byte value (in decimal)
        val currentByte = this.substring(i, i + 8)
        val value = currentByte.toInt(2)

        byteArray[counter] = value.toByte()
    }

    return byteArray
}

/**
 * Generates the 16 SubKeys from the given initial 64 bit Key
 */
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

/**
 * Shifts the bits inside the bitSet to the left
 * @param shifts how many shifts are to be executed
 */
private fun BitSet.leftShift(shifts: Int, previousBitSet: BitSet) {
    val tempBitSet = BitSet()
    for (i in 1..shifts) {
        // if there is more than 1 shift we need to use the TempBitSet as previousBitSet
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

/**
 * DES defined 16 rounds
 */
private fun DESRounds(dataBitSets: ArrayList<BitSet>, subKeys: Array<BitSet>) : ArrayList<BitSet> {
    val resultList = ArrayList<BitSet>()

    dataBitSets.forEach { message ->

        // IP box permutation
        val IPmessage = BitSet()
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

        // join R with L (switch both 32 bit blocks)
        val RL = BitSet()
        for (j in 0..63) {
            if (j < 32) {
                RL[j] = R[16][j]
            } else {
                RL[j] = L[16][j - 32]
            }
        }

        // IP box permutation
        val IP2message = BitSet()
        for (i in IP2.indices) {
            IP2message[i] = RL[IP2[i] - 1]
        }

        resultList.add(IP2message)
    }

    return resultList
}

/**
 * Feistel cypher
 */
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
    for (j in 0..7) {
        for (k in 0..5) {
            B[j + 1][k] = ERBitSet[(j * 6) + k]
        }
    }

    // use S-box to resize 6 bits to 4 bits
    val S = Array(9) { BitSet() }
    for (i in 1..8) {

        // create 6 bit array for each group
        val binary = IntArray(6)
        for (j in 0..5) {
            binary[j] = (if (B[i][j]) 1 else 0)
        }

        // calcultate row and column
        val row = binary[0] * 2 + binary[5]
        val column = binary[1] * 8 + binary[2] * 4 + binary[3] * 2 + binary[4]

        // get S Table Result
        val SResult = S_TABLE[i - 1][row * 16 + column]

        // convert S Table Result to binary
        val SResultBin = Integer.toBinaryString(SResult).padStart(4, '0')

        // insert binary into BitSet (Big-Endian)
        for (k in 0..3) {
            S[i][k] = (SResultBin[k] == '1')
        }
    }

    // Rejoin S results into 32 bit group
    val SResultBitSet = BitSet()
    var counter = 0
    for (j in 1..8) {
        for (l in 0..3) {
            SResultBitSet[counter++] = S[j][l]
        }
    }

    // P box permutation
    val feistelResult = BitSet()
    for (i in P.indices) {
        feistelResult[i] = SResultBitSet[P[i] - 1]
    }

    return feistelResult
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

private val S_TABLE = arrayOf(
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

private val P = intArrayOf(
    16, 7, 20, 21,
    29, 12, 28, 17,
    1, 15, 23, 26,
    5, 18, 31, 10,
    2, 8, 24, 14,
    32, 27, 3, 9,
    19, 13, 30, 6,
    22, 11, 4, 25
)

private val IP2 = intArrayOf(
    40, 8, 48, 16, 56, 24, 64, 32,
    39, 7, 47, 15, 55, 23, 63, 31,
    38, 6, 46, 14, 54, 22, 62, 30,
    37, 5, 45, 13, 53, 21, 61, 29,
    36, 4, 44, 12, 52, 20, 60, 28,
    35, 3, 43, 11, 51, 19, 59, 27,
    34, 2, 42, 10, 50, 18, 58, 26,
    33, 1, 41, 9, 49, 17, 57, 25
)