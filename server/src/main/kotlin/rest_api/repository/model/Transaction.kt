package rest_api.repository.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigInteger
import java.time.LocalDate
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table


/**
 * Represents the database entity for storing the transaction details.
 */
@Entity
@Table(name = "transaction")
data class Transaction(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: String?, // long is only 64 bits - need 128

    @Column(name = "transfer", nullable = false)
    val transfer: String,

    @Column(name = "UTxO", nullable = false)
    val UTxO: String,

)