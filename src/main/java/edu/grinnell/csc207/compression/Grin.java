package edu.grinnell.csc207.compression;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The driver for the Grin compression program.
 */
public class Grin {
    /**
     * Decodes the .grin file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     * 
     * @param infile  the file to decode
     * @param outfile the file to ouptut to
     */
    public static void decode(String infile, String outfile)
            throws IOException, IllegalArgumentException {
        BitInputStream in = null;
        try {
            in = new BitInputStream(infile);
        } catch (IOException e) {
            throw new IOException("Invalid infile name");
        }

        BitOutputStream out = null;
        try {
            out = new BitOutputStream(outfile);
        } catch (IOException e) {
            throw new IOException("Invalid outfile name");
        }

        if (in.readBits(32) != 0x736) {
            throw new IllegalArgumentException("Invalid file");
        }
        HuffmanTree tree = new HuffmanTree(in);
        // System.out.println("Decode tree:");
        // tree.printTree();

        tree.decode(in, out);
        in.close();
        out.close();

    }

    /**
     * Creates a mapping from 8-bit sequences to number-of-occurrences of
     * those sequences in the given file. To do this, read the file using a
     * BitInputStream, consuming 8 bits at a time.
     * 
     * @param file the file to read
     * @return a freqency map for the given file
     */
    public static Map<Short, Integer> createFrequencyMap(String file) throws IOException {
        BitInputStream in = null;
        try {
            in = new BitInputStream(file);
        } catch (IOException e) {
            throw new IOException("Invalid infile name");
        }
        Map<Short, Integer> freqMap = new HashMap<Short, Integer>();
        for (int readChar = in.readBits(8); readChar != -1; readChar = in.readBits(8)) {
            short key = (short) readChar;
            if (freqMap.containsKey(key)) {
                int freq = freqMap.get(key) + 1;
                freqMap.put(key, freq);
            } else {
                freqMap.put(key, 1);
            }
        }
        return freqMap;
    }

    /**
     * Encodes the given file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     * 
     * @param infile  the file to encode.
     * @param outfile the file to write the output to.
     */
    public static void encode(String infile, String outfile) throws IOException {
        BitInputStream in = null;
        try {
            in = new BitInputStream(infile);
        } catch (IOException e) {
            throw new IOException("Invalid infile name");
        }

        BitOutputStream out = null;
        try {
            out = new BitOutputStream(outfile);
        } catch (IOException e) {
            throw new IOException("Invalid outfile name");
        }

        Map<Short, Integer> freqMap = createFrequencyMap(infile);
        HuffmanTree tree = new HuffmanTree(freqMap);
        // System.out.println("Encode tree:");
        // tree.printTree();

        out.writeBits(0x736, 32);
        tree.serialize(out);
        tree.encode(in, out);
        in.close();
        out.close();
    }

    /**
     * The entry point to the program.
     * 
     * @param args the command-line arguments.
     */
    public static void main(String[] args) throws IOException {

        if (args.length != 3) {
            System.out.println("Usage: java Grin <encode|decode> <infile> <outfile>");
            return;
        }
        String infile = args[1];
        String outfile = args[2];
        String operation = args[0];

        if (operation.equals("encode")) {
            try {
                encode(infile, outfile);
            } catch (IOException e) {
                System.out.println("Invalid file!");
                return;
            }
        } else if (operation.equals("decode")) {
            try {
                decode(infile, outfile);
            } catch (IOException e) {
                System.out.println("Invalid file!");
                return;
            } catch (IllegalArgumentException e) {
                System.out.println("infile is not valid to be decoded!");
                return;
            }

        } else {
            System.out.println("Usage: java Grin <encode|decode> <infile> <outfile>");
            return;
        }

        /*
         * String originp = "D:/testtexts/test1o.txt";
         * String encodep = "D:/testtexts/test1e.txt";
         * String decodep = "D:/testtexts/test1d.txt";
         * 
         * encode(originp, encodep);
         * decode(encodep, decodep);
         */
    }
}
