package LinkState;

import javax.sound.midi.SysexMessage;
import javax.swing.*;
import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

/**
 * This is the class that students need to implement. The code skeleton is provided.
 * Students need to implement rtinit(), rtupdate() and linkhandler().
 * printdt() is provided to pretty print a table of the current costs for reaching
 * other nodes in the network.
 */ 
public class Node { 

	public static final int INFINITY = 9999;
	public static final int NUM_ROUTERS = 4;


	int[] lkcost;				/*The link cost between node 0 and other nodes*/
	int nodename;           	/*Name of this node*/
	int[][] costs;				/*forwarding table, where index is destination node, [i][0] is cost to destination node and
  	  							  [i][1] is the next hop towards the destination node */

	int[][] graph;				/*Adjacency metric for the network, where (i,j) is cost to go from node i to j */
//	ShortestPath t;             /*Have Dijkstra's implementation */
	Set<Integer> ControlFlooding = new HashSet<Integer>();
	public int PACKETS_SENT = 0;
	/* Class constructor */
	public Node() { }

	/* students to write the following two routines, and maybe some others */
	void rtinit(int nodename, int[] initial_lkcost) {
		System.out.println("rtinit was just called!");
		this.nodename = nodename;
		this.lkcost = initial_lkcost;
		// Set up the graph: copy lkcost to the correct node,
		// and set the rest to infinity (this helps with dijkstras)
		graph = new int[4][4];
		for(int i = 0; i < NUM_ROUTERS; i++){
			if(i == nodename) {
				graph[nodename] = initial_lkcost;
				continue;
			}
			Arrays.setAll(graph[i], p -> INFINITY);
		}
		// init costs and copy over lkcost into it.
		costs = new int[4][2];
		for(int i = 0; i < 4; i++){
				costs[i][0] = lkcost[i];
				costs[i][1] = i;
		}

		// send the initial packet to all neighbors
		// this packet has the cost contents of the current node because that
		// is all that it knows.
		for(int i =0; i < NUM_ROUTERS; i++){
			if(graph[nodename][i] != INFINITY && i != nodename){
				Packet make_pkt = new Packet(nodename, i, nodename, graph[nodename], nodename);
				NetworkSimulator.tolayer2(make_pkt);
				PACKETS_SENT += 1;
			}
		}
	}
	int minimum_dist_in_row(int graph[][], int src, Set Nprime) {
		// This is a helper function for djikstras algorithm. It will check
		// to make sure that w is not in N' and so that D(w) is a minimum.
		int min = INFINITY;
		int index = -1;
		for(int i = 0; i < 4; i++){
			if (graph[src][i] < min && graph[src][i] != 0 && !Nprime.contains(i)){
				min = graph[src][i];
				index = i;
			}
		}
		// this is the special case for when the link changes. It will
		// not detect if the min distance is still infinity, so it will
		// make the index the only element that has not been visited yet.
		if(index == -1){
			for(int i = 0; i < 4; i++){
				if(!Nprime.contains(i)){
					index = i;
				}
			}
		}
		return index;
	}
	int[][] dijkstra(int graph[][], int src) {
		// This is the implementation for dijkstra's algorithm
		// this is what is in the textbook on pg.384

		// init
		int output[][] = new int[4][2];
		int[][] dist = graph;
		Set<Integer> Nprime = new HashSet<Integer>();
		Nprime.add(src);
		for(int i = 0; i < 4; i++){
			if(dist[src][i] != INFINITY && dist[src][i] != 0){
				dist[src][i] = graph[src][i];
			}
			output[i][0] = INFINITY;
			output[i][1] = src;
		}

		//loop
		while(Nprime.size() != 4){
			int w = minimum_dist_in_row(dist, src, Nprime);
			Nprime.add(w);
			for(int j = 0; j < 4; j++){
				if(dist[src][j] > dist[src][w] + dist[w][j]){
					dist[src][j] = dist[src][w] + dist[w][j];
					output[j][0] = dist[src][j];
					output[j][1] = w;
				} else if (dist[src][j] < dist[src][w] + dist[w][j]){
					output[j][0] = dist[src][j];
				}
			}
		}
		return output;
	}
	void deepCopyArray(int[][] source, int[][] dest){
		// this is a helper function. It will deep copy an array instead of
		// just passing references.
		for(int i = 0; i < source.length; i++){
			for(int j = 0; j < source[i].length; j++){
				dest[i][j] = source[i][j];
			}
		}
	}
	boolean deepCompare(int[][] source, int[][] other){
		// Compare 2D arrays. Used to be much longer until learning of below method.
		boolean check1 = Arrays.deepEquals(source, other);
		return check1;
	}
	void rtupdate(Packet rcvdpkt) {
		System.out.println("Node "+nodename+" just entered rtupdate!");
		// This function will propogate packets through the network

		// Control flooding: if the router already received a packet,
		// don't send it again. Done by adding it to a set and checking
		// against the set.
		if(ControlFlooding.contains(rcvdpkt.seqNo)){
			return;
		}

		// Special case: if the link weights were updated, seqNo will be negative.
		// This means the graph needs to be reset as costs and paths for nodes
		// may go through node 0/1 and if not reset, will not be properly updated
		if(rcvdpkt.seqNo < 0){
			// copy over the lkcost to put it into the graph later
			int[] lkcost_copy = new int[4];
			for(int i = 0; i < 4; i++){
				lkcost_copy[i] = lkcost[i];
			}
			// set the graph to infinity. If this isn't done, costs will not
			// be accurately updated
			for(int i = 0; i < 4; i++){
				Arrays.setAll(graph[i], p -> INFINITY);
			}

			// copy the new costs into the graph
			for(int i =0; i<4;i++){
				graph[nodename][i] = lkcost_copy[i];
			}
//			graph[nodename] = lkcost_copy;
		}

		// When a packet is picked up, propagate it through the network
		for(int i = 0; i < NUM_ROUTERS; i++){
			Packet make_pkt = rcvdpkt;
			make_pkt.sourceid = nodename;
			make_pkt.destid = i;
			if(lkcost[i] != INFINITY && i != nodename && rcvdpkt.nodename != rcvdpkt.destid){
				System.out.println("Node "+ nodename +" just send a packet to node " +make_pkt.destid + " with source " + make_pkt.nodename + " containing the mincost array: " + Arrays.toString(make_pkt.mincost));
				NetworkSimulator.tolayer2(make_pkt);
				PACKETS_SENT += 1;
			}
		}
		// Add the packets information
		graph[rcvdpkt.nodename] = rcvdpkt.mincost;
		ControlFlooding.add(rcvdpkt.seqNo);

		// need to create a deep copy of costs for later
		int[][] old_costs = new int[4][2];
		deepCopyArray(costs, old_costs);

		// need to create a deep copy of the graph array for later
		int[][] graph_copy = new int[4][4];
		deepCopyArray(graph, graph_copy);

		// calling this on graph is by reference so it seems to send out its own packets immedediately
		// call dijkstras on the copy graph,
		int[][] test = dijkstra(graph_copy, this.nodename);
		deepCopyArray(test, costs);

		// create a new mincost array that will run if theres an update
		int[] new_mincost = new int[4];
		for(int i = 0; i < 4; i++){
			new_mincost[i] = costs[i][0];
		}
		// create a new costs table
		int[][] updated_table = new int[4][2];
		deepCopyArray(costs, updated_table);

		// propagate new costs table and then copy it in costs
		for(int i = 0; i < 4; i++){
			if(updated_table[i][1] == nodename){
				updated_table[i][1] = i;
			}
		}
		deepCopyArray(updated_table, costs);

		// Compare old costs and new costs. Only send a packet to neighbors
		// if the costs have been updated
		if(deepCompare(old_costs, costs) == false) {
			for (int i = 0; i < 4; i++) {
				if(i != nodename && graph[nodename][i] != INFINITY){
					Packet updated_pkt = new Packet(nodename, i, nodename, new_mincost, nodename + PACKETS_SENT * 4);
					System.out.println("Node "+ nodename +" just send a packet to node " +updated_pkt.destid + " with source " + updated_pkt.nodename + " containing the mincost array: " + Arrays.toString(updated_pkt.mincost));
					NetworkSimulator.tolayer2(updated_pkt);
					PACKETS_SENT+=1;
				}
			}
		}
		System.out.println("Number of packets: "+PACKETS_SENT + " and here's the final costs array on this call of rtupdate: ");
		printdt();
	}

	/* called when cost from the node to linkid changes from current value to newcost*/
	void linkhandler(int linkid, int newcost) {
		System.out.println("Linkhandler was just triggered!!" + linkid + " " + newcost);
		// Need to correctly send packets out, so inverse the linkid here
		int to_update = Math.abs((linkid - 1));

		// intermediate array keeps track of lkcost
		int[] inter = new int[4];
		for(int i = 0; i < 4; i++){
			inter[i] = lkcost[i];
		}
		inter[linkid] = newcost;
		lkcost[to_update] = newcost;

		// update lkcost and the graph.
		// seems redundant but apparently it isnt.
		for(int i = 0; i < 4; i++){
			Arrays.setAll(graph[i], p -> INFINITY);
		}
		lkcost = inter;

		// send a special packet that denotes that link costs have changed
		Packet make_pkt = new Packet(nodename, to_update, nodename, inter, to_update - PACKETS_SENT*4);
		PACKETS_SENT++;
		rtupdate(make_pkt);
	}

	/* Prints the current costs to reaching other nodes in the network */
	void printdt() {

		System.out.printf("                    \n");
		System.out.printf("   D%d |   cost  next-hop \n", nodename);
		System.out.printf("  ----|-----------------------\n");
		System.out.printf("     0|  %3d   %3d\n",costs[0][0],costs[0][1]);
		System.out.printf("dest 1|  %3d   %3d\n",costs[1][0],costs[1][1]);
		System.out.printf("     2|  %3d   %3d\n",costs[2][0],costs[2][1]);
		System.out.printf("     3|  %3d   %3d\n",costs[3][0],costs[3][1]);
		System.out.printf("                    \n");
	}

}