package rest_api.repository.model

import javax.persistence.*

/*@Entity
@Table(name = "utxos")
data class UTxOs(

    @Id
    val id: String, // client ID (his address)

    @Column
    @ElementCollection
    val lst: List<Pair<String?, String>> // UTxO (Tx_id, address)
)*/


data class UTxOs(
    @Id
    val id: String, // client ID (his address)

    @Column
    @ElementCollection
    val lst: MutableMap<String?, Unit> // UTxO (Tx_id, address)
)

