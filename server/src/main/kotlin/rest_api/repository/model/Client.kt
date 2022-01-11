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
 * Represents the database entity for storing the client details.
 */
@Entity
@Table(name = "client")
data class Client(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: String, // long is only 64 bits - need 128
    @Column(name = "coins", nullable = false)
    val coins: ULong,
)
