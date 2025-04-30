package edu.grinnell.csc207.compression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

/**
 * A HuffmanTree derives a space-efficient coding of a collection of byte
 * values.
 *
 * The huffman tree encodes values in the range 0--255 which would normally
 * take 8 bits. However, we also need to encode a special EOF character to
 * denote the end of a .grin file. Thus, we need 9 bits to store each
 * byte value. This is fine for file writing (modulo the need to write in
 * byte chunks to the file), but Java does not have a 9-bit data type.
 * Instead, we use the next larger primitive integral type, short, to store
 * our byte values.
 */
public class HuffmanTree {

    /**
     * A node which can either be a InnerNode or a Leaf
     */
    private abstract class Node {
        protected int freq;

        public int getFrequency() {
            return freq;
        }

        public abstract boolean isLeaf();
    }

    /**
     * A InnerNode, which have two nodes connect to it
     */
    private class InnerNode extends Node {
        private Node lhs;
        private Node rhs;

        public InnerNode(int freq) {
            this.lhs = null;
            this.rhs = null;
            this.freq = freq;
        }

        public InnerNode(int freq, Node lhs, Node rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
            this.freq = freq;
        }

        public Node getLhs() {
            return this.lhs;
        }

        public Node getRhs() {
            return this.rhs;
        }

        public boolean isLeaf() {
            return false;
        }
    }

    /**
     * A Leaf, no node append to it, it stores a char in the form of short
     */
    private class Leaf extends Node {
        private short charStored;

        public Leaf(short charStored, int freq) {
            this.freq = freq;
            this.charStored = charStored;
        }

        public boolean isLeaf() {
            return true;
        }

        public short getChar() {
            return this.charStored;
        }
    }

    /**
     * A queue of nodes
     */
    private class NodeQueue {
        private List<Node> queue;

        public NodeQueue(Map<Short, Integer> freqs) {
            this.queue = new ArrayList<Node>();
            Set<Short> keys = freqs.keySet();
            for (Short cur : keys) {
                insert(new Leaf(cur, freqs.get(cur)));
            }
        }

        public Node pop() {
            Node ret = this.queue.get(0);
            this.queue.remove(0);
            return ret;
        }

        public void insert(Node insertIt) {
            int freqInsert = insertIt.getFrequency();
            for (int iter = 0; iter < queue.size(); iter++) {
                if (freqInsert < queue.get(iter).getFrequency()) {
                    queue.add(iter, insertIt);
                    return;
                }
            }
            queue.add(insertIt);
        }

        public int getsize() {
            return queue.size();
        }

    }

    /**
     * Append two input nodes, return a innerNode with frequency equal to sum of
     * frequencies of two input nodes
     * @param lhs
     * @param rhs
     * @return a new innerNode with frequency equal to sum of frequencies of two input nodes 
     * which contains two input nodes
     */
    public Node append(Node lhs, Node rhs) {
        Node ret = new InnerNode(lhs.getFrequency() + rhs.getFrequency(), lhs, rhs);
        return ret;
    }

    private Node root;

    /**
     * Constructs a new HuffmanTree from a frequency map.
     * 
     * @param freqs a map from 9-bit values to frequencies.
     */
    public HuffmanTree(Map<Short, Integer> freqs) {
        NodeQueue treeQueue = new NodeQueue(freqs);
        treeQueue.insert(new Leaf((short) 0x100, 1));
        while (treeQueue.getsize() > 1) {
            Node node1 = treeQueue.pop();
            Node node2 = treeQueue.pop();
            Node insertNode = append(node1, node2);
            treeQueue.insert(insertNode);
        }
        this.root = treeQueue.pop();
    }

    /**
     * Helper procedure for in stream constructor
     * @param in
     * @param curNode
     */
    private void constructH(BitInputStream in, Node curNode) {
        // curNode should never be a leaf
        if (curNode.isLeaf()) {
            return;
        }

        if (in.readBit() == 1) {
            Node leftNode = new InnerNode(-1);
            ((InnerNode) curNode).lhs = leftNode;
            constructH(in, ((InnerNode) curNode).getLhs());
        } else {
            Node leftNode = new Leaf((short) in.readBits(9), -1);
            ((InnerNode) curNode).lhs = leftNode;
        }

        if (in.readBit() == 1) {
            Node rightNode = new InnerNode(-1);
            ((InnerNode) curNode).rhs = rightNode;
            constructH(in, ((InnerNode) curNode).getRhs());
        } else {
            Node rightNode = new Leaf((short) in.readBits(9), -1);
            ((InnerNode) curNode).rhs = rightNode;
        }
    }

    /**
     * Constructs a new HuffmanTree from the given file.
     * 
     * @param in the input file (as a BitInputStream)
     */
    public HuffmanTree(BitInputStream in) {
        if (in.readBit() == 0) {
            Short leafVal = (short) in.readBits(9);
            this.root = new Leaf(leafVal, -1);
        } else {
            this.root = new InnerNode(-1);
            constructH(in, root);
        }
    }

    /**
     * Writes this HuffmanTree to the given file as a stream of bits in a
     * serialized format.
     * 
     * @param out the output file as a BitOutputStream
     */
    public void serialize(BitOutputStream out) {
        serializeH(root, out);
    }

    /**
     * Helper procedure for serialize
     * @param curNode
     * @param out
     */
    private void serializeH(Node curNode, BitOutputStream out) {
        if (curNode.isLeaf()) {
            out.writeBit(0);
            out.writeBits(((Leaf) curNode).getChar(), 9);
            return;
        }
        out.writeBit(1);
        serializeH(((InnerNode) curNode).getLhs(), out);
        serializeH(((InnerNode) curNode).getRhs(), out);
    }

    /**
     * Generate a map based on current tree
     * @return the map that maps every char to a list of booleans,
     * true represents 1 and false represents 0
     */
    public Map<Short, List<Boolean>> convertMap() {
        Map<Short, List<Boolean>> ret = new HashMap<Short, List<Boolean>>();
        List<Boolean> bits = new ArrayList<>();
        convertMapH(ret, bits, root);
        return ret;
    }

    /**
     * Helper procedure for convertMap
     * @param map
     * @param currentBits
     * @param curNode
     */
    private void convertMapH(Map<Short, List<Boolean>> map,
            List<Boolean> currentBits, Node curNode) {
        if (curNode.isLeaf()) {
            map.put(((Leaf) curNode).getChar(), currentBits);
            return;
        }
        List<Boolean> curBitsLeft = new ArrayList<>(currentBits);
        curBitsLeft.add(false);
        List<Boolean> curBitsRight = new ArrayList<>(currentBits);
        curBitsRight.add(true);
        convertMapH(map, curBitsLeft, ((InnerNode) curNode).getLhs());
        convertMapH(map, curBitsRight, ((InnerNode) curNode).getRhs());
    }

    /**
     * Encodes the file given as a stream of bits into a compressed format
     * using this Huffman tree. The encoded values are written, bit-by-bit
     * to the given BitOuputStream.
     * 
     * @param in  the file to compress.
     * @param out the file to write the compressed output to.
     */
    public void encode(BitInputStream in, BitOutputStream out) {
        Map<Short, List<Boolean>> bitsMap = convertMap();
        for (int readChar = in.readBits(8); readChar != -1; readChar = in.readBits(8)) {
            Short curChar = (short) readChar;
            List<Boolean> curBits = bitsMap.get(curChar);
            for (int iter = 0; iter < curBits.size(); iter++) {
                if (curBits.get(iter)) {
                    out.writeBit(1);
                } else {
                    out.writeBit(0);
                }
            }
        }

        List<Boolean> eofBits = bitsMap.get((short) 0x100);
        for (int iter = 0; iter < eofBits.size(); iter++) {
            if (eofBits.get(iter)) {
                out.writeBit(1);
            } else {
                out.writeBit(0);
            }
        }

    }

    /**
     * print the tree, false represents go left, true represents go right
     */
    public void printTree() {
        Map<Short, List<Boolean>> bitsMap = convertMap();
        Set<Short> keys = bitsMap.keySet();
        for (Short cur : keys) {
            System.out.println("current char is: " + (char) cur.shortValue()
                    + " the bits it have is: "
                    + Arrays.toString((bitsMap.get(cur).toArray())));
        }
    }

    /**
     * Decodes a stream of huffman codes from a file given as a stream of
     * bits into their uncompressed form, saving the results to the given
     * output stream. Note that the EOF character is not written to out
     * because it is not a valid 8-bit chunk (it is 9 bits).
     * 
     * @param in  the file to decompress.
     * @param out the file to write the decompressed output to.
     */
    public void decode(BitInputStream in, BitOutputStream out) throws IllegalArgumentException {
        int bit = 0;
        bit = in.readBit();
        while (bit != -1) {
            Node curNode = root;
            for (; bit != -1;) {
                if (curNode.isLeaf()) {
                    if (((Leaf) curNode).getChar() == (short) 0x100) {
                        return;
                    }
                    out.writeBits(((Leaf) curNode).getChar(), 8);
                    break;
                } else {
                    if (bit == 1) {
                        curNode = ((InnerNode) curNode).getRhs();
                    } else {
                        curNode = ((InnerNode) curNode).getLhs();
                    }
                    bit = in.readBit();
                }
            }
        }
    }
}
