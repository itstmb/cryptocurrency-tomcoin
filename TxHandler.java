/**
 * Submitted by: Tom M. Breier
 * ID: 315699850
 * Bitcoin Blockchain Course - Homework Assignment 1
 */

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
public class TxHandler {

    private UTXOPool ledger;
    private double inputSum, outputSum;

    /**
     * Constructor
     * @param utxoPool
     */
    public TxHandler(UTXOPool utxoPool) {
        this.ledger = new UTXOPool(utxoPool);
        this.inputSum = 0;
        this.outputSum = 0;
    }

    /**
     * Transaction Validator
     * @param tx - Transaction
     * @return whether tx is valid according to the given rules
     */
    public boolean isValidTx(Transaction tx) {
        if (tx == null) // Could tx be a null?
            return false;

        this.inputSum = 0; this.outputSum = 0; // Variables reset

        HashMap<UTXO, Boolean> usedUTXO = new HashMap<>();
        /* States whether a UTXO has been used yet */

        for (int i = 0;  i < tx.numInputs(); i++) { // Iterate over transactions
            Transaction.Input trans_input = tx.getInput(i);
            if (trans_input == null)
                return false;

            UTXO utxo = new UTXO(trans_input.prevTxHash, trans_input.outputIndex);

            /* Rule 1 */
            if (this.ledger.contains(utxo) == false)
                return false;

            Transaction.Output previousTxOutput = this.ledger.getTxOutput(utxo);
            if (previousTxOutput == null) { return false; }

            /* Rule 2 */
            PublicKey pubKey = previousTxOutput.address;
            byte[] message = tx.getRawDataToSign(i);
            byte[] signature = trans_input.signature;
            if (Crypto.verifySignature(pubKey, message, signature) == false)
                return false;

            /* Rule 3 */
            if (usedUTXO.containsKey(utxo))
                return false;
            usedUTXO.put(utxo, true);

            /* Rule 5 (Partially) */
            this.inputSum += previousTxOutput.value;
        }

        /* Rule 4 */
        for (int i = 0;  i < tx.numOutputs(); i++) {
            Transaction.Output trans_output = tx.getOutput(i);
            if (trans_output == null)
                return false;
            if (trans_output.value < 0)
                return false;

            outputSum += trans_output.value;
        }

        /* Rule 5 */
        return this.inputSum >= outputSum;
    }

    /**
    * Transaction handler
     * @param possibleTxs - Array of unchecked transactions
     * @return Array of valid transactions
    */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        int index;
        if (possibleTxs == null) {
            return new Transaction[0]; // Null handler
        }

        ArrayList<Transaction> valid_Transactions = new ArrayList<>();

        for (Transaction tx : possibleTxs) {
            index = 0;
            if (!isValidTx(tx))
                continue; // Disallows invalid transactions

            valid_Transactions.add(tx);

            for (Transaction.Input trans_input : tx.getInputs()) {
                UTXO utxo = new UTXO(trans_input.prevTxHash, trans_input.outputIndex);
                this.ledger.removeUTXO(utxo);
            }
            byte[] txHash = tx.getHash();
            for (Transaction.Output trans_output : tx.getOutputs()) {
                UTXO utxo = new UTXO(txHash, index);
                index ++;
                this.ledger.addUTXO(utxo, trans_output);
            }
        }

        return valid_Transactions.toArray(new Transaction[valid_Transactions.size()]);
    }

    public UTXOPool getUTXOPool() {
        return this.ledger();
    }
}
