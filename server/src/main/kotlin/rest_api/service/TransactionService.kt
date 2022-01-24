package rest_api.service

import org.springframework.stereotype.Service
import rest_api.repository.model.Clients
import rest_api.repository.model.Transaction
import rest_api.repository.model.TransactionsLedger
import rest_api.repository.model.UTxOs
import java.util.*
import java.util.stream.Stream
import kotlin.math.pow
import kotlin.reflect.typeOf
const val initMoney : Long = 10000
const val numShards : Int = 3


fun getTxFromLedger(ledger: TransactionsLedger, tx_id : Long) : Transaction{
    val tx = ledger.ledger.last{it.tx_id == tx_id}
    return tx
}

fun getCoinsFromTxOutput(tx : Transaction, address: String) : Long{
    var coins : Long = 0
    for ((i,addr) in tx.outputs_address.withIndex()){
        if (addr == address){
            coins = tx.outputs_coins[i]
        }
    }
    return coins
}

fun gen_clients(service: TransactionService ,ledger: TransactionsLedger){
    for (i in 1..5){
        clients.addresses[i.toString()] = UTxOs(i.toString(), mutableMapOf())
        /*val genesisUTxOTxid  = service.getUnspentTransactions("0")["0->" +(i-1).toString()] // he got only 1 utxo (bank of money)
        println(service.getUnspentTransactions("0"))
        println(service.getUnspentTransactions("0")["0->" +(i-1).toString()])*/
        val tx = getTxFromLedger(ledger,(i-1).toLong())
        val genesisMoney = getCoinsFromTxOutput(tx,"0")
        val genTx = Transaction((i).toLong(), listOf((i-1).toLong()), listOf("0"),
            listOf(i.toString(),"0"), listOf(100, genesisMoney-100))
        service.createTransaction(genTx)
    }
}

var ledger: TransactionsLedger = TransactionsLedger(mutableListOf(), mutableSetOf())
var clients : Clients = Clients(mutableMapOf())
var txIds : MutableSet<Long> = mutableSetOf() // might be redundant
var nextId: Long = (Long.MAX_VALUE / numShards)
var tx_stream : MutableList<Transaction> = mutableListOf()


/**
 * Service for interactions with employee domain object
 */
@Service
class TransactionService (private val shard: Int = 0) {

    // TODO: add limit for history (from the end back???)

    init {
        // init ledger
        val init_transaction = Transaction(0, listOf(-1),listOf("-1"),listOf("0"),listOf(initMoney))
        ledger = TransactionsLedger(mutableListOf(init_transaction), mutableSetOf(0))

        // init clients with genesis
        val genesis_utxo = UTxOs("0",mutableMapOf(Pair(0,Unit)))
        clients = Clients(mutableMapOf(Pair("0",genesis_utxo)))

        gen_clients(this,ledger)
        tx_stream.clear()


        println(nextId)
        // important because of generation of clients!!
        if (nextId == 0.toLong()) {
            println("nextID from 0 to 6")
            nextId = 6
        }
    }


    /**
     * Create transaction.
     *
     * @param tx the transaction
     * @return status (TRUEEEEEEEE)
     */
    fun createTransaction(tx: Transaction): Boolean {
        if (tx.tx_id > 5) {
            tx.tx_id = nextId
        }
        val sender_addr = tx.inputs_address[0]  // get the sender of the tx
        if (clients.addresses.containsKey(sender_addr)){ // sender already exists
            // TODO: add checks for valid utxo -> choose utxos to work with - need to?????

            // erase input utxo's from sender
            for ((i,tx_id) in tx.inputs_tx_id.withIndex()){
                clients.addresses[sender_addr]!!.lst.get(tx.inputs_tx_id[i]) ?: return false
            }

            for ((i,tx_id) in tx.inputs_tx_id.withIndex()){
                clients.addresses[sender_addr]!!.lst.remove(tx.inputs_tx_id[i])
            }

            // create output utxo's for receivers and send to the servers
            for (recv_addr in tx.outputs_address){
                // remember that we assume that the client exists
                clients.addresses[recv_addr]!!.lst[tx.tx_id] = Unit
            }

        }else{ // client is new!!
            // TODO: add client to clients - need to?????
            val genTx = Transaction(-1, listOf(-1), listOf("0"), listOf(sender_addr), listOf(100))
            ledger.ledger.add(genTx)
            clients.addresses[sender_addr] = UTxOs(sender_addr, mutableMapOf(Pair(-1,Unit)))

        }

        ledger.ledger += tx
        tx_stream += tx // add tx to buffer
        ledger.txMap.add(tx.tx_id)
        txIds.add(tx.tx_id)

        if (tx.tx_id > 5)
            nextId++

        return true

    }


    fun transferCoins(sender_address: String, receiver_address: String, amount: Long): Boolean {

        // for start, we search coins + build input list
        val input_tx_id = mutableListOf<Long>()
        val input_address = mutableListOf<String>()
        val sender_utxos : Set<Long> = getUnspentTransactions(sender_address).keys
        var coins : Long = 0
        var enougCoins = false
        for (utxo in sender_utxos){
            val tx = getTxFromLedger(ledger,utxo)
            val tempCoins = getCoinsFromTxOutput(tx,sender_address)
            coins += tempCoins
            input_tx_id.add(utxo)
            input_address.add(sender_address)
            if (coins >= amount){
                enougCoins = true
                break
            }
        }
        if (!enougCoins){
            println("NOT ENOUGH")
            // TODO: return message to client
            return false
        }
        val output_address = mutableListOf<String>(receiver_address)
        val output_coins = mutableListOf<Long>(amount)
        if (coins > amount){
            output_address.add(sender_address)
            output_coins.add(coins-amount)
        }

        // build a new tx
        // TODO: choose id
        val tx = Transaction(nextId,
            input_tx_id,input_address,
            output_address,output_coins)
        createTransaction(tx)

        txIds.add(nextId) // might be redundant
        ledger.txMap.add(nextId)
        nextId++


        return true

    }

    fun getUnspentTransactions(address: String): Map<Long,Unit>{
        return clients.addresses[address]!!.lst
    }

    // ------ for debugging ------------------------
    fun getCoins(address: String) : Long{
        val utxos = getUnspentTransactions(address).keys
        var coins : Long = 0
        for (utxo in utxos) {
            val tx = getTxFromLedger(ledger,utxo)
            val tempCoins = getCoinsFromTxOutput(tx,address)
            coins += tempCoins
        }
        return coins
    }
    // ---------------------------------------------


    fun getTransactionHistory(address: String): TransactionsLedger{
        val retLedger = TransactionsLedger(mutableListOf(), mutableSetOf())
        for (tx in ledger.ledger.reversed()){
            if (tx.inputs_address[0] == address){
                retLedger.ledger.add(0,tx)
                continue
            }
            for (recv in tx.outputs_address){
                if (recv == address){
                    retLedger.ledger.add(0,tx)
                    continue
                }
            }
        }
        return retLedger
    }

    fun getTransactionHistoryForAll(): TransactionsLedger {
        return ledger
    }

    fun getStream() : List<Transaction>{
        return tx_stream
    }

    fun addOtherShardTx(tx : Transaction){
        if (!ledger.txMap.contains(tx.tx_id)){
            ledger.ledger += tx
            ledger.txMap.add(tx.tx_id)
        }
    }
}