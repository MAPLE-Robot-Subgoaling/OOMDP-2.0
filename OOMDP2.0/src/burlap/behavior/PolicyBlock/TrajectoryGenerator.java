package burlap.behavior.PolicyBlock;

import java.util.ArrayList;
import java.util.HashMap;

import domain.PolicyBlock.PolicyBlockDomain;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.options.Option;
import burlap.behavior.singleagent.options.PolicyDefinedSubgoalOption;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.oomdp.core.State;

/**
 * Class: PolicyBlockPolicyGenerator
 * 
 * This class is designed to run the merge operations for the policy blocks algorithm
 * 
 * @author Tenji Tembo
 *
 */

public class TrajectoryGenerator {

	protected PolicyBlockDomain environ;
	protected ArrayList<EpisodeAnalysis> episodes = new ArrayList<EpisodeAnalysis>();
	protected ArrayList<TrajectoryPolicy> policies = new ArrayList<TrajectoryPolicy>();
	protected String outputPath;
	
	/**
	 * runSim() - controls the run of the Trajectory Merging in a domain.
	 * 
	 * Note:the printing of the merged policies can be cleaned up, because the are visualized in 
	 * 		showEpisodes() + also needs to be cleaned up as well
	 * 
	 * Description: 
	 * 		Main class for generating and running the policies needed for visualization purposes. The
	 * 		Episode Anaylsis Trajectories only contain the best possible path afer being generated from 
	 * 		a number of random policy goal states 
	 * 
	 * @param num - the number of policies to perform merging with
	 */
	@SuppressWarnings("rawtypes")
	public void runSim(int num){
		//set number of policies to merge
		int number = num;
		this.generatePolicies(number);
		
		EpisodeAnalysis[] input = new EpisodeAnalysis[number];
		for(int i = 0; i < input.length; i++)
			input[i] = environ.episodes.get(i);
		
		ArrayList <Object> output = unionSet(input);
		int max = 0;
		for(int i = 0; i < ((ArrayList)(output.get(0))).size(); i++)
		{
			if(max < ((Integer)((ArrayList)output.get(2)).get(i)))
				max = ((Integer)((ArrayList)output.get(2)).get(i));
			System.out.print("\n" + ((ArrayList)(output.get(1))).get(i) + "\n  Score: " + Integer.toString((Integer)((ArrayList)output.get(2)).get(i)));
			visualize((EpisodeAnalysis)(((ArrayList)(output.get(0))).get(i)));
		}
		
		System.out.println("Highest score: " + Integer.toString(max));
		
		System.out.println(((int)(Math.pow(2, number))-1) + " possible policies\n" + ((ArrayList)output.get(0)).size() + " resulting policies");
		
		System.out.print("\n" + ((ArrayList)(output.get(1))).get(((ArrayList)output.get(2)).indexOf(max)) + "\n  Score: " + Integer.toString((Integer)((ArrayList)output.get(2)).get(((ArrayList)output.get(2)).indexOf(max))));
		visualize((EpisodeAnalysis)(((ArrayList)(output.get(0))).get(((ArrayList)output.get(2)).indexOf(max))));
		
		
		//Converting to Policy Objects in order to visualize
		this.convertToPolicies();
		this.writeTrajectories();
		this.showEpisodes();
	}
	
	/**
	 * TrajectoryGenerator() - a TrajectoryGenerator Object
	 * @param outputPath - the string file path where the trajectories are saved.
	 */
	public TrajectoryGenerator(String outputPath){
		environ = new PolicyBlockDomain();
		
		if(!outputPath.contains("/"))
			outputPath = outputPath + "/";
		
		this.outputPath = outputPath;
	}
	
	/**
	 * generatePolicies() - Generates "number" iterations which contains 100 policies run via Q-Learning
	 * @param number - takes the number of desired trajectories stored in the object and generates them.  
	 */
	public void generatePolicies(int number){	
		environ.createEpisodes("policyBlocks", number);
	}
	
	/**
	 * visualize() - Displays an ASCII map of a GridWorld domain policy
	 * @param merged - the EA obj to be visualized.
	 * 
	 * Note:
	 * 		this can go or be cleaned up. Each of the merged policies are able to be visualized.  
	 */
	public void visualize(EpisodeAnalysis merged){
		//initializes an empty map array
		char[][] matrix = new char[11][11];
		for(int x = 0; x < 11; x++)
		{
			for(int y = 0; y <11; y++)
			{
				matrix[x][y] = ' ';
			}
		}
		//adds walls found in the standard map
		for(int y = 0; y < 11; y++)
			matrix[5][y] = 'X';
		for(int x = 0; x < 5; x++)
			matrix[x][5] = 'X';
		for(int x = 6; x < 11; x++)
			matrix[x][4] = 'X';
		matrix[5][1] = ' ';
		matrix[5][8] = ' ';
		matrix[1][5] = ' ';
		matrix[8][4] = ' ';
		//maps path traveled by agent
		for(int i = 0; i < merged.stateSequence.size()-1; i++)
		{
			matrix[merged.stateSequence.get(i).getObservableObjectAt(1).getValues().get(0).getDiscVal()][merged.stateSequence.get(i).getObservableObjectAt(1).getValues().get(1).getDiscVal()] = (merged.actionSequence.get(i).toString().charAt(0));
		}
		//displays map
		System.out.print("\n");
		for(int col = 10; col >= 0; col--)
		{
			//left-hand numbers
			System.out.print((col)%10/* + " "*/);
			for(int row = 0; row < 11; row++)
			{
				System.out.print(matrix[row][col]);
				//accounts of most fonts having spaces as half-size characters
				//if(matrix[row][col] == (' '))
					//System.out.print(" ");
				//extra space to allow reading rows of actions
				System.out.print(" ");
			}
			System.out.println();
		}
		//bottom numbers
		//System.out.print("   1  2  3  4  5  6  7  8  9  0  1 \n\n");
		System.out.print(" 0 1 2 3 4 5 6 7 8 9 0 \n\n");
	}
	
	//pushes the generated episodes to the GUI
	public void showEpisodes(){
		environ.visualize(outputPath);
	}
	
	/*
	 * Merge() - Merges two policies
	 * This is the merge function. Currently all it does is collect the first two merged
	 * generated by Q-Learning. Then it iterates state by state, checking to see if the agent visits the 
	 * same state in both policies. 
	 * 
	 * If so, it takes the state-action-reward set and writes it to the episode analysis object.
	*/
	public EpisodeAnalysis merge(EpisodeAnalysis e0, EpisodeAnalysis e1){
		
		//new blank episode for merging of the two policies
		EpisodeAnalysis merged = new EpisodeAnalysis();
		
			for(int i = 0; i < e0.stateSequence.size()-1; i++){
				Object s = e0.stateSequence.get(i).getObservableObjectAt(1).getValues(); 			//collect the first state
				for(int j = 0; j < e1.stateSequence.size()-1; j++){
					Object p = e1.stateSequence.get(j).getObservableObjectAt(1).getValues(); 		//collect the second state
					if(s.equals(p) && e0.actionSequence.get(i).toString().equals(e1.actionSequence.get(j).toString())){					//do we have a match
						
						//if you can figure out a better way to do this, by all means!!!
						if(e0.actionSequence.size() <= i){
							break;
						}else{								//push the state-action-reward set into merged
						merged.stateSequence.add(e0.stateSequence.get(i));
						merged.actionSequence.add(e0.actionSequence.get(i));
						merged.rewardSequence.add(e0.rewardSequence.get(i));
						}
					}
				}
			}
		
		episodes.add(merged);
		return merged;
	}
	
	/**
	 * unionSet() - returns the union set of merged policies
	 * @param set - the array set of all the merged trajectory objects needed to merge
	 * @return - the merged Arraylist of all the merged policies. 
	 */
	public ArrayList<Object>  unionSet(EpisodeAnalysis[] set){
		ArrayList<EpisodeAnalysis> result = new ArrayList<EpisodeAnalysis>();
		ArrayList<String> names = new ArrayList<String>();
		ArrayList<Integer> depth = new  ArrayList<Integer>();
		ArrayList<Integer> scores = new ArrayList<Integer>();
		ArrayList<Integer> label = new ArrayList<Integer>();
		//this loop seeks to exhaustively find the union set of the policies provided
		//the variable "i" is treated as though it were in binary with each bit representing whether a specific policy is part of the merged policy or not
		//"i" is split into the leading boolean non-zero bit and the other bits; the former is used to find the proper policy and the latter is used to find the already merged other policies (within the result array)
		//n is used to identify the leading bit easily
		//for example: when "i" is 11, this can be seen as 1011; the 1 indicates the obtain the fourth policy in the provided list and the 011 indicate the merge policy 4 with policy 3 (011 in decimal) in the resulting array
		
		//variable to store the number of binary digits used to represent previous merges
		int n = 1;
		EpisodeAnalysis temp;
		
		double max = Math.pow(2, set.length);
		
		System.out.println("Merges complete:         (of " + ((int)(max/100000) + " * 100K)"));
		
		for(int i = 1; i < max; i++)
		{
			if(i == Math.pow(2,  n))
				n++;
			//System.out.println(label.indexOf(i-Math.pow(2, n-1)));
			if(i-Math.pow(2, n-1) == 0)
			{
				label.add(i);
				result.add(set[n-1]);
				names.add(Integer.toString(n));
				depth.add(1);
				scores.add(result.get(label.indexOf(i)).stateSequence.size());
			}
			else if (label.indexOf(((int)(i-Math.pow(2, n-1)))) > -1/* && depth.get((int)(label.indexOf((int)(i-Math.pow(2, n-1))))) < 3*/) //toggling this bit causes longer pathways to be found, but not in as many policies
			{
				temp = merge(set[n-1], result.get((int)(label.indexOf((int)(i-Math.pow(2, n-1))))));
				if(temp.stateSequence.size() > 0)
				{
				label.add(i);
				result.add(merge(set[n-1], result.get((int)(label.indexOf((int)(i-Math.pow(2, n-1)))))));
				names.add(names.get((int)(label.indexOf((int)(i-Math.pow(2, n-1))))) + "+" + Integer.toString(n));
				depth.add(depth.get((int)(label.indexOf((int)(i-Math.pow(2, n-1))))) + 1);
				scores.add(result.get(label.indexOf(i)).stateSequence.size() * depth.get(label.indexOf(i)));
				}
			}
			
			if(i%100000 == 0)
				System.out.println(i/100000 + " * 100K");
			else if (i%1000 == 0)
				System.out.print(".");
				
			
		}
		ArrayList <Object> output = new ArrayList <Object>();
		output.add(result);
		output.add(names);
		output.add(scores);
		return output;
	}
	
	/**
	 * convertToPolicies() - converts the merges into policies
	 */
	public void convertToPolicies(){
		for(EpisodeAnalysis obj: episodes){
			if(obj.stateSequence.size() != 0){
				policies.add(new TrajectoryPolicy(obj));
			}
		}
	}
	
	/**
	 * writeTrajectories() - writes the trajectories merged to the file output path
	 */
	public void writeTrajectories(){
		int i = 0;
		
		for(TrajectoryPolicy p: policies){
			environ.writeTrajectory(p, outputPath + "merged-" + i);
			i++;
		}
	}
	
	/**
	 * createOptions() - takes the merged set of policies, and converts them into 
	 * Policy Based Options
	 * @return - An ArrayList of merged policies. 
	 */
	public ArrayList<Option> createOptions(){
		ArrayList<Option> options = new ArrayList<Option>();
		int i = 0;
		
		for(Policy p: policies){
			options.add(new PolicyDefinedSubgoalOption("option-"+i, p, new PolicyStateCheck(p)));
			i++;
		}
		
		
		for(Option o: options){
			System.out.println("\tOptions:" + o.hashCode());
		}
		
		return options;
	}
	
	/**
	 * Class - PolicyStateCheck - responsible for running the policy state check
	 * @author Tenji Tembo
	 *
	 */
	private class PolicyStateCheck implements StateConditionTest{

		Policy pol;
		
		public PolicyStateCheck(Policy p){
			this.pol = p;
		}
		
		@Override
		public boolean satisfies(State s) {
			return pol.isDefinedFor(s);
		}
		
	}
}
