
/************************************************************************************
 * @file BpTreeMap.java
 *
 * @author  John Miller
 */

import java.io.*;
import java.lang.reflect.Array;

import static java.lang.System.out;

import java.util.*;




/************************************************************************************
 * This class provides B+Tree maps.  B+Trees are used as multi-level index structures
 * that provide efficient access for both point queries and range queries.
 */
public class BpTreeMap <K extends Comparable <K>, V>
       extends AbstractMap <K, V>
       implements Serializable, Cloneable, SortedMap <K, V>
{
    /** The maximum fanout for a B+Tree node.
     */
    private static final int ORDER = 5;

    /** The class for type K.
     */
    private final Class <K> classK;

    /** The class for type V.
     */
    private final Class <V> classV;

    /********************************************************************************
     * This inner class defines nodes that are stored in the B+tree map.
     */
    private class Node
    {
        boolean   isLeaf;
        int       nKeys;
        K []      key;
        Object [] ref;
        @SuppressWarnings("unchecked")
        Node (boolean _isLeaf)
        {
            isLeaf = _isLeaf;
            nKeys  = 0;
            key    = (K []) Array.newInstance (classK, ORDER - 1);
            if (isLeaf) {
                //ref = (V []) Array.newInstance (classV, ORDER);
                ref = new Object [ORDER];
            } else {
                ref = (Node []) Array.newInstance (Node.class, ORDER);
            } // if
        } // constructor
    } // Node inner class


    /** The root of the B+Tree
     */
    private Node root;

    /** The counter for the number nodes accessed (for performance testing).
     */
    private int count = 0;

    private TreeMap<K,V> map;
    /********************************************************************************
     * Construct an empty B+Tree map.
     * @param _classK  the class for keys (K)
     * @param _classV  the class for values (V)
     */
    public BpTreeMap (Class <K> _classK, Class <V> _classV)
    {
        classK = _classK;
        classV = _classV;
        root   = new Node (true);
        map = new TreeMap<>();
    } // constructor

    /********************************************************************************
     * Return null to use the natural order based on the key type.  This requires the
     * key type to implement Comparable.
     */
    public Comparator <? super K> comparator () 
    {
        return null;
    } // comparator

    /********************************************************************************
     * Return a set containing all the entries as pairs of keys and values.
     * @return  the set view of the map
     * @author Michael Tan
     */
    public Set <Map.Entry <K, V>> entrySet ()
    {
    	   Set <Map.Entry <K, V>> enSet = new HashSet <> ();
           
           enSet = map.entrySet();    

           return enSet;
    } // entrySet

    /********************************************************************************
     * Given the key, look up the value in the B+Tree map.
     * @param key  the key used for look up
     * @return  the value associated with the key
     */
    @SuppressWarnings("unchecked")
    public V get (Object key)
    {
        return find ((K) key, root);
    } // get

    /********************************************************************************
     * Put the key-value pair in the B+Tree map.
     * @param key    the key to insert
     * @param value  the value to insert
     * @return  null (not the previous value)
     */
    public V put (K key, V value)
    {
        insert (key, value, root, null);
        
        return null;
    } // put

    /********************************************************************************
     * Return the first (smallest) key in the B+Tree map.
     * @return  the first key in the B+Tree map.
     */
    public K firstKey () 
    {

        Node curr = root;
        while(!curr.isLeaf && curr.nKeys >= 1)
        	curr = (Node)curr.ref[0];
        
        return curr.key[0];

    }

    /********************************************************************************
     * Return the last (largest) key in the B+Tree map.
     * @return  the last key in the B+Tree map.
     */
    public K lastKey () 
    {
        //  T O   B E   I M P L E M E N T E D

        Node curr = root;
        while(!curr.isLeaf && curr.nKeys >= 1)
             curr = (Node) curr.ref[curr.nKeys];
        
        return curr.key[curr.nKeys-1];
    } // lastKey

    /********************************************************************************
     * Return the portion of the B+Tree map where key < toKey.
     * @return  the submap with keys in the range [firstKey, toKey)
     */
    public SortedMap <K,V> headMap (K toKey)
    {
        //  T O   B E   I M P L E M E N T E D
        SortedMap<K,V> map = new TreeMap<K,V>();
        Node curr = root;
        while(!curr.isLeaf && curr.nKeys > 0)  
        	curr = (Node) curr.ref[curr.nKeys];
        
        do
        {
            for(int i = 0; i < curr.nKeys; i++)
            {
                if(curr.key[i].compareTo(toKey) < 0)
                	map.put(curr.key[i], (V) curr.ref[i+1]);
            }
            curr = (Node) curr.ref[0]; 
        }
        while(curr != null);
        
        return map;
    } // headMap

    /********************************************************************************
     * Return the portion of the B+Tree map where fromKey <= key.
     * @return  the submap with keys in the range [fromKey, lastKey]
     */
    public SortedMap <K,V> tailMap (K fromKey)
    {
        //  T O   B E   I M P L E M E N T E D

    	 SortedMap<K,V> map = new TreeMap<K,V>();
         Node curr = root;
         while(!curr.isLeaf && curr.nKeys > 0)
        	 curr = (Node) curr.ref[curr.nKeys];   
         
         do
         {
             for(int i = 0; i < curr.nKeys; i++)
             {
                 if(curr.key[i].compareTo(fromKey) >= 0)
                	 map.put(curr.key[i], (V) curr.ref[i+1]);
                 else 
                 	return map;
             }
             curr = (Node) curr.ref[0];
         }
         while(curr != null);
         return map;
    } // tailMap

    /********************************************************************************
     * Return the portion of the B+Tree map whose keys are between fromKey and toKey,
     * i.e., fromKey <= key < toKey.
     * @return  the submap with keys in the range [fromKey, toKey)
     * @author Michael Tan
     */
    public SortedMap <K,V> subMap (K fromKey, K toKey)
    {


    	/**Make the temporary node the root*/
        Node tmpNode = root;
        
    	/**Create a new treeMap*/
    	SortedMap<K,V> treeMap = new TreeMap<K,V>();
    	
        /**While the temporary node is not a leaf and it's number of keys is greater than zero, compare and increment x into the temp node*/
        while (!tmpNode.isLeaf && tmpNode.nKeys > 0) 
        {
            int x = 0;
            while (x < tmpNode.nKeys && tmpNode.key[x].compareTo(fromKey) < 0)
            {
                ++x;
            	tmpNode = (Node)tmpNode.ref[x];
            }
        }
        /**Loop through this while the temporary node is a leaf*/
        do
        {
        	/**If the temporary node is greater than i*/
            for (int i = 0; i < tmpNode.nKeys; ++i)
            {
                if (tmpNode.key[i].compareTo(fromKey) >= 0)
                {
                    if (tmpNode.key[i].compareTo(toKey) <0)
                    {
                        treeMap.put(tmpNode.key[i], (V)tmpNode.ref[i+1]);
                    }
                    
                    else
                    { 
                        return treeMap;
                    }
                }                
            }
            
            tmpNode = (Node)tmpNode.ref[ORDER-1]; 
        }
        /**Do this while the temporary node is not null*/
        while (tmpNode != null);
        /**return the tree map*/
        return treeMap;
    } // subMap

    /********************************************************************************
     * Return the size (number of keys) in the B+Tree.
     * @return  the size of the B+Tree
     */
    public int size ()
    {
    	 int sum = 0;
         Stack<Node> stack = new Stack();
         stack.push(root);
         Node curr;
         while(!stack.empty())
         {
             curr = stack.pop();
             sum += curr.nKeys;
             if(!curr.isLeaf)
             {
                 for(int i = 0; i <= curr.nKeys; i++)
                	 stack.push((Node) curr.ref[i]);
             }
         }
         return sum;
    } // size

    /********************************************************************************
     * Print the B+Tree using a pre-order traveral and indenting each level.
     * @param n      the current node to print
     * @param level  the current level of the B+Tree
     */
    @SuppressWarnings("unchecked")
    private void print (Node n, int level)
    {
    	if (n == null)
    	{
    		return;
    	}
        out.println ("BpTreeMap");
        out.println ("-------------------------------------------");

        for (int j = 0; j < level; j++) out.print ("\t");
        out.print ("[ . ");
        for (int i = 0; i < n.nKeys; i++) out.print (n.key [i] + " . ");
        out.println ("]");
        if ( ! n.isLeaf) {
            for (int i = 0; i <= n.nKeys; i++) print ((Node) n.ref [i], level + 1);
        } // if

        out.println ("-------------------------------------------");
    } // print

    /********************************************************************************
     * Recursive helper function for finding a key in B+trees.
     * @param key  the key to find
     * @param ney  the current node
     */
    @SuppressWarnings("unchecked")
    private V find (K key, Node n)
    {
        count++;
        for (int i = 0; i < n.nKeys; i++) {
            K k_i = n.key [i];
            if (key.compareTo (k_i) < 0) {
                if (n.isLeaf) {
                    return (key.equals (k_i)) ? (V) n.ref [i+1] : null;
                } else {
                    return find (key, (Node) n.ref [i]);
                } // if
            } // if
            
            if (key.compareTo (k_i) == 0) {
                if (n.isLeaf) {
                    return (key.equals (k_i)) ? (V) n.ref [i+1] : null;
                } else {
                    return find (key, (Node) n.ref [i+1]);
                } // if
            } // if            
        } // for
        return (n.isLeaf) ? null : find (key, (Node) n.ref [n.nKeys]);
    } // find

    /********************************************************************************
     * Recursive helper function for inserting a key in B+trees.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     * @param p    the parent node
     */
    private Node insert (K key, V ref, Node n, Node p)
    {
    	Node newNode = null;  
        //n is not a leaf
    	if(!n.isLeaf)
         {  
             for(int i = 0; i < n.nKeys; ++i)
             {
                 if(key.compareTo(n.key[i]) < 0) 
                 {
                     newNode = insert(key, ref, (Node) n.ref[i], n);
                     break;
                 }
                 else if(key.compareTo(n.key[i]) == 0)
                 {
                     out.println ("BpTree:insert: attempt to insert duplicate key = " + key);
                     return null;
                 }                       
                 else if(i == n.nKeys-1) 
                 {
                     newNode = insert(key, ref, (Node) n.ref[n.nKeys], n);
                     break;
                 }
             }
             if(newNode != null)
             {
            	 // key has not been fully inserted
                 if(n.nKeys < ORDER-1)
                 {
                	 //insert new node to n directly if n still has space
                     int pos = 0;
                     for(int i = 0; i < n.nKeys; ++i)
                     {
                        K pushKey = newNode.key[0];
                       
                        if(n.key[i].compareTo(pushKey)>=0)
                        { 		
                             pos = i; 
                             break;
                         	}
                         else if(i == n.nKeys-1)
                         {
                         	pos = n.nKeys;
                         	break;
                         }
                     }
                    wedge(newNode.key[0], (V) newNode, n, pos);
                }
                 else
                 {
                	 //split n and return the new Node                	
                 	 Node temp = split(newNode.key[0], (V) newNode, n);
                     if(p == null) 
                     {//create a new root for n
                         p = new Node(false);
                         p.ref[0] = n;
                         p.ref[1] = temp; 
                         p.key[0] = temp.key[0];
                         p.nKeys++;
                         root = p;
                         return null;
                     }
                     
                     return temp;
                 }
             }
         }     
         else if (n.nKeys < ORDER - 1)  
         {
         	map.put(key, ref); 
         	for (int i = 0; i < n.nKeys; ++i) 
             {
                 K ithKey = n.key [i];
                 if (key.compareTo (ithKey) < 0)  
                 {
                     wedge (key, ref, n, i);
                     return null;
                 } 
                 else if (key.equals (ithKey)) 
                 {
                     out.println ("BpTree:insert: attempt to insert duplicate key = " + key);
                     return null;
                 } // if
             } // for
             wedge (key, ref, n, n.nKeys); 
         } 
         else  
         {
        	// n does not have space so split 
         	map.put(key, ref);
         	for(int i = 0; i < n.nKeys; ++i)
             {
                 if(key.compareTo(n.key[i]) == 0)
                 {
                     out.println ("BpTree:insert: attempt to insert duplicate key = " + key);
                     return null;
                 }
             }
             Node sibling = split (key, ref, n); 
                 
             if(p == null) 
             {
            	 //create a new root for n
                 p = new Node (false);          
                 wedge (sibling.key[0],(V)sibling, p,0);
                 p.ref[0] = n;
                 root = p;
                 return null;
             }
             return sibling;
         } // if
         return null; 

    } // insert

    /********************************************************************************
     * Wedge the key-ref pair into node n.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     * @param i    the insertion position within node n
     */
    private void wedge (K key, V ref, Node n, int i)
    {
    	n.ref[n.nKeys+1] = n.ref[n.nKeys];
        for (int j = n.nKeys; j > i; j--) {
            n.key [j] = n.key [j - 1];
            n.ref [j] = n.ref [j - 1];
        } // for
        n.key [i] = key;
        n.ref [i + 1] = ref;
        n.nKeys++;
    } // wedge

    /********************************************************************************
     * Split node n and return the newly created node.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     * @author Michael Tan
     */
    private Node split (K key, V ref, Node n)
    {
    	 /**Check if node n is a leaf, if it is, then implement split method*/
        
    	/**Create a new tree node*/
        Node treeNode = new Node(n.isLeaf);
        /**Make a counter for number of nodes*/
        int numCounter = 0; 
        /**Size of nodes should be tree order divided by 2*/
        int sizeOfNode = ORDER / 2; 
        /**create a temporary tree map to hold the values*/
        TreeMap <K, V> temporaryTreeMap = new TreeMap <K, V>();
        
        /**Loop through the nodes*/
    	for(int i=0; i < n.nKeys; i++)  
    	{
    		K k_i = (K)n.key[i];
    		V k_v = (V)n.ref[i+1];
    		temporaryTreeMap.put(k_i,k_v);    		
    	}
    	
    	/**Add the keys to the temp tree Map*/
    	temporaryTreeMap.put(key,ref); 
    	
    	/**Loop through the temporary tree map*/
    	for (Map.Entry<K,V> entry : temporaryTreeMap.entrySet()) 
    	{
    		numCounter++;
    		/**If the sizeOfNode is greater or equal to the counter, then get the entry key and it's value*/
    		if(numCounter <= sizeOfNode) 
    		{
    			n.key[numCounter-1] = entry.getKey();
    			n.ref[numCounter] = entry.getValue(); 
    		}
    		/**Else, if the sizeOfNode is not greater than or equal, subtract sizOfNode to the counter to get it
    		 *the entry's key and it's value
    		 */
    		else 
    		{
    			treeNode.key[numCounter-sizeOfNode-1] = entry.getKey();
    			
    			treeNode.ref[numCounter-sizeOfNode] = entry.getValue();
    			
    			treeNode.nKeys++;
    			/**If treeOrder is greater than the counter, the key and ref should be equal to null*/
    	    	if (numCounter  < ORDER) 
    	    	{ 
    	    		n.key[numCounter - 1] = null;
        	    	n.ref[numCounter] = null;
				}
    		}		
    	}
    	
    	/**Number of keys should be equal to the size of the node*/
    	n.nKeys = sizeOfNode; 
    	
    	if (n.isLeaf) 
    	{
    		n.ref[ORDER-1] = treeNode;  
        	treeNode.ref[0] = n; 
		}
    	
    	return treeNode;
    } // split

    /********************************************************************************
     * The main method used for testing.
     * @param  the command-line arguments (args [0] gives number of keys to insert)
     */
    public static void main (String [] args)
    {
        BpTreeMap <Integer, Integer> bpt = new BpTreeMap <> (Integer.class, Integer.class);
        int totKeys = 10;
        if (args.length == 1) totKeys = Integer.valueOf (args [0]);
        for (int i = 1; i < totKeys; i += 2) bpt.put (i, i * i);
        bpt.print (bpt.root, 0);
        for (int i = 0; i < totKeys; i++) {
            out.println ("key = " + i + " value = " + bpt.get (i));
        } // for
        out.println ("-------------------------------------------");
        out.println ("Average number of nodes accessed = " + bpt.count / (double) totKeys);
    } // main

} // BpTreeMap class

