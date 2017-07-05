import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     * values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {

        List<UTXO> seenUxs = new ArrayList<UTXO>();

        int index = 0;

        double inSum = 0;
        double outSum = 0;

        for (Transaction.Input input : tx.getInputs()) {
            UTXO checkUx = new UTXO(input.prevTxHash, input.outputIndex);
            // no UTXO is claimed multiple times
            if (seenUxs.contains(checkUx)) return false;

            seenUxs.add(checkUx);

            // all output claimed by tx are in the current UTXO pool
            if (!utxoPool.contains(checkUx)) return false;

            // the signatures on each input are valid.
            if (!Crypto.verifySignature(utxoPool.getTxOutput(checkUx).address, tx.getRawDataToSign(index), input.signature)) return false;

            inSum += utxoPool.getTxOutput(checkUx).value;

            index++;

        }

        for (Transaction.Output output : tx.getOutputs()) {
            if (output.value < 0) return false;
            outSum += output.value;
        }

        if (inSum < outSum) return false;

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {

        Set<Transaction> validTx = new HashSet<Transaction>();

        for (Transaction tx : possibleTxs) {
            int index = 0;
            if (isValidTx(tx)) {
                validTx.add(tx);
                for (Transaction.Input input : tx.getInputs()) {
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    utxoPool.removeUTXO(utxo);
                }
                for (Transaction.Output output : tx.getOutputs()) {
                    UTXO utxo = new UTXO(tx.getHash(), index);
                    utxoPool.addUTXO(utxo, output);
                    index++;
                }
            }
        }

        Transaction[] validTxArray = new Transaction[validTx.size()];
        return validTx.toArray(validTxArray);
    }

}
