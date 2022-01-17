package rest_api.repository.model


/**
 * Represents the database entity for storing the client details.
 */
/*@Entity
@Table(name = "client")
data class Clients(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: String, // long is only 64 bits - need 128
    @Column(name = "coins", nullable = false)
    val coins: ULong,
)*/


data class Clients(
    var addresses : MutableMap<String,UTxOs>
)
