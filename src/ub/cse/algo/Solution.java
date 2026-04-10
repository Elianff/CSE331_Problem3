package ub.cse.algo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;

public class Solution {

    private Info info;
    private Graph graph;
    private ArrayList<Client> clients;
    private ArrayList<Integer> bandwidths;

    /**
     * Basic Constructor
     *
     * @param info: data parsed from input file
     */
    public Solution(Info info) {
        this.info = info;
        this.graph = info.graph;
        this.clients = info.clients;
        this.bandwidths = info.bandwidths;
    }

    /**
     * Method that returns the calculated
     * SolutionObject as found by your algorithm
     *
     * @return SolutionObject containing the paths, priorities and bandwidths
     */
    public SolutionObject outputPaths() {
        SolutionObject sol = new SolutionObject();
        sol.bandwidths = new ArrayList<>(bandwidths);

        int[] bandWidthCount = new int[bandwidths.size()];   //create integer array to store bandwidth count at each id/index. We don't plan to add more than bandwidths.size

        //need sol.priorities to organize priorities.
        //determine order based on payment
        ArrayList<Client> sortedClients = new ArrayList<>(clients); //based on payments
        //sort sortedClients
        //either use sortedClients.sort() if allowed or manually do it. Use client.payment?
        //https://docs.oracle.com/javase/8/docs/api/java/util/Comparator.html:
        //to sort the clients. If negative, client a is first, and if positive, b

        sortedClients.sort((a,b) -> {
            //FCC clients go first
            if(a.isFcc && b.isFcc) return 0;
            else if (a.isFcc && !b.isFcc) return -1;
            else if (!a.isFcc && b.isFcc) return 1;

            else if (b.payment != a.payment) {
                //effectively: if b.payment>a.payment, return 1, b goes first
                //if a.payment>b.payment, return -1, a goes first
                //simplified b.payment-a.payment
                //example: b=100 and a=500 then 100-500=-400, a goes first
                return b.payment - a.payment;
            }

            //compare the beta
            else if (a.beta != b.beta)
                return Float.compare(a.beta, b.beta);

            //if it is equal, need tiebreaker to determine which one is first.
            return Float.compare(a.alpha, b.alpha);
        });

        for (Client client : sortedClients) {
            //https://docs.oracle.com/javase/8/docs/api/java/util/PriorityQueue.html
            PriorityQueue<int[]> todo = new PriorityQueue<>((a, b) -> a[1] - b[1]);
            int[] minCost = new int[graph.size()]; //paths with the cheapest costs
            Arrays.fill(minCost, Integer.MAX_VALUE);      //default fill with really big numbers
            int[] priority = new int[graph.size()];
            Arrays.fill(priority, -1);                 //default fill with smallest numbers, update when adding

            int startNode = graph.contentProvider; //Need to locate starting node
            minCost[startNode] = 0;     //distance from startNode to..startNode is 0
            int[] toQueue = {startNode, 0};    //increased steps to make it clearer, store the node and the cost, least cost prioritized
            todo.add(toQueue);

            //Queue
            while (!todo.isEmpty()) {
                int[] current = todo.poll();   // take first from queue
                int node = current[0];         //current looks like {int, int} or {startNode, cost}

                //trying to improve runtime
                //skip if the path is not better than the one we already found
                if (current[1] > minCost[node]) continue;

                if (node == client.id)// removing client from the queue
                    break;

                for (Integer currentNode : graph.get(node)) {//going through all non-client nodes
                    //trying to improve runtime
                    //skip nodes with 0 bandwidth
                    if (bandwidths.get(currentNode) == 0) continue;

                    int extra = (bandWidthCount[currentNode] + 1)/ bandwidths.get(currentNode); //+1 because it rounds down
                    int newCost = minCost[node] + 1 + extra;  //increases revenue from 1200...0 to 1300...0

                    if (newCost < minCost[currentNode]) {//if distance of node to starting node is >than new cost, a less
                        minCost[currentNode] = newCost;  //update
                        priority[currentNode] = node;
                        int[] toAdd= {currentNode, newCost};
                        todo.add(toAdd);//adding new cost to queue
                    }
                }
            }

            if (priority[client.id] != -1) { //to make sure it exits, reconstruct path and backtrack
                ArrayList<Integer> path = new ArrayList<>(); //initial plan for problem 1
                int currentClient = client.id; //start from client
                //backtrack from client to ISP
                while (currentClient != startNode) {//condition for looping
                    path.add(0, currentClient);//constructing path
                    currentClient = priority[currentClient];
                }

                path.add(0, startNode); //add ISP at front
                sol.paths.put(client.id, path);

                for (int i = 0; i < path.size()-1 ; i++) {//counting bandwidth to calculate new cost for next client
                    bandWidthCount[path.get(i)]++;
                }
            }
        }

        //increase the bandwidth
        for (int i = 0; i < bandwidths.size(); i++) {
            if (bandwidths.get(i) == 0) continue;

            int used = bandWidthCount[i];
            int capacity = bandwidths.get(i);

            if(used > capacity) {
                //small increase: used=11, capacity=10 -> increase, increase=1
                //large increase: used=20, capacity=10 -> increase, increase=6
                int increase = (used - capacity) / 2 + 1;
                sol.bandwidths.set(i, capacity + increase);
            }
        }

        return sol;
    }
}
