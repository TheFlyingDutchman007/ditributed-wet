package rest_api.repository.model

data class TransactionsLedger (
    var ledger : MutableList<Transaction>,
    var txMap : MutableSet<Long>
        )