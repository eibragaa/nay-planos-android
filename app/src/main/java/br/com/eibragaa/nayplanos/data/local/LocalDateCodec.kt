package br.com.eibragaa.nayplanos.data.local

import java.time.LocalDate

/** ISO-8601 date serialization used at the persistence boundary. */
object LocalDateCodec {
    fun encode(date: LocalDate): String = date.toString()

    fun decode(value: String): LocalDate = LocalDate.parse(value)
}
