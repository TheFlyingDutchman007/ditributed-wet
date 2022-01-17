package rest_api.repository.model

import com.fasterxml.jackson.annotation.JsonProperty
import org.hibernate.annotations.CollectionType
import org.springframework.beans.factory.annotation.Value
import java.math.BigInteger
import java.time.LocalDate
import java.util.*
import javax.persistence.*


typealias input_tx_id = List<String> // (TX-id, address) = UTxO
typealias input_address = List<String> // (TX-id, address) = UTxO
typealias output_address = List<String> // (address, coins) = transfer
typealias output_coins = List<Long> // (address, coins) = transfer

/**
 * Represents the database entity for storing the transaction details.
 */
/*@Entity
@Table(name = "transaction")
data class Transaction(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val tx_id: String?, // long is only 64 bits - need 128

    @Column(name = "inputs_tx_id", nullable = false)
    @ElementCollection
    val inputs_tx_id: input_tx_id,

    @Column(name = "inputs_address", nullable = false)
    @ElementCollection
    val inputs_address: input_address,

    @Column(name = "outputs_address", nullable = false)
    @ElementCollection
    val outputs_address: output_address,

    @Column(name = "outputs_coins", nullable = false)
    @ElementCollection
    val outputs_coins: output_coins,

    )*/


data class Transaction(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val tx_id: String?, // long is only 64 bits - need 128

    @ElementCollection
    val inputs_tx_id: input_tx_id,

    @ElementCollection
    val inputs_address: input_address,

    @ElementCollection
    val outputs_address: output_address,

    @ElementCollection
    val outputs_coins: output_coins,

    )
