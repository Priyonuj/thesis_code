/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Charafeddine Mechalikh
 **/
package com.mechalikh.pureedgesim.tasksorchestration;

import java.util.*;
import java.util.stream.IntStream;

import com.mechalikh.pureedgesim.datacentersmanager.ComputingNode;
import com.mechalikh.pureedgesim.scenariomanager.SimulationParameters;
import com.mechalikh.pureedgesim.simulationmanager.SimulationManager;
import com.mechalikh.pureedgesim.tasksgenerator.Task;
import com.mechalikh.pureedgesim.simulationmanager.SimLog;
import net.sourceforge.jFuzzyLogic.FIS;

public class DefaultOrchestrator extends Orchestrator {
	protected Map<Integer, Integer> historyMap = new HashMap<>();
    protected  Map<Integer ,Double> server_energyMap = new HashMap<>();
	protected  Map<Integer ,Integer> task_queue = new HashMap<>();
	protected  Map<Integer ,Integer> task_server = new HashMap<>();
	static Vector<Integer> server_combination = new Vector<Integer>();
	int[] arr = new int[nodeList.size()];

	int total_edge_server = 3;

	public DefaultOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
		SimLog.println(" nodeList.size() : " +  nodeList.size());
		// Generate all binary strings

		generateAllBinaryStrings(total_edge_server,arr,0);

		//for (int i = 0; i < server_combination.size(); i++)
		//	SimLog.println(" server_combination : " + server_combination.get(i));
		// Initialize the history map
		for (int i = 1; i < total_edge_server+1; i++)
			server_energyMap.put(i, 0.0);
		for (int i = 1; i < total_edge_server+1; i++)
			task_queue.put(i, 0);
		int count = 0;
		/*for(int i=0;i<server_combination.size();i++)
		{
			if(count==3)
			{
				SimLog.println("---- ");
				count=0;
			}
			SimLog.println(" server_combination : " + server_combination.get(i));
			count++;
		}*/
		for(int i=9;i<12;i++)
		{
			int temp = server_combination.get(i);
			server_combination.set(i,server_combination.get(i+3));
			server_combination.set(i+3,temp);
		}


		/*for(int i=0;i<server_combination.size();i++)
		{
			if(count==3)
			{
				SimLog.println("---- ");
				count=0;
			}
			SimLog.println(" server_combination : " + server_combination.get(i));
			count++;
		}*/
		for (int i = 0; i < nodeList.size(); i++)
		   historyMap.put(i, 0);


	}
	public void printTheArray(int arr[], int n)
	{
		for (int i = 0; i < n; i++)
            server_combination.add(arr[i]);
	}
	public void generateAllBinaryStrings(int n, int arr[],int i)
	{
		if (i == n)
		{
			printTheArray(arr, n);
			return;
		}
		arr[i] = 0;
		generateAllBinaryStrings(n, arr ,i+ 1);

		arr[i] = 1;
		generateAllBinaryStrings(n, arr,i + 1);
	}


	protected int findComputingNode(String[] architecture, Task task) {

		if ("ROUND_ROBIN".equals(algorithm)) {
			return roundRobin(architecture, task);
		} else if ("TRADE_OFF".equals(algorithm)) {
			return tradeOff(architecture, task);
		}
		else if ("PROJECT_BPSO".equals(algorithm)) {
			return bpso(architecture, task);
		}
		else if ("FUZZY_LOGIC".equals(algorithm)) {
			return fuzzyLogic(task);
		 } else if ("SIMULATED".equals(algorithm)) {
			return simulated(task);
		} else {
			throw new IllegalArgumentException(getClass().getSimpleName() + " - Unknown orchestration algorithm '"
					+ algorithm + "', please check the simulation parameters file...");
		}
	}

	protected int tradeOff(String[] architecture, Task task) {
		int selected = -1;
		double min = -1;
		double new_min;// the computing node with minimum weight;

		// get best computing node for this task
		for (int i = 0; i < nodeList.size(); i++) {
			if (offloadingIsPossible(task, nodeList.get(i), architecture)) {
				// the weight below represent the priority, the less it is, the more it is
				// suitable for offlaoding, you can change it as you want
				double weight = 1.2; // this is an edge server 'cloudlet', the latency is slightly high then edge
										// devices
				if (nodeList.get(i).getType() == SimulationParameters.TYPES.CLOUD) {
					weight = 1.8; // this is the cloud, it consumes more energy and results in high latency, so
									// better to avoid it
				} else if (nodeList.get(i).getType() == SimulationParameters.TYPES.EDGE_DEVICE) {
					weight = 1.3;// this is an edge device, it results in an extremely low latency, but may
									// consume more energy.
				}
				new_min = (historyMap.get(i) + 1) * weight * task.getLength() / nodeList.get(i).getMipsCapacity();
				if (min == -1 || min > new_min) { // if it is the first iteration, or if this computing node has more
													// cpu mips and
													// less waiting tasks
					min = new_min;
					// set the first computing node as the best one
					selected = i;
				}
			}
		}
		if (selected != -1)
			historyMap.put(selected, historyMap.get(selected) + 1);
		// assign the tasks to the found vm
		return selected;
	}

	protected int roundRobin(String[] architecture, Task task) {
      //  SimLog.println("task sent : " + task.getId());
		int selected = -1;
		/*int minTasksCount = -1; // Computing node with minimum assigned tasks.
		for (int i = 0; i < nodeList.size(); i++) {
			if (offloadingIsPossible(task, nodeList.get(i), architecture)&& (minTasksCount == -1 || minTasksCount > historyMap.get(i))) {

				minTasksCount = historyMap.get(i);
				// if this is the first time,
				// or new min found, so we choose it as the best computing node.
				selected = i;
			}
		}
		// Assign the tasks to the obtained computing node.
		historyMap.put(selected, minTasksCount + 1);

		 */

		return selected;
	}



      protected int bpso(String[] architecture, Task task )
        {
			int selected = -1;
			//SimLog.println("task sent : " + task.getId());
			/*double final_approx_energy = Integer.MAX_VALUE;
			int a=0,b=0,c=0;
			double a_e = 0, b_e = 0,c_e = 0;
			for(int i=12;i<server_combination.size();i = i+3)
			{

				double es1 =  ((((nodeList.get(1).getEnergyModel().getMaxActiveConsumption() -
						nodeList.get(1).getEnergyModel().getIdleConsumption()) / 3600
						* task.getLength() / nodeList.get(1).getMipsCapacity()) ) + server_energyMap.get(1)) * server_combination.get(i);

				double es2 = ((((nodeList.get(2).getEnergyModel().getMaxActiveConsumption()-
						nodeList.get(2).getEnergyModel().getIdleConsumption()) / 3600
						* task.getLength() / nodeList.get(2).getMipsCapacity()))+ server_energyMap.get(2) ) * server_combination.get(i+1);

				double es3= ((((nodeList.get(3).getEnergyModel().getMaxActiveConsumption()-
						nodeList.get(3).getEnergyModel().getIdleConsumption()) / 3600
						* task.getLength() / nodeList.get(3).getMipsCapacity()))+ server_energyMap.get(3)) * server_combination.get(i+2);

				double total_approx_energy = es1 + es2 + es3;

				 if(final_approx_energy > total_approx_energy)
				 {
					 final_approx_energy = total_approx_energy;

					 a =  server_combination.get(i);
					 a_e = es1;
					 b = 2 * server_combination.get(i+1);
					 b_e = es2;
					 c = 3 * server_combination.get(i+2);
					 c_e = es3;
				 }
			}

			if(a == 0 && b==0 && c ==0)
				return selected;

			//if(a == 0 && b == 0) {selected = c ; server_energyMap.put(c, server_energyMap.get(c) + c_e);}
			//else if(a == 0 && c == 0) {selected = b ; server_energyMap.put(b, server_energyMap.get(b) + b_e);}
		   // else if(c == 0 && b == 0) {selected = a ; server_energyMap.put(a, server_energyMap.get(a) + a_e);}
			if(a == 0){selected = b_e > c_e ? c : b; server_energyMap.put(selected, server_energyMap.get(selected) + Math.min(b_e, c_e));}
			else if(b == 0) {selected = a_e > c_e ? c : a; server_energyMap.put(selected, server_energyMap.get(selected) +Math.min(a_e, c_e));}
			else if(c == 0) {selected = a_e > b_e ? b : a;server_energyMap.put(selected, server_energyMap.get(selected) +Math.min(a_e, b_e));}
			else {selected = a_e > b_e ? b_e > c_e ? c : b : a;
				server_energyMap.put(selected, server_energyMap.get(selected) + a_e > b_e ? Math.min(b_e, c_e) : a_e);}

			task_server.put(task.getId(),selected);*/

			double final_approx_energy = Integer.MAX_VALUE;
			int a=0,b=0,c=0;
			double a_e = 0, b_e = 0,c_e = 0;
			for(int i=12;i<server_combination.size();i = i+3)
			{

				int t1 = task_queue.get(1);
				int t2 = task_queue.get(2);
				int t3 = task_queue.get(3);

				if(t1 == 0) t1 = 1;
				if(t2 == 0) t2 = 1;
				if(t3 == 0) t3 = 1;
				double es1 =  ((((nodeList.get(1).getEnergyModel().getMaxActiveConsumption() -
						nodeList.get(1).getEnergyModel().getIdleConsumption()) / 3600
						* task.getLength() / nodeList.get(1).getMipsCapacity()) ) * t1) * server_combination.get(i);

				double temp1 = (((((nodeList.get(1).getEnergyModel().getMaxActiveConsumption() -
						nodeList.get(1).getEnergyModel().getIdleConsumption()) / 3600
						* task.getLength() / nodeList.get(1).getMipsCapacity()) ) * t1)
						- (nodeList.get(1).getEnergyModel().getIdleConsumption()) / 3600) /
						(((nodeList.get(1).getEnergyModel().getMaxActiveConsumption())/ 3600) -
								((nodeList.get(1).getEnergyModel().getIdleConsumption())/ 3600 ));
				double temp2 = (((((nodeList.get(2).getEnergyModel().getMaxActiveConsumption() -
						nodeList.get(2).getEnergyModel().getIdleConsumption()) / 3600
						* task.getLength() / nodeList.get(2).getMipsCapacity()) ) * t2)
						- (nodeList.get(2).getEnergyModel().getIdleConsumption()) / 3600) /
						(((nodeList.get(2).getEnergyModel().getMaxActiveConsumption())/ 3600) -
								((nodeList.get(2).getEnergyModel().getIdleConsumption())/ 3600 ));
				double temp3 = (((((nodeList.get(3).getEnergyModel().getMaxActiveConsumption() -
						nodeList.get(3).getEnergyModel().getIdleConsumption()) / 3600
						* task.getLength() / nodeList.get(3).getMipsCapacity()) ) * t3)
						- (nodeList.get(3).getEnergyModel().getIdleConsumption()) / 3600) /
						(((nodeList.get(3).getEnergyModel().getMaxActiveConsumption())/ 3600) -
								((nodeList.get(3).getEnergyModel().getIdleConsumption())/ 3600 ));

			//	SimLog.println("temp1 : " + temp1 + " temp2 : " + temp2 + "temp3 : " + temp3);

				double es2 = ((((nodeList.get(2).getEnergyModel().getMaxActiveConsumption()-
						nodeList.get(2).getEnergyModel().getIdleConsumption()) / 3600
						* task.getLength() / nodeList.get(2).getMipsCapacity())) * t2 ) * server_combination.get(i+1);

				double es3= ((((nodeList.get(3).getEnergyModel().getMaxActiveConsumption()-
						nodeList.get(3).getEnergyModel().getIdleConsumption()) / 3600
						* task.getLength() / nodeList.get(3).getMipsCapacity())) * t3) * server_combination.get(i+2);

				double total_approx_energy = es1 + es2 + es3;

				if(final_approx_energy > total_approx_energy)
				{
					final_approx_energy = total_approx_energy;

					a =  server_combination.get(i);
					a_e = es1;
					b = 2 * server_combination.get(i+1);
					b_e = es2;
					c = 3 * server_combination.get(i+2);
					c_e = es3;
				}
			}

			if(a == 0 && b == 0) {selected = c ;}
		    else if(a == 0 && c == 0) {selected = b ;}
			else if(c == 0 && b == 0) {selected = a ; }
			else if(a == 0){selected = b_e > c_e ? c : b;}
			else if(b == 0) {selected = a_e > c_e ? c : a; }
			else if(c == 0) {selected = a_e > b_e ? b : a;}
			else {selected = a_e > b_e ? b_e > c_e ? c : b : a;}

			task_queue.put(selected, task_queue.get(selected)+1);
			task_server.put(task.getId(),selected);

			return selected;
		}


	private int fuzzyLogic(Task task) {
		String fileName = "PureEdgeSim/examples/Example8_settings/stage1.fcl";
		FIS fis = FIS.load(fileName, true);
		// Error while loading?
		if (fis == null) {
			System.err.println("Can't load file: '" + fileName + "'");
			return -1;
		}
		double cpuUsage = 0;
		int count = 0;
		for (int i = 0; i < nodeList.size(); i++) {
			if (nodeList.get(i).getType() != SimulationParameters.TYPES.CLOUD) {
				count++;
				cpuUsage += nodeList.get(i).getAvgCpuUtilization();

			}
		}

		// Set fuzzy inputs
		fis.setVariable("wan",
				(SimulationParameters.WAN_BANDWIDTH_BITS_PER_SECOND - simulationManager.getNetworkModel().getWanUpUtilization())/SimulationParameters.WAN_BANDWIDTH_BITS_PER_SECOND);
		fis.setVariable("taskLength", task.getLength());
		fis.setVariable("delay", task.getMaxLatency());
		fis.setVariable("cpuUsage", cpuUsage / count);

		// Evaluate
		fis.evaluate();

		if (fis.getVariable("offload").defuzzify() > 50) {
			String[] architecture2 = { "Cloud" };
			return tradeOff(architecture2, task);
		} else {
			String[] architecture2 = { "Edge", "Mist" };
			return stage2(architecture2, task);
		}

	}

	private int stage2(String[] architecture2, Task task) {
		double min = -1;
		int vm = -1;
		String fileName = "PureEdgeSim/examples/Example8_settings/stage2.fcl";
		FIS fis = FIS.load(fileName, true);
		// Error while loading?
		if (fis == null) {
			System.err.println("Can't load file: '" + fileName + "'");
			return -1;
		}
		for (int i = 0; i < nodeList.size(); i++) {
			if (offloadingIsPossible(task, nodeList.get(i), architecture2)
					&& nodeList.get(i).getTotalStorage() > 0) {
				if (!task.getEdgeDevice().getMobilityModel().isMobile())
					fis.setVariable("vm_local", 0);
				else
					fis.setVariable("vm_local", 0);
				fis.setVariable("vm", (1 - nodeList.get(i).getAvgCpuUtilization()) * nodeList.get(i).getMipsCapacity() / 1000);
				fis.evaluate();

				if (min == -1 || min > fis.getVariable("offload").defuzzify()) {
					min = fis.getVariable("offload").defuzzify();
					vm = i;
				}
			}
		}

		return vm;
	}


	protected int simulated(Task task ){
		int selected = -1;
		int minTasksCount = -1; // Computing node with minimum assigned tasks.
		double temp=100;
		double a=0.9;
		double b=1.05;
		double nrep=1.0;
		double Es = 0.0;

		//for (int i = 1; i < 4; i++) {
		Es=((nodeList.get(1).getEnergyModel().getMaxActiveConsumption()- nodeList.get(1).getEnergyModel().getIdleConsumption())/3600
					* task.getLength() / nodeList.get(1).getTotalMipsCapacity());
		//}

		for (int i = 1; i < 4; i++) {

			selected = i;

			for (int j = 1; j <= nrep; j++) {

				/*
				double En = ((nodeList.get(j).getEnergyModel().getMaxActiveConsumption() -
						nodeList.get(j).getEnergyModel().getIdleConsumption()) / 3600
						* task.getLength() / nodeList.get(i).getTotalMipsCapacity());

				double diffEnergy = En - Es;

				if (diffEnergy < 0) {
					selected = j;
					Es = En;
				} else {
					double random_value = Math.random();

					if (random_value < (Math.exp((-diffEnergy) / temp))) {
						selected = j;
						Es = En;
					}
				}*/

			}

			temp = a * temp;
			nrep = b * nrep;
		}
			historyMap.put(selected, minTasksCount + 1);

			return selected;
	}
	public class Ant {

		protected int trailSize;
		protected int trail[];
		protected boolean visited[];

		public Ant(int tourSize) {
			this.trailSize = tourSize;
			this.trail = new int[tourSize];
			this.visited = new boolean[tourSize];
		}

		protected void visitCity(int currentIndex, int city) {
			trail[currentIndex + 1] = city;
			visited[city] = true;
		}

		protected boolean visited(int i) {
			return visited[i];
		}

		protected double trailLength(double graph[][]) {
			double length = graph[trail[trailSize - 1]][trail[0]];
			for (int i = 0; i < trailSize - 1; i++) {
				length += graph[trail[i]][trail[i + 1]];
			}
			return length;
		}

		protected void clear() {
			for (int i = 0; i < trailSize; i++)
				visited[i] = false;
		}

	}


	private double c = 1.0;
	private double alpha = 1;
	private double beta = 5;
	private double evaporation = 0.5;
	private double Q = 500;
	private double antFactor = 0.8;
	private double randomFactor = 0.01;

	private int maxIterations = 1000;

	private int numberOfCities;
	private int numberOfAnts;
	private double graph[][];
	private double trails[][];
	private List<Ant> ants = new ArrayList<>();
	private Random random = new Random();
	private double probabilities[];

	private int currentIndex;

	private int[] bestTourOrder;
	private double bestTourLength;
	private int ga(Task task) {


		return 0;
	}
	/**
	 * Generate initial solution
	 */
	public double[][] generateRandomMatrix(int n) {
		double[][] randomMatrix = new double[n][n];
		IntStream.range(0, n)
				.forEach(i -> IntStream.range(0, n)
						.forEach(j -> randomMatrix[i][j] = Math.abs(random.nextInt(100) + 1)));
		return randomMatrix;
	}

	/**
	 * Perform ant optimization
	 */
	public void startAntOptimization() {
		IntStream.rangeClosed(1, 3)
				.forEach(i -> {
					System.out.println("Attempt #" + i);
					solve();
				});
	}

	/**
	 * Use this method to run the main logic
	 */
	public int[] solve() {
		setupAnts();
		clearTrails();
		IntStream.range(0, maxIterations)
				.forEach(i -> {
					moveAnts();
					updateTrails();
					updateBest();
				});
		System.out.println("Best tour length: " + (bestTourLength - numberOfCities));
		System.out.println("Best tour order: " + Arrays.toString(bestTourOrder));
		return bestTourOrder.clone();
	}

	/**
	 * Prepare ants for the simulation
	 */
	private void setupAnts() {
		IntStream.range(0, numberOfAnts)
				.forEach(i -> {
					ants.forEach(ant -> {
						ant.clear();
						ant.visitCity(-1, random.nextInt(numberOfCities));
					});
				});
		currentIndex = 0;
	}

	/**
	 * At each iteration, move ants
	 */
	private void moveAnts() {
		IntStream.range(currentIndex, numberOfCities - 1)
				.forEach(i -> {
					ants.forEach(ant -> ant.visitCity(currentIndex, selectNextCity(ant)));
					currentIndex++;
				});
	}

	/**
	 * Select next city for each ant
	 */
	private int selectNextCity(Ant ant) {
		int t = random.nextInt(numberOfCities - currentIndex);
		if (random.nextDouble() < randomFactor) {
			OptionalInt cityIndex = IntStream.range(0, numberOfCities)
					.filter(i -> i == t && !ant.visited(i))
					.findFirst();
			if (cityIndex.isPresent()) {
				return cityIndex.getAsInt();
			}
		}
		calculateProbabilities(ant);
		double r = random.nextDouble();
		double total = 0;
		for (int i = 0; i < numberOfCities; i++) {
			total += probabilities[i];
			if (total >= r) {
				return i;
			}
		}

		throw new RuntimeException("There are no other cities");
	}

	/**
	 * Calculate the next city picks probabilites
	 */
	public void calculateProbabilities(Ant ant) {
		int i = ant.trail[currentIndex];
		double pheromone = 0.0;
		for (int l = 0; l < numberOfCities; l++) {
			if (!ant.visited(l)) {
				pheromone += Math.pow(trails[i][l], alpha) * Math.pow(1.0 / graph[i][l], beta);
			}
		}
		for (int j = 0; j < numberOfCities; j++) {
			if (ant.visited(j)) {
				probabilities[j] = 0.0;
			} else {
				double numerator = Math.pow(trails[i][j], alpha) * Math.pow(1.0 / graph[i][j], beta);
				probabilities[j] = numerator / pheromone;
			}
		}
	}

	/**
	 * Update trails that ants used
	 */
	private void updateTrails() {
		for (int i = 0; i < numberOfCities; i++) {
			for (int j = 0; j < numberOfCities; j++) {
				trails[i][j] *= evaporation;
			}
		}
		for (Ant a : ants) {
			double contribution = Q / a.trailLength(graph);
			for (int i = 0; i < numberOfCities - 1; i++) {
				trails[a.trail[i]][a.trail[i + 1]] += contribution;
			}
			trails[a.trail[numberOfCities - 1]][a.trail[0]] += contribution;
		}
	}

	/**
	 * Update the best solution
	 */
	private void updateBest() {
		if (bestTourOrder == null) {
			bestTourOrder = ants.get(0).trail;
			bestTourLength = ants.get(0)
					.trailLength(graph);
		}
		for (Ant a : ants) {
			if (a.trailLength(graph) < bestTourLength) {
				bestTourLength = a.trailLength(graph);
				bestTourOrder = a.trail.clone();
			}
		}
	}

	/**
	 * Clear trails after simulation
	 */
	private void clearTrails() {
		IntStream.range(0, numberOfCities)
				.forEach(i -> {
					IntStream.range(0, numberOfCities)
							.forEach(j -> trails[i][j] = c);
				});
	}



	@Override
	public void resultsReturned(Task task) {
		// Do something when the execution results are returned
	//	SimLog.println("task returned : " + task.getId());
		if(task_server.containsKey(task.getId())){

			int server = task_server.get(task.getId());
			//SimLog.println("task remove from server " + sever);
			task_queue.put(server, task_queue.get(server)-1);
		}
	}

}
