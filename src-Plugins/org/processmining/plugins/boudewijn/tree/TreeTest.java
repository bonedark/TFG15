package org.processmining.plugins.boudewijn.tree;

import gnu.trove.list.TIntList;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.deckfour.xes.classification.XEventAndClassifier;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventLifeTransClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension.StandardModel;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.in.XesXmlGZIPParser;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.astar.algorithm.AStarThread.Canceller;
import org.processmining.plugins.boudewijn.tree.Node.Type;
import org.processmining.plugins.boudewijn.treebasedreplay.AStarAlgorithm;
import org.processmining.plugins.boudewijn.treebasedreplay.AbstractTreeBasedAStar;
import org.processmining.plugins.boudewijn.treebasedreplay.AbstractTreeBasedAStar.PerformanceType;
import org.processmining.plugins.boudewijn.treebasedreplay.AbstractTreeBasedAStar.VerboseLevel;
import org.processmining.plugins.boudewijn.treebasedreplay.TreeBasedAStarWithILP;
import org.processmining.plugins.boudewijn.treebasedreplay.TreeBasedAStarWithoutILP;
import org.processmining.plugins.boudewijn.treebasedreplay.TreeILPTail;
import org.processmining.plugins.boudewijn.treebasedreplay.tokencount.TokenCountAStarHybrid;
import org.processmining.plugins.boudewijn.treebasedreplay.tokencount.TokenCountAStarWithILP;
import org.processmining.plugins.boudewijn.treebasedreplay.tokencount.TokenCountAStarWithoutILP;
import org.processmining.plugins.boudewijn.treebasedreplay.tokencount.TokenCountHybridTail;
import org.processmining.plugins.boudewijn.treebasedreplay.tokencount.TokenCountILPTail;

public class TreeTest {

	public static Node makeRandom(XEventClasses classes, int bound) {
		Random random = new Random();
		ArrayList<XEventClass> cls = new ArrayList<XEventClass>(classes.getClasses());

		return makeRandom(cls, random, 0.2, bound);
	}

	private static Node makeRandom(List<XEventClass> cls, Random random, double leafProb, int maxDepth) {
		Node root;
		if (random.nextDouble() < leafProb || maxDepth == 1) {
			// random leaf node
			root = new Node(cls.get(random.nextInt(cls.size())));
		} else {
			Node left = makeRandom(cls, random, leafProb, maxDepth - 1);
			Node right = makeRandom(cls, random, leafProb, maxDepth - 1);
			Type type = Type.values()[random.nextInt(Type.values().length - 1)];
			if (type == Type.LOOP) {
				right = new Node((XEventClass) null);
			}
			root = new Node(type, left, right);
		}

		return root;
	}

	public static Node makeForType(XEventClasses classes, Type type, int depth) {
		Random random = new Random();
		ArrayList<XEventClass> cls = new ArrayList<XEventClass>(classes.getClasses());

		return makeForType(cls, random, type, depth);
	}

	private static Node makeForType(List<XEventClass> cls, Random random, Type type, int maxDepth) {
		Node root;
		if (maxDepth == 1) {
			// random leaf node
			root = new Node(cls.get(random.nextInt(cls.size())));
		} else {
			Node left = makeForType(cls, random, type, maxDepth - 1);
			Node right;
			if (type == Type.LOOP) {
				right = new Node((XEventClass) null);
			} else {
				right = makeForType(cls, random, type, maxDepth - 1);
			}

			root = new Node(type, left, right);
		}

		return root;
	}

	public static Node makeForTrace(XEventClasses classes, XTrace trace, int i) {
		Node root;
		if (i < trace.size() - 1) {
			Node right = makeForTrace(classes, trace, i + 1);
			Node left = new Node(classes.getClassOf(trace.get(i)));
			root = new Node(Type.SEQ, left, right);
		} else {
			root = new Node(classes.getClassOf(trace.get(i)));
		}
		return root;
	}

	public static void main(String[] args) throws Exception {

		System.out.println("LP=" + System.getProperty("java.library.path"));

		//		String[][] l = new String[][] { { "a", "b", "c", "a" } };
		//		XLog log = createLog(l);

		//		XLog log = createInterleavedLog(500, "a", "a", "b", "b", "c", "d", "e", "f");

		XesXmlGZIPParser parser = new XesXmlGZIPParser();
		XLog org = parser.parse(new File("C:/Temp/treetest/logs/a12Unpacked.xes.gz")).get(0);

		XEventAndClassifier classifier = new XEventAndClassifier(new XEventNameClassifier(),
				new XEventLifeTransClassifier());
		XLogInfo info = XLogInfoFactory.createLogInfo(org, classifier);
		XLog log = org;

		//XLog log = createLog(new String[][] { { "a", "b", "c", "d", "e", "f" } });
		//		XLog log = createLog(new String[][] { { "a", "b", "c" } });
		//XLog log = createLog(new String[][] { { "a" }, { "b", "c", "a", "b", "c", "b", "c" }, { "a", "b", "b" } });
		//XLog log = createLog(new String[][] {{"a"}, {"b" }, {"a","b" }, {"b", "a"}});
		//		XLog log = createLog(new String[][] { { "a", "a", "a", "b", "b", "a" } });
		System.out.println("Log size: " + log.size());

		//		XLogInfo info = XLogInfoFactory.createLogInfo(log, new XEventNameClassifier());

		PerformanceType perf = PerformanceType.MEMORYEFFICIENT;
		boolean traceByTrace = true;

		int reps = (args.length > 0 ? Integer.parseInt(args[0]) : traceByTrace ? 1 : 500);
		VerboseLevel level = (args.length > 1 ? VerboseLevel.valueOf(args[1]) : traceByTrace ? VerboseLevel.ALL
				: VerboseLevel.SOME);

		//		Set<Node> forrest = getTrees("c:/temp/generation72.log", info.getEventClasses());

		Collection<Node> forrest = new ArrayList<Node>();
		int bmin = 3;
		int bmax = 12;
		for (int i = 0; i < reps; i++) {
			if (traceByTrace) {
				forrest.add(getTree(info));
			} else {
				forrest.add(makeRandom(info.getEventClasses(), bmin + (int) ((bmax - bmin) * i * 1.0 / reps)));
			}
		}

		if (traceByTrace) {
			for (int i = 0; i < log.size(); i++) {
				System.out.println("Trace: " + i);
				XLog newLog = XFactoryRegistry.instance().currentDefault().createLog();
				newLog.add(log.get(i));

				info = XLogInfoFactory.createLogInfo(newLog, classifier);
				doReplay(newLog, info, level, perf, forrest);
			}
		} else {

			doReplay(log, info, level, perf, forrest);
		}

	}

	private static Set<Node> getTrees(String filename, XEventClasses classes) throws IOException {
		File f = new File(filename);
		BufferedReader r = new BufferedReader(new FileReader(f));

		Set<Node> forrest = new TreeSet<Node>(new Comparator<Node>() {

			public int compare(Node o1, Node o2) {
				int i = o2.countNodes() - o1.countNodes();
				if (i == 0) {
					return -o2.toString().compareTo(o1.toString());
				} else {
					return -i;
				}
			}

		});

		String tree = r.readLine();
		while (tree != null) {
			Node n = Node.fromString(tree.trim() + " ", classes);
			forrest.add(n);

			tree = r.readLine();
		}
		return forrest;
	}

	private static Node getTree(XLogInfo info) {
		//			Node root = Node
		//					.fromString(
		//							"AND( AND( AND( SEQ( OR( SEQ( LEAF: b , LEAF: e ) , OR( LEAF: d , LEAF: f ) ) , SEQ( LOOP( LEAF: b , LEAF: EXIT ) , XOR( LEAF: f , LEAF: a ) ) ) , SEQ( AND( AND( LEAF: d , LEAF: e ) , LOOP( LEAF: d , LEAF: EXIT ) ) , AND( LEAF: d , SEQ( LEAF: e , LEAF: c ) ) ) ) , AND( SEQ( XOR( AND( LEAF: a , LEAF: e ) , OR( LEAF: e , LEAF: f ) ) , LEAF: c ) , SEQ( OR( AND( LEAF: a , LEAF: e ) , LOOP( LEAF: f , LEAF: EXIT ) ) , LOOP( SEQ( LEAF: b , LEAF: b ) , LEAF: EXIT ) ) ) ) , SEQ( SEQ( OR( LOOP( SEQ( LEAF: e , LEAF: b ) , LEAF: EXIT ) , SEQ( XOR( LEAF: c , LEAF: e ) , LOOP( LEAF: e , LEAF: EXIT ) ) ) , XOR( AND( OR( LEAF: b , LEAF: b ) , LOOP( LEAF: e , LEAF: EXIT ) ) , OR( OR( LEAF: d , LEAF: e ) , LOOP( LEAF: e , LEAF: EXIT ) ) ) ) , XOR( LOOP( OR( LOOP( LEAF: f , LEAF: EXIT ) , AND( LEAF: e , LEAF: d ) ) , LEAF: EXIT ) , AND( AND( SEQ( LEAF: c , LEAF: b ) , XOR( LEAF: d , LEAF: c ) ) , AND( XOR( LEAF: f , LEAF: e ) , LOOP( LEAF: c , LEAF: EXIT ) ) ) ) ) )",
		//							info.getEventClasses());

		//Node root = makeForType(info.getEventClasses(), Type.SEQ, 1);

		//		Node root = Node.fromString("AND( XOR( LEAF: c , LEAF: f ) , LOOP( LEAF: b , LEAF: EXIT ) )",
		//				info.getEventClasses());

		//		Node root = Node
		//		.fromString(
		//				"XOR( LOOP( OR( OR( AND( XOR( LEAF: f , LEAF: e ) , SEQ( LEAF: b , LEAF: c ) ) , SEQ( SEQ( LEAF: e , LEAF: a ) , OR( LEAF: c , LEAF: d ) ) ) , SEQ( SEQ( LOOP( LEAF: e , LEAF: EXIT ) , AND( LEAF: c , LEAF: b ) ) , LOOP( LEAF: d , LEAF: EXIT ) ) ) , LEAF: EXIT ) , OR( LOOP( OR( AND( LOOP( LEAF: e , LEAF: EXIT ) , OR( LEAF: a , LEAF: f ) ) , XOR( SEQ( LEAF: d , LEAF: f ) , LEAF: c ) ) , LEAF: EXIT ) , LEAF: f ) )",
		//				info.getEventClasses());
		//		Node root = Node
		//		.fromString(
		//				"OR( LEAF: b , SEQ( SEQ( AND( OR( XOR( LEAF: d , LEAF: b ) , LEAF: f ) , LEAF: b ) , SEQ( SEQ( LEAF: e , SEQ( LEAF: d , LEAF: e ) ) , OR( AND( LEAF: c , LEAF: d ) , OR( LEAF: b , LEAF: e ) ) ) ) , LEAF: f ) )",
		//				info.getEventClasses());

		//		Node root = Node.fromString("OR( LEAF: a , SEQ( SEQ( LEAF: c , AND( LEAF: a , LEAF: b ) ) , LEAF: c ) )",
		//				info.getEventClasses());

		Node root = Node
				.fromString(
						//a12 Tree:
						"SEQ( LEAF: SSSTTTAAARRRTTT+complete , SEQ( SEQ( SEQ( LEAF: A+complete , XOR( SEQ( LEAF: B+complete , SEQ( AND( SEQ( LEAF: D+complete , LEAF: E+complete ) , LEAF: F+complete ) , LEAF: J+complete ) ) , SEQ( LEAF: C+complete , SEQ( XOR( LEAF: G+complete , SEQ( LEAF: H+complete , LEAF: I+complete ) ) , LEAF: K+complete ) ) ) ) , LEAF: L+complete ) , LEAF: EEENNNDDD+complete ) )",
						info.getEventClasses());

		//		Node root = Node.fromString(
		//				"AND( SEQ( SEQ( LEAF: b , LEAF: b ) , LEAF: g ) , LOOP( OR( LEAF: c , LEAF: a ) , LEAF: EXIT ) )",
		//				info.getEventClasses());

		//		Node root = makeRandom(info.getEventClasses());

		//		Node root = Node
		//				.fromString(
		//						"AND( AND( LOOP( LEAF: b , LEAF: EXIT ) , LOOP( XOR( LEAF: e , LEAF: b ) , LEAF: EXIT ) ) , SEQ( LEAF: e , OR( LEAF: a , SEQ( LEAF: f , LEAF: a ) ) ) )",
		//						info.getEventClasses());

		//		Node root = Node
		//				.fromString(
		//						"OR( LOOP( SEQ( LEAF: g , XOR( LOOP( AND( LEAF: b , AND( LEAF: b , LEAF: f ) ) , LEAF: EXIT ) , XOR( SEQ( XOR( LEAF: g , LEAF: a ) , AND( LEAF: a , LEAF: d ) ) , SEQ( SEQ( LEAF: a , LEAF: g ) , LOOP( LEAF: d , LEAF: EXIT ) ) ) ) ) , LEAF: EXIT ) , OR( XOR( LOOP( LEAF: d , LEAF: EXIT ) , LEAF: f ) , SEQ( OR( LEAF: f , SEQ( LOOP( XOR( OR( LEAF: a , LEAF: g ) , OR( LEAF: f , LEAF: f ) ) , LEAF: EXIT ) , LEAF: d ) ) , SEQ( LEAF: g , LEAF: e ) ) ) )",
		//						info.getEventClasses());

		//		Node root = Node
		//				.fromString(
		//						"OR( SEQ( SEQ( OR( LEAF: g , LEAF: b ) , OR( LEAF: a , LEAF: f ) ) , SEQ( AND( LEAF: e , LEAF: d ) , SEQ( LEAF: f , LEAF: d ) ) ) , XOR( LEAF: a , LEAF: g ) )",
		//						info.getEventClasses());

		//		BIG NASTY TREE (177 nodes0
		//		Node root = Node
		//				.fromString(
		//						"AND( OR( LOOP( XOR( XOR( OR( XOR( XOR( LEAF: d , LEAF: d ) , OR( LEAF: f , LEAF: e ) ) , SEQ( LEAF: a , LEAF: f ) ) , AND( AND( LEAF: c , XOR( LEAF: e , LEAF: c ) ) , LOOP( AND( LEAF: b , LEAF: d ) , LEAF: EXIT ) ) ) , XOR( OR( SEQ( LEAF: b , LEAF: d ) , LEAF: b ) , OR( SEQ( SEQ( LEAF: e , LEAF: b ) , AND( LEAF: e , LEAF: c ) ) , AND( OR( LEAF: b , LEAF: a ) , AND( LEAF: a , LEAF: a ) ) ) ) ) , LEAF: EXIT ) , XOR( AND( AND( LOOP( OR( LEAF: d , LEAF: b ) , LEAF: EXIT ) , SEQ( LEAF: d , OR( SEQ( LEAF: a , LEAF: a ) , LOOP( LEAF: d , LEAF: EXIT ) ) ) ) , OR( OR( XOR( SEQ( LEAF: d , LEAF: e ) , LOOP( LEAF: d , LEAF: EXIT ) ) , SEQ( AND( LEAF: a , LEAF: f ) , AND( LEAF: f , LEAF: a ) ) ) , SEQ( LEAF: c , XOR( SEQ( LEAF: c , LEAF: f ) , XOR( LEAF: d , LEAF: d ) ) ) ) ) , XOR( LOOP( OR( OR( OR( LEAF: e , LEAF: a ) , OR( LEAF: e , LEAF: c ) ) , XOR( LOOP( LEAF: a , LEAF: EXIT ) , XOR( LEAF: a , LEAF: f ) ) ) , LEAF: EXIT ) , LEAF: c ) ) ) , AND( SEQ( SEQ( LOOP( LEAF: b , LEAF: EXIT ) , LOOP( OR( LEAF: b , XOR( LEAF: b , AND( LEAF: c , LEAF: b ) ) ) , LEAF: EXIT ) ) , LOOP( AND( XOR( OR( OR( LEAF: d , LEAF: e ) , SEQ( LEAF: a , LEAF: d ) ) , XOR( XOR( LEAF: e , LEAF: f ) , LEAF: c ) ) , OR( LEAF: e , LOOP( OR( LEAF: c , LEAF: a ) , LEAF: EXIT ) ) ) , LEAF: EXIT ) ) , OR( LOOP( OR( LOOP( SEQ( SEQ( LEAF: d , LEAF: d ) , LOOP( LEAF: a , LEAF: EXIT ) ) , LEAF: EXIT ) , OR( AND( XOR( LEAF: e , LEAF: c ) , OR( LEAF: a , LEAF: d ) ) , OR( LOOP( LEAF: c , LEAF: EXIT ) , OR( LEAF: b , LEAF: a ) ) ) ) , LEAF: EXIT ) , LEAF: a ) ) )",
		//						info.getEventClasses());

		// FIXME: this tree shows a bug in the old implementation, since it does not find the correct alignment for the trace <a,b,c,e,f,d>
		//		Node root = Node
		//				.fromString(
		//						"SEQ( LEAF: e , OR( LEAF: e , SEQ( LOOP( LEAF: b , LEAF: EXIT ) , SEQ( LOOP( SEQ( LEAF: c , LEAF: e ) , LEAF: EXIT ) , SEQ( LOOP( LEAF: b , LEAF: EXIT ) , SEQ( LEAF: f , LEAF: e ) ) ) ) ) )",
		//						info.getEventClasses());

		//		Node root = Node.fromString("LOOP( AND( LOOP( LEAF: a , LEAF: EXIT ) , LEAF: b ) , LEAF: EXIT )",
		//				info.getEventClasses());

		//		Node root = Node.fromString(
		//				"SEQ( LOOP( SEQ( SEQ( LEAF: d , LEAF: a ) , LOOP( LEAF: c , LEAF: EXIT ) ) , LEAF: EXIT ) , LEAF: b )",
		//				info.getEventClasses());
		//		Node root = Node.fromString("OR( AND( LEAF: a , LEAF: e ) , LOOP( LEAF: a , LEAF: EXIT ) )",
		//				info.getEventClasses());
		//			Node root = Node.fromString("SEQ( LOOP( LEAF: a , LEAF: EXIT ) , LEAF: b )", info.getEventClasses());

		// Hard tree:
		//			Node root = Node
		//					.fromString(
		//							"SEQ( SEQ( LOOP( LEAF: b , LEAF: EXIT ) , AND( LEAF: b , LEAF: b ) ) , LOOP( AND( LEAF: f , LEAF: a ) , LEAF: EXIT ) )",
		//							info.getEventClasses());

		//				Node root = Node.fromString("LOOP( SEQ( LEAF: c , LEAF: b ) , LEAF: EXIT ) ", info.getEventClasses());

		//Some more hard trees (11-11-2011 15:34)
		//			Node root = Node
		//					.fromString(
		//							"AND( LOOP( SEQ( LOOP( LEAF: c , LEAF: EXIT ) , LOOP( SEQ( LOOP( LEAF: a , LEAF: EXIT ) , LOOP( LEAF: c , LEAF: EXIT ) ) , LEAF: EXIT ) ) , LEAF: EXIT ) , AND( AND( AND( SEQ( LOOP( LEAF: d , LEAF: EXIT ) , AND( LEAF: b , LEAF: c ) ) , XOR( AND( LEAF: e , LEAF: a ) , LOOP( LEAF: e , LEAF: EXIT ) ) ) , AND( AND( XOR( LEAF: b , LEAF: e ) , AND( LEAF: c , LEAF: e ) ) , LOOP( LEAF: d , LEAF: EXIT ) ) ) , AND( XOR( SEQ( LOOP( LEAF: e , LEAF: EXIT ) , SEQ( LEAF: d , LEAF: b ) ) , LOOP( XOR( LEAF: a , LEAF: a ) , LEAF: EXIT ) ) , LOOP( LEAF: d , LEAF: EXIT ) ) ) )",
		//							info.getEventClasses());
		//			Node root = Node.fromString("XOR( LOOP( SEQ( LEAF: a , LEAF: f ) , LEAF: EXIT ) , XOR( LEAF: b , SEQ( LEAF: c , LOOP( LOOP( AND( LEAF: e , LEAF: d ) , LEAF: EXIT ) , LEAF: EXIT ) ) ) )",info.getEventClasses());
		//			1 SEQ( SEQ( LEAF: A+complete , LOOP( SEQ( LEAF: C+complete , LEAF: D+complete ) , LEAF: EXIT ) ) , AND( LEAF: B+complete , AND( LEAF: F+complete , LEAF: E+complete ) ) )
		//			1 XOR( AND( LEAF: A+complete , LEAF: F+complete ) , AND( LOOP( AND( LEAF: B+complete , LEAF: E+complete ) , LEAF: EXIT ) , XOR( LEAF: C+complete , LEAF: D+complete ) ) )
		return root;
	}

	public static void doReplay(XLog log, XLogInfo info, VerboseLevel level, PerformanceType perf,
			Collection<Node> forrest) throws Exception {

		Canceller c = new Canceller() {

			public boolean isCancelled() {
				return false;
			}

		};

		Map<XEventClass, Integer> act2cost = new HashMap<XEventClass, Integer>();
		for (XEventClass cls : info.getEventClasses().getClasses()) {
			act2cost.put(cls, 1);
		}

		AStarAlgorithm algorithm = new AStarAlgorithm(log, info.getEventClasses(), act2cost);
		System.out.println("Different traces: " + algorithm.getDifferentTraceCount());

		int size = 9;
		//                                  OwI   NwI    NwI gc NwI gm OwoI   NwoI  NwoI G
		boolean perform[] = new boolean[] { false, true, true, false, false, false, false, false, false };
		String names[] = new String[] { "Old with ILP", "New with ILP", "New with ILP (Greedy, CPU)",
				"New with ILP (Greedy, MEM)", "Old w/o ILP", "New w/o ILP", "New w/o ILP (Greedy)",
				"Hybrid w ILP (Greedy, MEM)", "Hybrid w ILP (Greedy, CPU)" };
		long[] time = new long[size];
		double[] statesPerSec = new double[size];
		double[] cost = new double[size];
		long[] visitedNodes = new long[size];
		int[] maxNodes = new int[size];
		long[] visitedArcs = new long[size];
		long[] statesStored = new long[size];
		int[] solvedLPs = new int[size];
		int[] generatedLPs = new int[size];
		double[] fitness = new double[size];

		int r = 0;
		int reps = forrest.size();
		for (Node root : forrest) {

			System.out.println("------------------------------------");
			System.out.println("Iteration: " + (++r) + "/" + reps);

			System.out.println("Number of nodes: " + root.getPreorder().size());
			System.out.println("Original tree:   " + root);

			MutatableTree orgTree = new MutatableTree(root);

			MutatableTree mtree;
			AbstractTreeBasedAStar<?, ?, ?> aStar;
			long start;
			long end;
			Map<TIntList, TIntSet> marking2modelmove;
			TObjectIntHashMap<TIntList> marking2visitCount;
			Map<TIntList, TIntSet> enabled2modelmove;
			TObjectIntHashMap<TIntList> enabled2visitCount;
			int test = 0;
			if (perform[test]) {
				TreeILPTail.LPDerived = 0;
				TreeILPTail.LPSolved = 0;
				mtree = new MutatableTree(orgTree);
				System.out.println("- " + names[test] + " --------------------------------");
				enabled2modelmove = new HashMap<TIntList, TIntSet>();
				enabled2visitCount = new TObjectIntHashMap<TIntList>();
				Map<Node, Integer> node2cost = new HashMap<Node, Integer>();
				for (Node n : mtree.getRoot().getPreorder()) {
					if (n.getClazz() != null) {
						node2cost.put(n, 1);
					}
				}
				aStar = new TreeBasedAStarWithILP(algorithm, c, mtree.getRoot(), node2cost, enabled2modelmove,
						enabled2visitCount);
				aStar.setType(perf);
				start = System.currentTimeMillis();
				fitness[test] = aStar.run(level, Double.MAX_VALUE);
				end = System.currentTimeMillis();
				time[test] += (end - start);
				cost[test] += fitness[test];
				visitedNodes[test] += aStar.getQueuedStateCount();
				maxNodes[test] = Math.max(maxNodes[test], (int) aStar.getQueuedStateCount());
				visitedArcs[test] += aStar.getTraversedArcCount();
				statesPerSec[test] += end == start ? 0 : aStar.getQueuedStateCount() / ((end - start) / 1000.0);
				statesStored[test] += aStar.getNumStoredStates();
				solvedLPs[test] += TreeILPTail.LPSolved;
				generatedLPs[test] += TreeILPTail.LPDerived;
			}
			test++;
			if (perform[test]) {
				TokenCountILPTail.LPDerived = 0;
				TokenCountILPTail.LPSolved = 0;
				mtree = new MutatableTree(orgTree);
				System.out.println();
				System.out.println("- " + names[test] + " --------------------------------");
				marking2modelmove = new HashMap<TIntList, TIntSet>();
				marking2visitCount = new TObjectIntHashMap<TIntList>();
				Map<Node, Integer> node2cost = new HashMap<Node, Integer>();
				for (Node n : mtree.getRoot().getPreorder()) {
					if (n.getClazz() != null) {
						node2cost.put(n, 1);
					}
				}

				aStar = new TokenCountAStarWithILP(algorithm, c, mtree.getRoot(), node2cost, marking2modelmove,
						marking2visitCount, false);
				aStar.setType(perf);
				start = System.currentTimeMillis();
				fitness[test] = aStar.run(level, Double.MAX_VALUE);
				end = System.currentTimeMillis();
				time[test] += (end - start);
				cost[test] += fitness[test];
				visitedNodes[test] += aStar.getQueuedStateCount();
				maxNodes[test] = Math.max(maxNodes[test], (int) aStar.getQueuedStateCount());
				visitedArcs[test] += aStar.getTraversedArcCount();
				statesPerSec[test] += end == start ? 0 : aStar.getQueuedStateCount() / ((end - start) / 1000.0);
				statesStored[test] += aStar.getNumStoredStates();
				solvedLPs[test] += TokenCountILPTail.LPSolved;
				generatedLPs[test] += TokenCountILPTail.LPDerived;
			}
			test++;
			if (perform[test]) {
				TokenCountILPTail.LPDerived = 0;
				TokenCountILPTail.LPSolved = 0;
				mtree = new MutatableTree(orgTree);
				System.out.println();
				System.out.println("- " + names[test] + " --------------------------------");
				marking2modelmove = new HashMap<TIntList, TIntSet>();
				marking2visitCount = new TObjectIntHashMap<TIntList>();
				Map<Node, Integer> node2cost = new HashMap<Node, Integer>();
				for (Node n : mtree.getRoot().getPreorder()) {
					if (n.getClazz() != null) {
						node2cost.put(n, 1);
					}
				}

				aStar = new TokenCountAStarWithILP(algorithm, c, mtree.getRoot(), node2cost, marking2modelmove,
						marking2visitCount, true);
				aStar.setType(PerformanceType.CPUEFFICIENT);
				start = System.currentTimeMillis();
				fitness[test] = aStar.run(level, Double.MAX_VALUE);
				end = System.currentTimeMillis();
				time[test] += (end - start);
				cost[test] += fitness[test];
				visitedNodes[test] += aStar.getQueuedStateCount();
				maxNodes[test] = Math.max(maxNodes[test], (int) aStar.getQueuedStateCount());
				visitedArcs[test] += aStar.getTraversedArcCount();
				statesPerSec[test] += end == start ? 0 : aStar.getQueuedStateCount() / ((end - start) / 1000.0);
				statesStored[test] += aStar.getNumStoredStates();
				solvedLPs[test] += TokenCountILPTail.LPSolved;
				generatedLPs[test] += TokenCountILPTail.LPDerived;
			}
			test++;
			if (perform[test]) {
				TokenCountILPTail.LPDerived = 0;
				TokenCountILPTail.LPSolved = 0;
				mtree = new MutatableTree(orgTree);
				System.out.println();
				System.out.println("- " + names[test] + " --------------------------------");
				marking2modelmove = new HashMap<TIntList, TIntSet>();
				marking2visitCount = new TObjectIntHashMap<TIntList>();
				Map<Node, Integer> node2cost = new HashMap<Node, Integer>();
				for (Node n : mtree.getRoot().getPreorder()) {
					if (n.getClazz() != null) {
						node2cost.put(n, 1);
					}
				}

				aStar = new TokenCountAStarWithILP(algorithm, c, mtree.getRoot(), node2cost, marking2modelmove,
						marking2visitCount, true);
				aStar.setType(PerformanceType.MEMORYEFFICIENT);
				start = System.currentTimeMillis();
				fitness[test] = aStar.run(level, Double.MAX_VALUE);
				end = System.currentTimeMillis();
				time[test] += (end - start);
				cost[test] += fitness[test];
				visitedNodes[test] += aStar.getQueuedStateCount();
				maxNodes[test] = Math.max(maxNodes[test], (int) aStar.getQueuedStateCount());
				visitedArcs[test] += aStar.getTraversedArcCount();
				statesPerSec[test] += end == start ? 0 : aStar.getQueuedStateCount() / ((end - start) / 1000.0);
				statesStored[test] += aStar.getNumStoredStates();
				solvedLPs[test] += TokenCountILPTail.LPSolved;
				generatedLPs[test] += TokenCountILPTail.LPDerived;
			}
			test++;
			if (perform[test]) {
				mtree = new MutatableTree(orgTree);
				System.out.println();
				System.out.println("- " + names[test] + " -----------------------------");
				enabled2modelmove = new HashMap<TIntList, TIntSet>();
				enabled2visitCount = new TObjectIntHashMap<TIntList>();
				Map<Node, Integer> node2cost = new HashMap<Node, Integer>();
				for (Node n : mtree.getRoot().getPreorder()) {
					if (n.getClazz() != null) {
						node2cost.put(n, 1);
					}
				}
				aStar = new TreeBasedAStarWithoutILP(algorithm, c, mtree.getRoot(), node2cost, enabled2modelmove,
						enabled2visitCount);
				aStar.setType(perf);
				start = System.currentTimeMillis();
				fitness[test] = aStar.run(level, Double.MAX_VALUE);
				end = System.currentTimeMillis();
				time[test] += (end - start);
				cost[test] += fitness[test];
				visitedNodes[test] += aStar.getQueuedStateCount();
				maxNodes[test] = Math.max(maxNodes[test], (int) aStar.getQueuedStateCount());
				visitedArcs[test] += aStar.getTraversedArcCount();
				statesPerSec[test] += end == start ? 0 : aStar.getQueuedStateCount() / ((end - start) / 1000.0);
				statesStored[test] += aStar.getNumStoredStates();
			}
			test++;
			if (perform[test]) {
				mtree = new MutatableTree(orgTree);
				System.out.println();
				System.out.println("- " + names[test] + " -----------------------------");

				marking2modelmove = new HashMap<TIntList, TIntSet>();
				marking2visitCount = new TObjectIntHashMap<TIntList>();
				Map<Node, Integer> node2cost = new HashMap<Node, Integer>();
				for (Node n : mtree.getRoot().getPreorder()) {
					if (n.getClazz() != null) {
						node2cost.put(n, 1);
					}
				}
				aStar = new TokenCountAStarWithoutILP(algorithm, c, mtree.getRoot(), node2cost, marking2modelmove,
						marking2visitCount, false);
				aStar.setType(perf);
				start = System.currentTimeMillis();
				fitness[test] = aStar.run(level, Double.MAX_VALUE);
				end = System.currentTimeMillis();
				time[test] += (end - start);
				cost[test] += fitness[test];
				visitedNodes[test] += aStar.getQueuedStateCount();
				maxNodes[test] = Math.max(maxNodes[test], (int) aStar.getQueuedStateCount());
				visitedArcs[test] += aStar.getTraversedArcCount();
				statesPerSec[test] += end == start ? 0 : aStar.getQueuedStateCount() / ((end - start) / 1000.0);
				statesStored[test] += aStar.getNumStoredStates();

			}
			test++;
			if (perform[test]) {
				mtree = new MutatableTree(orgTree);
				System.out.println();
				System.out.println("- " + names[test] + " -----------------------------");

				marking2modelmove = new HashMap<TIntList, TIntSet>();
				marking2visitCount = new TObjectIntHashMap<TIntList>();
				Map<Node, Integer> node2cost = new HashMap<Node, Integer>();
				for (Node n : mtree.getRoot().getPreorder()) {
					if (n.getClazz() != null) {
						node2cost.put(n, 1);
					}
				}
				aStar = new TokenCountAStarWithoutILP(algorithm, c, mtree.getRoot(), node2cost, marking2modelmove,
						marking2visitCount, true);
				aStar.setType(perf);
				start = System.currentTimeMillis();
				fitness[test] = aStar.run(level, Double.MAX_VALUE);
				end = System.currentTimeMillis();
				time[test] += (end - start);
				cost[test] += fitness[test];
				visitedNodes[test] += aStar.getQueuedStateCount();
				maxNodes[test] = Math.max(maxNodes[test], (int) aStar.getQueuedStateCount());
				visitedArcs[test] += aStar.getTraversedArcCount();
				statesPerSec[test] += end == start ? 0 : aStar.getQueuedStateCount() / ((end - start) / 1000.0);
				statesStored[test] += aStar.getNumStoredStates();

			}
			test++;
			if (perform[test]) {
				TokenCountHybridTail.LPDerived = 0;
				TokenCountHybridTail.LPSolved = 0;
				mtree = new MutatableTree(orgTree);
				System.out.println();
				System.out.println("- " + names[test] + " -----------------------------");

				marking2modelmove = new HashMap<TIntList, TIntSet>();
				marking2visitCount = new TObjectIntHashMap<TIntList>();
				Map<Node, Integer> node2cost = new HashMap<Node, Integer>();
				for (Node n : mtree.getRoot().getPreorder()) {
					if (n.getClazz() != null) {
						node2cost.put(n, 1);
					}
				}
				aStar = new TokenCountAStarHybrid(algorithm, c, mtree.getRoot(), node2cost, marking2modelmove,
						marking2visitCount, true);
				aStar.setType(PerformanceType.MEMORYEFFICIENT);
				start = System.currentTimeMillis();
				fitness[test] = aStar.run(level, Double.MAX_VALUE);
				end = System.currentTimeMillis();
				time[test] += (end - start);
				cost[test] += fitness[test];
				visitedNodes[test] += aStar.getQueuedStateCount();
				maxNodes[test] = Math.max(maxNodes[test], (int) aStar.getQueuedStateCount());
				visitedArcs[test] += aStar.getTraversedArcCount();
				statesPerSec[test] += end == start ? 0 : aStar.getQueuedStateCount() / ((end - start) / 1000.0);
				statesStored[test] += aStar.getNumStoredStates();
				solvedLPs[test] += TokenCountHybridTail.LPSolved;
				generatedLPs[test] += TokenCountHybridTail.LPDerived;

			}
			test++;
			if (perform[test]) {
				TokenCountHybridTail.LPDerived = 0;
				TokenCountHybridTail.LPSolved = 0;
				mtree = new MutatableTree(orgTree);
				System.out.println();
				System.out.println("- " + names[test] + " -----------------------------");

				marking2modelmove = new HashMap<TIntList, TIntSet>();
				marking2visitCount = new TObjectIntHashMap<TIntList>();
				Map<Node, Integer> node2cost = new HashMap<Node, Integer>();
				for (Node n : mtree.getRoot().getPreorder()) {
					if (n.getClazz() != null) {
						node2cost.put(n, 1);
					}
				}
				aStar = new TokenCountAStarHybrid(algorithm, c, mtree.getRoot(), node2cost, marking2modelmove,
						marking2visitCount, true);
				aStar.setType(PerformanceType.CPUEFFICIENT);
				start = System.currentTimeMillis();
				fitness[test] = aStar.run(level, Double.MAX_VALUE);
				end = System.currentTimeMillis();
				time[test] += (end - start);
				cost[test] += fitness[test];
				visitedNodes[test] += aStar.getQueuedStateCount();
				maxNodes[test] = Math.max(maxNodes[test], (int) aStar.getQueuedStateCount());
				visitedArcs[test] += aStar.getTraversedArcCount();
				statesPerSec[test] += end == start ? 0 : aStar.getQueuedStateCount() / ((end - start) / 1000.0);
				statesStored[test] += aStar.getNumStoredStates();
				solvedLPs[test] += TokenCountHybridTail.LPSolved;
				generatedLPs[test] += TokenCountHybridTail.LPDerived;

			}
			boolean ok = true;
			for (int k = 0; ok && k < size; k++) {
				if (perform[k]) {
					for (int j = 0; ok && j < size; j++) {
						if (perform[j]) {
							if (fitness[j] != fitness[k]) {
								System.err.println("ERROR in fitness");
								for (test = 0; test < size; test++) {
									if (perform[test]) {
										System.err.println("  fitness " + names[test] + " : " + fitness[test]);
									}
								}
								ok = false;
							}
						}
					}
				}
			}

			//			mtree.reduceBehavior();
			//			System.out.println();
			//			System.out.println("Reduced tree:    " + mtree.getRoot());
			//			System.out.println(TokenCountHeadCompressor.CALLS + " / " + TokenCountHeadCompressor.FALSEANSWER + " / "
			//					+ TokenCountHeadCompressor.FALSEONHASH);

			System.out.println();
			for (int i = 0; i < size; i++) {
				if (!perform[i]) {
					continue;
				}
				System.out.println("-- " + names[i] + " ------------------------------------");
				System.out.println("  Average cost per trace:  " + cost[i] / (1.0 * reps * log.size()));
				System.out.println("  Total time:              " + ((time[i]) / (1000.0)) + " seconds");
				System.out.println("  Time per run:            " + ((time[i]) / (reps * 1000.0)) + " seconds");
				System.out.println("  States stored:           " + (statesStored[i] / (reps * 1.0)));
				System.out.println("  States visited:          "
						+ String.format("%12.2f", visitedNodes[i] / (reps * 1.0)));
				System.out.println("  Max. States visited:     " + maxNodes[i]);
				System.out.println("  Arcs traversed:          "
						+ String.format("%12.2f", visitedArcs[i] / (reps * 1.0)));
				System.out.println("  Average states per sec.  " + (statesPerSec[i] / (reps * 1.0)));
				System.out.println("  LP solved / LP derived   " + solvedLPs[i] + " / " + generatedLPs[i]);
				//			System.out.println("" + TokenCountHeadCompressor.FALSEONHASH + " / " + TokenCountHeadCompressor.FALSEANSWER
				//					+ " / " + TokenCountHeadCompressor.CALLS);
			}
		}
	}

	@SuppressWarnings({ "all", "unused" })
	private static XLog createLog(String[][] l) {
		XLog log = XFactoryRegistry.instance().currentDefault().createLog();

		for (int i = 0; i < l.length; i++) {

			XTrace trace = XFactoryRegistry.instance().currentDefault().createTrace();
			XConceptExtension.instance().assignName(trace, "trace " + i);
			log.add(trace);
			XEvent e;

			for (int j = 0; j < l[i].length; j++) {
				e = XFactoryRegistry.instance().currentDefault().createEvent();
				XConceptExtension.instance().assignName(e, l[i][j]);
				XLifecycleExtension.instance().assignStandardTransition(e, StandardModel.COMPLETE);
				trace.add(e);
			}
		}
		return log;

	}

	public static XLog createInterleavedLog(int tracelimit, String... evts) {

		XLog log = XFactoryRegistry.instance().currentDefault().createLog();

		XTrace trace = XFactoryRegistry.instance().currentDefault().createTrace();
		createInterleavedTrace(tracelimit, log, trace, Arrays.asList(evts));

		return log;

	}

	private static void createInterleavedTrace(int tracelimit, XLog log, XTrace trace, List<String> evts) {

		if (log.size() >= tracelimit) {
			return;
		}

		for (String evt : evts) {
			// select one,
			// add as event
			XEvent e = XFactoryRegistry.instance().currentDefault().createEvent();
			XConceptExtension.instance().assignName(e, evt);
			XLifecycleExtension.instance().assignStandardTransition(e, StandardModel.COMPLETE);
			trace.add(e);

			// continue with rest
			List<String> rest = new ArrayList<String>(evts);
			rest.remove(evt);
			createInterleavedTrace(tracelimit, log, trace, rest);
			trace.remove(e);
		}

		if (evts.isEmpty()) {
			log.add((XTrace) trace.clone());
		}
	}
}
