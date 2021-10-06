package org.rfcx.guardian.admin.comms.swm

class SwmCommandChecksum {

    companion object {
        fun get(text: String): String {
            var checksum = 0
            for (element in text) {
                checksum = checksum xor element.toInt()
            }
            var hex = Integer.toHexString(checksum)
            if (hex.length == 1) hex = "0$hex"
            return hex.toUpperCase()
        }

        fun verify(text: String, checksum: String): Boolean {
            return get(text) == checksum
        }
    }
}