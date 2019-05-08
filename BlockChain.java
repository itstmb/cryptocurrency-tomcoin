// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory
// as it would cause a memory overflow.

import java.util.ArrayList;
import java.util.HashMap;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

    private TransactionPool txPool;
    private HashMap<ByteArrayWrapper, Node> blockChain;
    private Node genesis_n;


    /** create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid block */
    public BlockChain(Block genesisBlock) {
        blockChain = new HashMap<>();
        UTXOPool UTXO_Pool = new UTXOPool();
        addCoin_UTXOPool(genesisBlock, UTXO_Pool);
        Node genesisNode = new Node(genesisBlock, null, UTXO_Pool);
        blockChain.put(new ByteArrayWrapper(genesisBlock.getHash()), genesisNode);
        txPool = new TransactionPool();
        genesis_n = genesisNode;
    }

    private class Node {
        public Block b;
        public Node parent_node;
        public int height;
        public ArrayList<Node> child_nodes;
        private UTXOPool uPool;

        public Node(Block b, Node parent_node, UTXOPool uPool) {
            this.b = b;
            this.parent_node = parent_node;
            this.uPool = uPool;
            child_nodes = new ArrayList<>();
            if (parent_node != null) {
                height = parent_node.height + 1;
                parent_node.child_nodes.add(this);
            } else {
                height = 1;
            }
        }

        public UTXOPool getUTXOPool_() {
            return new UTXOPool(uPool);
        }
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        return genesis_n.b;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        return genesis_n.getUTXOPool_();
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return txPool;
    }

    /**
    * Add { @code block} to the block chain if it is valid. For validity, all transactions
    * should be valid and block should be at { @code height > (maxHeight - CUT_OFF_AGE)}.
    * For example, you can try creating a new block over the genesis block (block height 2)
    * if the block chain height is { @code <= CUT_OFF_AGE + 1}. As soon as { @code height >
    * CUT_OFF_AGE + 1}, you cannot create a new block at height 2.
    * @return true if block is successfully added
    */
    public boolean addBlock(Block block) {
        byte[] prevBlockHash = block.getPrevBlockHash();
        if (prevBlockHash == null)
            return false;
        Node parent_Node = blockChain.get(new ByteArrayWrapper(prevBlockHash));
        if (parent_Node == null) {
            return false;
        }
        TxHandler handler = new TxHandler(parent_Node.getUTXOPool_());
        Transaction[] txs = block.getTransactions().toArray(new Transaction[0]);
        Transaction[] validTxs = handler.handleTxs(txs);
        if (validTxs.length != txs.length) {
            return false;
        }
        int proposedHeight = parent_Node.height + 1;
        if (proposedHeight <= genesis_n.height - CUT_OFF_AGE) {
            return false;
        }
        UTXOPool UTXO_Pool = handler.getUTXOPool();
        addCoin_UTXOPool(block, UTXO_Pool);
        Node node = new Node(block, parent_Node, UTXO_Pool);
        blockChain.put(new ByteArrayWrapper(block.getHash()), node);
        if (proposedHeight > genesis_n.height) {
            genesis_n = node;
        }
        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        txPool.addTransaction(tx);
    }

    private void addCoin_UTXOPool(Block block, UTXOPool UTXO_Pool) {
        Transaction coinbase = block.getCoinbase();
        for (int i = 0; i < coinbase.numOutputs(); i++) {
            Transaction.Output out = coinbase.getOutput(i);
            UTXO utxo = new UTXO(coinbase.getHash(), i);
            UTXO_Pool.addUTXO(utxo, out);
        }
    }
}
