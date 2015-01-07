
/************************************************************************************
 * @file LinHashMap.java
 *
 * @author  John Miller
 */

import java.io.*;
import java.lang.reflect.Array;

import static java.lang.System.out;

import java.util.*;
import java.util.stream.Collectors;

/************************************************************************************
 * This class provides hash maps that use the Linear Hashing algorithm.
 * A hash table is created that is an array of buckets.
 */
public class LinHashMap <K, V>
       extends AbstractMap <K, V>
       implements Serializable, Cloneable, Map <K, V>
{
    /**
	 * 
	 */
	private static final long serialVersionUID = -3049021800145590937L;

	/** The number of slots (for key-value pairs) per bucket.
     */
    private static final int SLOTS = 4;

    /** The class for type K.
     */
    private final Class <K> classK;

    /** The class for type V.
     */
    private final Class <V> classV;

    /********************************************************************************
     * This inner class defines buckets that are stored in the hash table.
     */
    private class Bucket implements Serializable
    {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1339961090644713622L;
		
		int    nKeys;
        K []   key;
        V []   value;
        Bucket next;
        @SuppressWarnings("unchecked")
        Bucket (Bucket n)
        {
            nKeys = 0;
            key   = (K []) Array.newInstance (classK, SLOTS);
            value = (V []) Array.newInstance (classV, SLOTS);
            next  = n;
        } // constructor
    } // Bucket inner class
    
    /********************************************************************************
     * This class defines a wrapper to create simple key-pair entries.
     */
    @SuppressWarnings("hiding")
    /***
     * 
     * @author Germanno Domingues
     *
     * @param <K> Type of key.
     * @param <V> Type of value.
     */
	private final class CustomEntry<K, V> implements Map.Entry<K, V> {
    	
        private final K mKey;
        private V mValue;
        
        public CustomEntry(K key, V value) {
            mKey = key;
            mValue = value;
        }
        
        @Override
        public K getKey() {
            return mKey;
        }
        
        @Override
        public V getValue() {
            return mValue;
        }
        
        @Override
        public V setValue(V value) {
            V old = mValue;
            mValue = value;
            return old;
        }
    } //CustomEntry inner class
    
    /** The list of buckets making up the hash table.
     */
    private final List <Bucket> hTable;

    /** The modulus for low resolution hashing
     */
    private int mod1;

    /** The modulus for high resolution hashing
     */
    private int mod2;

    /** Counter for the number buckets accessed (for performance testing).
     */
    private int count = 0;

    /** The index of the next bucket to split.
     */
    private int split = 0;

    /********************************************************************************
     * Construct a hash table that uses Linear Hashing.
     * @param classK    the class for keys (K)
     * @param classV    the class for keys (V)
     * @param initSize  the initial number of home buckets (a power of 2, e.g., 4)
     */
    public LinHashMap (Class <K> _classK, Class <V> _classV, int initSize)
    {
        classK = _classK;
        classV = _classV;
        hTable = new ArrayList <> ();
        mod1   = initSize;
        mod2   = 2 * mod1;
    } // constructor

    /********************************************************************************
     * Return a set containing all the entries as pairs of keys and values.
     * @return  the set view of the map
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public Set <Map.Entry <K, V>> entrySet ()
    {
        Set <Map.Entry <K, V>> enSet = new HashSet <> ();

        //  T O   B E   I M P L E M E N T E D
        
        for (Bucket b : hTable)
        {
        	if (b != null)
        	{
        		enSet.add(new CustomEntry(b.key, b.value));	
        	}
        }
            
        return enSet;
    } // entrySet

    /********************************************************************************
     * Given the key, look up the value in the hash table.
     * @param key  the key used for look up
     * @return  the value associated with the key
     * 
     * @author Germanno Domingues
     */
    public V get (Object key)
    {
        int i = h (key);

        //  T O   B E   I M P L E M E N T E D
        
        if (i < split)
        {
        	i = h2(key);
        }
        
        i = Math.max(0, i);
        
        Bucket b = hTable.get(i);
        
        while (b != null)
        {
        	for (int j = 0; j < b.nKeys; j++) {
				
        		if (b.key[j].hashCode() == key.hashCode())
        		{
        			return b.value[j];
        		}
			}
        	
        	b = b.next;
        }

        return null;
    } // get

    /********************************************************************************
     * Put the key-value pair in the hash table.
     * @param key    the key to insert
     * @param value  the value to insert
     * @return  null (not the previous value)
     * 
     * @author Germanno Domingues
     */
    public V put (K key, V value)
    {
        int i = h (key);

        //  T O   B E   I M P L E M E N T E D
        
        if (i < split)
        {
        	i = h2 (key);
        }
        
        i = Math.max(0, i);
        
        /***
         * Reference to the first bucket in the chain.
         */        
        while (hTable.size() <= i)
        {
        	hTable.add(new Bucket(null));
        }
        
        Bucket b = hTable.get(i);
        count++;
        
        /***
         * Reference to the last bucket in the chain.
         */
        Bucket last = b;
        
        if (b.nKeys == SLOTS)
        {        	
        	/**
        	 * Gets the last bucket of chain.
        	 */
        	while (last.next != null)
        	{
        		last = last.next;
        		count++;
        	}
        	
        	/**
        	 * Adds an extra bucket if the last is full.
        	 */
        	if (last.nKeys == SLOTS)
        	{
        		last.next = new Bucket(null);
        		last = last.next;
        		count++;
        		
        		doSplit(b, i);
        		
            	/***
            	 * Replace the current splitted bucket with an empty bucket.
            	 */
            	hTable.set(i, new Bucket(null));
            	
            	/***
            	 * Reassign the items of the splitted bucket to new positions.
            	 */
            	reassign(b, i);
        	}
        }
        
		last.key[last.nKeys] = key;
		last.value[last.nKeys] = value;
		last.nKeys++;

        return null;
    } // put
    
    /***
     * Splits a given bucket.
     * 
     * @author Germanno Domingues
     * 
     * @param b Bucket to be splitted.
     * @param bIndex Index of the bucket in the Hash Table.
     */
    public void doSplit(Bucket b, int bIndex)
    {
    	split++;
    	
    	/***
    	 * If all the buckets where splitted, enlarge the hash table.
    	 */
    	if (split == mod1)
    	{
    		mod1 = mod2;
    		mod2 *= 2;
    		
    		while (hTable.size() < mod2)
    		{
    			hTable.add(new Bucket(null));
    		}
    		
    		split = 0;
    	}
    }
    
    /***
     * Reassign all the key-pair values of this bucket and its chain
     * after a split is being made.
     * 
     * @author Germanno Domingues
     * 
     * @param b
     * @param bIndex
     */
    public void reassign(Bucket b, int bIndex)
    {
    	while (b != null)
    	{
        	int newIndex = 0;
        	
        	for (int i = 0; i < b.nKeys; i++)
        	{
        		newIndex = h(b.key[i]);
        		
        		put(b.key[i], b.value[i]);
        	}
        	
    		b = b.next;
    	}
    }

    /********************************************************************************
     * Return the size (SLOTS * number of home buckets) of the hash table. 
     * @return  the size of the hash table
     */
    public int size ()
    {
        return SLOTS * (mod1 + split);
    } // size

    /********************************************************************************
     * Print the hash table.
     */
    private void print ()
    {
        out.println ("Hash Table (Linear Hashing)");
        out.println ("-------------------------------------------");

        //  T O   B E   I M P L E M E N T E D
        
        Bucket b = null;
        
        for (int i = 0; i < hTable.size(); i++) {
			
        	out.println("**** BUCKET " + i + " ****");
        	
        	b = hTable.get(i);
        	
        	while (b != null)
        	{
        		for (int j = 0; j < b.nKeys; j++) {
					out.print(" | key=" + b.key[j] + ", value=" + b.value[j] + (j == b.nKeys - 1 ? " |" : ""));
				}
        		
        		b = b.next;
        	}
        	
        	out.println();
        	out.println();
		}

        out.println ("-------------------------------------------");
    } // print

    /********************************************************************************
     * Hash the key using the low resolution hash function.
     * @param key  the key to hash
     * @return  the location of the bucket chain containing the key-value pair
     */
    private int h (Object key)
    {
        return key.hashCode () % mod1;
    } // h

    /********************************************************************************
     * Hash the key using the high resolution hash function.
     * @param key  the key to hash
     * @return  the location of the bucket chain containing the key-value pair
     */
    private int h2 (Object key)
    {
        return key.hashCode () % mod2;
    } // h2

    /********************************************************************************
     * The main method used for testing.
     * @param  the command-line arguments (args [0] gives number of keys to insert)
     */
    public static void main (String [] args)
    {
        LinHashMap <Integer, Integer> ht = new LinHashMap <> (Integer.class, Integer.class, 11);
        int nKeys = 30;
        if (args.length == 1) nKeys = Integer.valueOf (args [0]);
        for (int i = 1; i < nKeys; i += 2) ht.put (i, i * i);
        ht.print ();
        for (int i = 0; i < nKeys; i++) {
            out.println ("key = " + i + " value = " + ht.get (i));
        } // for
        out.println ("-------------------------------------------");
        out.println ("Average number of buckets accessed = " + ht.count / (double) nKeys);
    } // main

} // LinHashMap class

