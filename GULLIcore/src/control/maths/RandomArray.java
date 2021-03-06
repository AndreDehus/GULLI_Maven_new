/*
 * The MIT License
 *
 * Copyright 2019 saemann.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package control.maths;

import java.util.Random;

/**
 * Returns uniform and normal distributed numbers, that have been
 * pre-initialized and do not need to be generated for every call.
 *
 * @author saemann
 */
public class RandomArray {

    private double[] gaussians = null, uniform = null;
    private int index = 0;
    private int indexU = 0;
//    private int indexRND = 0;
    private boolean initialized = false;

    /**
     * if true the random number will always be generated new. if false the
     * apriori filled array is used. The array version can have reoccuring
     * values, that causes non-uniform distributed results (when using a high
     * number of simulation loops. Array version is much faster as the
     * random-algorithm is very complex.
     */
    public static boolean alwaysGenerateNew = true;
    private final long seed;
    private Random r;
    public static int numberOfGaussLoops = 0;
    public static int numberOfDoubleLoops = 0;
    public final int cachesize;

    public RandomArray(long seed, int numberOfValues) {
        cachesize = numberOfValues;
        this.seed = seed;
        r = new Random(seed);
        if (!alwaysGenerateNew) {

            gaussians = new double[cachesize];
            uniform = new double[cachesize];
            for (int i = 0; i < gaussians.length; i++) {
                gaussians[i] = r.nextGaussian();
            }
            for (int i = 0; i < uniform.length; i++) {
                uniform[i] = r.nextDouble();
            }
            initialized = true;
             r = new Random(seed);
        }
       
        index = 0;
        indexU = 0;
//        indexRND = 0;
    }

    private void initRandomArrays(int numberOfValues) {

        r = new Random(seed);
        gaussians = new double[cachesize];
        uniform = new double[cachesize];
        for (int i = 0; i < gaussians.length; i++) {
            gaussians[i] = r.nextGaussian();
        }
        for (int i = 0; i < uniform.length; i++) {
            uniform[i] = r.nextDouble();
        }
        initialized = true;

        index = 0;
        indexU = 0;
//        indexRND = 0;
        initialized = true;
    }

    public double nextGaussian() {
        if (alwaysGenerateNew) {
            return r.nextGaussian();
        } else if (!initialized) {
            initRandomArrays(cachesize);
        }
        index++;
        if (index >= gaussians.length) {
            index = 0;
            numberOfGaussLoops++;
        }

        return gaussians[index];
    }

    public double nextDouble() {
        if (alwaysGenerateNew) {
            return r.nextDouble();
        } else if (!initialized) {
            initRandomArrays(cachesize);
        }
        indexU++;
        if (indexU >= uniform.length) {
            indexU = 0;
            numberOfDoubleLoops++;
        }

        return uniform[indexU];
    }

    public void resetIndex() {
        this.index = 0;
        this.indexU = 0;
//        this.indexRND = 0;
        numberOfDoubleLoops = 0;
        numberOfGaussLoops = 0;
    }

    public void reset() {
        if (alwaysGenerateNew) {
            r = new Random(seed);
        }
        resetIndex();
    }

    public int getIndex() {
        return index;
    }

    public boolean hasEqualValues(RandomArray other) {
        boolean equal = true;
        for (int i = 0; i < uniform.length; i++) {
            if (uniform[i] != other.uniform[i]) {
                equal = false;
//                System.out.println("Entry uniform[" + i + "] is not equal :" + this.uniform[i] + " != " + other.uniform[i]);
            }
        }
        return equal;
    }

    public long getSeed() {
        return seed;
    }

    public int getCachesize() {
        return cachesize;
    }

}
