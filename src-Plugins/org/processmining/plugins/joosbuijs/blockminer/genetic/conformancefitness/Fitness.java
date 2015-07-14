package org.processmining.plugins.joosbuijs.blockminer.genetic.conformancefitness;

import java.util.HashMap;


/**
 * This class contains fitness information about trees
 * 
 * @author jbuijs
 * 
 */
public class Fitness {
	private double fitness;
	private double replayFitness;
	private double behavioralAppropriateness;
	private double coverage;
	//TODO transform to subclass Hotspots
	private HashMap<Node, HotspotInfo> hotspots;

	public Fitness(double replayFitness, double behavioralFitness, double coverage) {
		setReplayFitness(replayFitness);
		setBehavioralAppropriateness(behavioralFitness);
		setCoverage(coverage);
	}

	public Fitness(double fitness, double replayFitness, double behavioralFitness, double coverage) {
		setFitness(fitness);
		setReplayFitness(replayFitness);
		setBehavioralAppropriateness(behavioralFitness);
		setCoverage(coverage);
		//setHotspots(hotspots);
	}

	public Fitness(Fitness fitness) {
		this(fitness.getFitness(), fitness.getReplayFitness(), fitness.getBehavioralAppropriateness(), fitness
				.getCoverage());
	}

	/**
	 * Initializes an empty fitness object
	 */
	public Fitness() {
		behavioralAppropriateness = 0;
		coverage=0;
		fitness=0;
		hotspots = new HashMap<Node, HotspotInfo>();
		replayFitness=0;
	}

	public double getBehavioralAppropriateness() {
		return behavioralAppropriateness;
	}

	public void setBehavioralAppropriateness(double behavioralAppropriateness) {
		this.behavioralAppropriateness = behavioralAppropriateness;
	}

	public double getCoverage() {
		return coverage;
	}

	public void setCoverage(double coverage) {
		this.coverage = coverage;
	}

	public double getReplayFitness() {
		return replayFitness;
	}

	public void setReplayFitness(double fitness) {
		this.replayFitness = fitness;
	}

	public void setFitness(double fitness) {
		this.fitness = fitness;
	}

	public double getFitness() {
		return fitness;
	}

	/**
	 * Set the hotspots
	 * 
	 * @param hotspots
	 *            Maps the severity of the hotspot to (Maps a node to (a pair of
	 *            nodes in between which the node should be added for
	 *            improvement)). Severity > 0 means add node between two
	 *            provided nodes, severity < 0 means remove node
	 */
	public void setHotspots(HashMap<Node, HotspotInfo> hotspots) {
		this.hotspots = hotspots;
	}
	
	public void addHotspot(Node node, HotspotInfo hotspotInfo){
		hotspots.put(node, hotspotInfo);
	}

	public HashMap<Node, HotspotInfo> getHotspots() {
		return hotspots;
	}
	
	public HotspotInfo getNewHotspotInfo(){
		return new HotspotInfo();		
	}
}
