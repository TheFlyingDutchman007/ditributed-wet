package rest_api.repository.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
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
    val id: Long?, // TODO: long is only 64 bits - need 128

    @Column(name = "transfer", nullable = false)
    val transfer: Pair<Long,ULong>,

    @Column(name = "UTxO", nullable = false)
    val UTxO: String,


    @Column(name = "middle_name", nullable = true)
    val middleName: String?,
    @Column(name = "last_name", nullable = false)
    val lastName: String,
    @Column(name = "email_address", nullable = false)
    val emailId: String,
    @Column(name = "day_of_birth", nullable = false)
    @JsonProperty("day_of_birth")
    val dayOfBirth: LocalDate,
)