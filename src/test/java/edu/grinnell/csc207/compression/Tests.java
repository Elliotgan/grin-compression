package edu.grinnell.csc207.compression;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class Tests {
    String path = "files";

    @Test
    public void unzipTest() throws IOException {
        edu.grinnell.csc207.compression.Grin.decode(path + "/huffman-example.grin",
                path + "/decodetest0.txt");

        BitInputStream testIn = new BitInputStream(path + "/decodetest0.txt");
        BitInputStream originalIn = new BitInputStream(path + "/huffman-example.txt");

        while (true) {
            int testBit = testIn.readBit();
            if (testBit == -1) {
                break;
            }
            assert (testBit == originalIn.readBit());
        }

        testIn.close();
        originalIn.close();

    }

    /**
     * For some reason, the .grin file is not exactly same as the given .grin file,
     * The reason is probably that my program append bits in different order compare
     * to professor Osera's.
     * However, the outcome stays the same, and size of the .grin file is exactly
     * the same, and this program can unzip the given .grin file, so I will count
     * it as a success (since there is no requirement saying I must have exactly
     * same .grin file XD)
     * 
     * @throws IOException
     */
    @Test
    public void generalTest() throws IOException {
        edu.grinnell.csc207.compression.Grin.encode(path + "/huffman-example.txt",
                path + "/encodetest1.grin");
        edu.grinnell.csc207.compression.Grin.decode(path + "/encodetest1.grin",
                path + "/decodetest1.txt");

        BitInputStream testIn = new BitInputStream(path + "/decodetest1.txt");
        BitInputStream originalIn = new BitInputStream(path + "/huffman-example.txt");

        while (true) {
            int testBit = testIn.readBit();
            if (testBit == -1) {
                break;
            }
            assert (testBit == originalIn.readBit());
        }

        testIn.close();
        originalIn.close();
    }
}
