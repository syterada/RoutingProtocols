package DistanceVector;
import java.io.*;

/**
 * This is the class that students need to implement. The code skeleton is provided.
 * Students need to implement rtinit(), rtupdate() and linkhandler().
 * printdt() is provided to pretty print a table of the current costs for reaching
 * other nodes in the network.
 */ 
public class Node { 
    
    public static final int INFINITY = 9999;
    
    int[] lkcost;		/*The link cost between this node and other nodes*/
    int[][] costs;  		/*Define distance table*/
    int nodename;               /*Name of this node*/

	int rounting_messages_sent; //routing messages statistic 

    
    /* Class constructor */
    public Node() { }

	int[][] make_costs(int[] initial, int nodename){//init costs table. Right now we only know direct neighbors. Rest is 9999
		int[][] return_costs = new int [4][4];
		for (int i = 0; i < return_costs.length; i++){
			for (int j = 0;j<return_costs[0].length;j++){
				if(i == j){
					return_costs[i][j] = initial[i];
				}else{
					return_costs[i][j] = INFINITY;
				}
			}
		}
		return return_costs;
	}

	int[][] update_costs(int[] min_from_neighbor, int[][] current_costs, int node_from){//updates costs table after receiving from neighbor
		int[][] return_costs = current_costs;
		for(int i = 0; i < min_from_neighbor.length; i++){
			if(i != nodename){
				return_costs[i][node_from] = min_from_neighbor[i] + this.lkcost[node_from];
				if(return_costs[i][node_from] > INFINITY){
					return_costs[i][node_from] = INFINITY;
				}
			}
		}
		
		return return_costs;
	}

	int[] find_current_mins(){//Bellman-Ford, finds min cost to each node by iterating over rows and cols and picking min val for that node
		int[] current_mins = new int [4];
		for(int i = 0; i < this.costs.length; i++){
			int min_for_row = INFINITY;
			for(int j = 0; j < this.costs[0].length; j++){
				if(this.costs[i][j] < min_for_row){
					min_for_row = this.costs[i][j];
				}
			}
			current_mins[i] = min_for_row;
		}
		return current_mins;
	}

	void update_costs_after_increase(int linkid, int newcost, int oldcost){//updates costs after a link value changes
		int change_from_old_cost = newcost - oldcost;
		for(int i = 0; i < this.lkcost.length; i++){
				this.costs[i][linkid] = this.costs[i][linkid] + change_from_old_cost;
				if(this.costs[i][linkid] > INFINITY){
					this.costs[i][linkid] = INFINITY;
				}
			}


	}
    
    /* students to write the following two routines, and maybe some others */
    void rtinit(int nodename, int[] initial_lkcost) { 
		this.nodename = nodename;
		this.lkcost = initial_lkcost;
		this.costs = make_costs(initial_lkcost,this.nodename);
		this.rounting_messages_sent = 0;//initialization lines

		for(int i = 0;i<lkcost.length;i++){//send information to our neighbors
			if(initial_lkcost[i] != 0 && initial_lkcost[i] != INFINITY){
				Packet pkt = new Packet(this.nodename,i,initial_lkcost);
				NetworkSimulator.tolayer2(pkt);
				this.rounting_messages_sent += 1;
			}
		}
		System.out.println("Initialization for packet " + nodename);
		printdt();
	}    
    
    void rtupdate(Packet rcvdpkt){ 

		int[] min_from_neighbor = rcvdpkt.mincost;
		int node_from = rcvdpkt.sourceid;
		int[] current_forwarding_table = find_current_mins();//find mins from knowledge before receipt
		System.out.println(this.nodename + " received an update from " + node_from);
		System.out.println("Table before update:");//table before updating
		printdt();

		this.costs = update_costs(min_from_neighbor,this.costs, node_from);//update costs with new knowledge
		int[] new_forwarding_table = find_current_mins();//table after updates
		boolean table_different = false;
		for(int i = 0; i < new_forwarding_table.length; i++){//sees if new table is different from old table.
			if(new_forwarding_table[i] != current_forwarding_table[i]){
				table_different = true;
				break;
			}
			if(i == 3){
				System.out.println("No longer sending");
			}
		}
		if(table_different){//if we find out that the table is different, send information to our neighbors.
			for(int i = 0;i<lkcost.length;i++){
				if(this.lkcost[i] != 0 && this.lkcost[i] != INFINITY){
					Packet pkt = new Packet(this.nodename,i,new_forwarding_table);
					NetworkSimulator.tolayer2(pkt);
					this.rounting_messages_sent += 1;
				}
			}
			System.out.println("sending: " + new_forwarding_table[0] + "  "+ 
					new_forwarding_table[1] + "  "+new_forwarding_table[2] + "  "+new_forwarding_table[3]);
		}
		
		System.out.println("routing messages sent from node " + this.nodename + ": " + this.rounting_messages_sent);
		System.out.println("Updated table: ");
		printdt();


	}
    
    
    /* called when cost from the node to linkid changes from current value to newcost*/
    void linkhandler(int linkid, int newcost) {  
		//this method is only called on nodes 0 and 1. The only link that changes is the link btwn 0 and 1.
		System.out.println("changing link to " + newcost);
		int oldcost = this.lkcost[linkid];//take old cost
		this.lkcost[linkid] = newcost;//update link cost
		update_costs_after_increase(linkid, newcost, oldcost);
		int[] mins_after_increase = find_current_mins();//update costs and find new table after increase, then send to neighbors
		for(int i = 0; i< this.lkcost.length; i++){
			if(this.lkcost[i] != 0 && this.lkcost[i] != INFINITY){
				Packet pkt = new Packet(this.nodename,i,mins_after_increase);
				NetworkSimulator.tolayer2(pkt);
				this.rounting_messages_sent +=1;
			}
		}
		System.out.println(this.nodename + " sending after cost change: " + mins_after_increase[0] + "  "+ 
					mins_after_increase[1] + "  "+mins_after_increase[2] + "  "+mins_after_increase[3]);
		System.out.println("routing messages sent from node " + this.nodename + ": " + this.rounting_messages_sent);
	}    


    /* Prints the current costs to reaching other nodes in the network */
    void printdt() {
        switch(nodename) {
	
	case 0:
	    System.out.printf("                via     \n");
	    System.out.printf("   D0 |    1     2 \n");
	    System.out.printf("  ----|-----------------\n");
	    System.out.printf("     1|  %3d   %3d \n",costs[1][1], costs[1][2]);
	    System.out.printf("dest 2|  %3d   %3d \n",costs[2][1], costs[2][2]);
	    System.out.printf("     3|  %3d   %3d \n",costs[3][1], costs[3][2]);
	    break;
	case 1:
	    System.out.printf("                via     \n");
	    System.out.printf("   D1 |    0     2    3 \n");
	    System.out.printf("  ----|-----------------\n");
	    System.out.printf("     0|  %3d   %3d   %3d\n",costs[0][0], costs[0][2],costs[0][3]);
	    System.out.printf("dest 2|  %3d   %3d   %3d\n",costs[2][0], costs[2][2],costs[2][3]);
	    System.out.printf("     3|  %3d   %3d   %3d\n",costs[3][0], costs[3][2],costs[3][3]);
	    break;    
	case 2:
	    System.out.printf("                via     \n");
	    System.out.printf("   D2 |    0     1    3 \n");
	    System.out.printf("  ----|-----------------\n");
	    System.out.printf("     0|  %3d   %3d   %3d\n",costs[0][0], costs[0][1],costs[0][3]);
	    System.out.printf("dest 1|  %3d   %3d   %3d\n",costs[1][0], costs[1][1],costs[1][3]);
	    System.out.printf("     3|  %3d   %3d   %3d\n",costs[3][0], costs[3][1],costs[3][3]);
	    break;
	case 3:
	    System.out.printf("                via     \n");
	    System.out.printf("   D3 |    1     2 \n");
	    System.out.printf("  ----|-----------------\n");
	    System.out.printf("     0|  %3d   %3d\n",costs[0][1],costs[0][2]);
	    System.out.printf("dest 1|  %3d   %3d\n",costs[1][1],costs[1][2]);
	    System.out.printf("     2|  %3d   %3d\n",costs[2][1],costs[2][2]);
	    break;
        }
    }
    
}
