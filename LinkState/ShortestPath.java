package LinkState;

// A Java program for Dijkstra's single source shortest path algorithm.
// The program is for adjacency matrix representation of the graph
import java.util.*;
import java.lang.*;
import java.io.*;
 
class ShortestPath
{
    // A utility function to find the vertex with minimum distance value,
    // from the set of vertices not yet included in shortest path tree
    public static int V;
    
    public static final int INFINITY = 9999;
    
    int minDistance(int dist[][], Boolean sptSet[])
    {
        // Initialize min value
        int min = INFINITY, min_index=-1;
 
        //FILL IN THE CODE HERE
        for(int i = 0; i < sptSet.length;i++){
            if(sptSet[i] == true){//check edges of nodes that have already been added to the set
                for (int j = 0; j < sptSet.length;j++){
                    if(dist[i][j] < min && dist[i][j] > 0 && sptSet[j] == false){
                        //if node hasn't been discovered yet and has a min edge cost
                        min = dist[i][j];
                        min_index = j;
                    }
                }
            }
        }
 
        return min_index;
    }

    // A utility function to print the constructed distance array
    void printSolution(int dist[][])
    {
        System.out.println("Vertex Distance from Source");
        for (int i = 0; i < V; i++)
            System.out.println("Dest:"+i+" Cost:"+dist[i][0]+" Path:"+dist[i][1]);
    }

    // Function that implements Dijkstra's single source shortest path
    // algorithm for a graph represented using adjacency matrix
    // representation
    int[][] dijkstra(int graph[][], int src)
    {
        int output[][] = new int[4][2];
        Boolean[] sptSet = new Boolean [4];
        Arrays.fill(sptSet,false);
        
        // FILL IN THE CODE HERE
        int parents[] = new int [4];
        sptSet[src] = true;
        output[src][0] = 0;
        output[src][1] = src;
        parents[src] = src;


        for(int i = 0; i < sptSet.length;i++){
            int next_min = minDistance(graph, sptSet);
            int min_weight = INFINITY;
            int parent = -1;
            if(next_min >=0){
                for(int j = 0; j < graph[i].length; j++){
                    if(sptSet[j] == true && graph[next_min][j] < min_weight && graph[next_min][j] != 0){
                        min_weight = graph[next_min][j];
                        parent = j;
                        System.out.println(parent);
                        System.out.println(next_min);
                    }
                }
                if(parent == -1){
                    parent = V;
                }

                sptSet[next_min] = true;
                output[next_min][0] = graph[parent][next_min] + output[parent][0];
                output[next_min][1] = parent;
                parents[next_min] = parent;
                V = parent;
            }
        }
        V = 4;
        
        return output;

    }
 
    // Driver method - example of object creation
    public static void main (String[] args)
    {
    	int graph[][]= new int[][]{{0, 1, 1, 9999},
            					   {1, 0,10, 7},
            					   {1,10, 0, 2},
            					   {9999, 7, 2, 0},
    							   };
    							   
        ShortestPath t = new ShortestPath();
        t.dijkstra(graph, 3);

    }
}
