package gr.osnet.rxsocket.meta


val String.addCheckSum: String
    get() { //Sum of Bytes % 65536
        var sum: Long = 0
        this.forEach { sum += it.toLong() }
        return this + sum.toString(16).padStart(4, '0').takeLast(4).toUpperCase()
    }

val String.removeCheckSum get() = this.substring(0, this.length - 4)
val String.validateCheckSum get() = this.substring(0, this.length - 4).addCheckSum == this
 
