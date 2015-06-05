/**
 * 
 */
package org.processmining.plugins.replayer.tester;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.rpstwrapper.RPSTConsultant;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.rpstwrapper.RPSTTreeWrapper;
import org.processmining.plugins.petrinet.replayer.util.codec.EncPNWSetFinalMarkings;

/**
 * @author aadrians
 * 
 */
public class TestRPSTreeWrapper {

	public static void main(String[] args) {
		TestRPSTreeWrapper tes = new TestRPSTreeWrapper();
		try {
			tes.test_xorSoundNetWLoop();
		} catch (Exception exc1) {
			System.out.println(exc1.toString());
		}
		
		try {
			tes.test_linearNet();
		} catch (Exception exc2) {
			System.out.println(exc2.toString());
		}
		
		try {
			tes.test_xorSoundNet();
		} catch (Exception exc3) {
			System.out.println(exc3.toString());
		}
		
		try {
			tes.test_andBackwardSoundNetWLoop();
		} catch (Exception exc4) {
			System.out.println(exc4.toString());
		}
	}

	// Petrinet Structure:
	//	o-A-o-B-o-C-o-D-o
	@Test
	public void test_linearNet() throws Exception {
		// create a linear petri net
		Petrinet net = PetrinetFactory.newPetrinet("testing");
		Place p0 = net.addPlace("p0");
		Place p1 = net.addPlace("p1");
		Place p2 = net.addPlace("p2");
		Place p3 = net.addPlace("p3");
		Place p4 = net.addPlace("p4");

		Transition t0 = net.addTransition("t0");
		Transition t1 = net.addTransition("t1");
		Transition t2 = net.addTransition("t2");
		Transition t3 = net.addTransition("t3");

		net.addArc(p0, t0);
		net.addArc(t0, p1);

		net.addArc(p1, t1);
		net.addArc(t1, p2);

		net.addArc(p2, t2);
		net.addArc(t2, p3);

		net.addArc(p3, t3);
		net.addArc(t3, p4);

		Marking initMarking = new Marking();
		initMarking.add(p0);

		Marking finalMarking = new Marking();
		finalMarking.add(p4);

		EncPNWSetFinalMarkings encodedPN = new EncPNWSetFinalMarkings(net, initMarking, new Marking[] { finalMarking },
				null);

		// create mapping from integer to encoded marking
		Map<Integer, Map<Integer, Integer>> mapInt2EncMarking = new HashMap<Integer, Map<Integer, Integer>>();

		RPSTConsultant consultant = new RPSTConsultant(new RPSTTreeWrapper(encodedPN));

		// Testing 1 : given initial marking from the source place, return ABCD
		// create encoded marking
		Map<Integer, Integer> encMarking = new HashMap<Integer, Integer>();

		Integer encT0 = encodedPN.getEncOf(t0);
		for (Integer encPlace : encodedPN.getPredecessorsOf(encT0)) {
			encMarking.put(encPlace, 1);
		}
		;
		mapInt2EncMarking.put(1, encMarking);

		Map<Integer, Integer> expectedResult = new HashMap<Integer, Integer>();
		expectedResult.put(encodedPN.getEncOf(t0), 1);
		expectedResult.put(encodedPN.getEncOf(t1), 1);
		expectedResult.put(encodedPN.getEncOf(t2), 1);
		expectedResult.put(encodedPN.getEncOf(t3), 1);

		Map<Integer, Integer> estimation = consultant.getRequiredSuccessors(1, mapInt2EncMarking, encodedPN);

//		assertTrue(estimation.equals(expectedResult));

		// Testing 2 : give an initial marking after B is executed, expecting only C and D
		// create encoded marking
		Map<Integer, Integer> encMarking2 = new HashMap<Integer, Integer>();

		Integer encT2 = encodedPN.getEncOf(t2);
		for (Integer encPlace : encodedPN.getPredecessorsOf(encT2)) {
			encMarking2.put(encPlace, 1);
		}
		;
		mapInt2EncMarking.put(2, encMarking2);

		Map<Integer, Integer> expectedResult2 = new HashMap<Integer, Integer>();
		expectedResult2.put(encodedPN.getEncOf(t2), 1);
		expectedResult2.put(encodedPN.getEncOf(t3), 1);

		Map<Integer, Integer> estimation2 = consultant.getRequiredSuccessors(2, mapInt2EncMarking, encodedPN);

//		assertTrue(estimation2.equals(expectedResult2));
	}

	// Petrinet Structure:
	//	  -A-       -D-o-E-
	//	 /   \     /       \
	//	o     o-C-o         o-G-o-H-o
	//	 \   /     \       /
	//	  -B-       ---F--- 

	@Test
	public void test_xorSoundNet() throws Exception {
		// create a linear petri net
		Petrinet net = PetrinetFactory.newPetrinet("testing");
		Place p0 = net.addPlace("p0");
		Place p1 = net.addPlace("p1");
		Place p2 = net.addPlace("p2");
		Place p3 = net.addPlace("p3");
		Place p4 = net.addPlace("p4");
		Place p5 = net.addPlace("p5");
		Place p6 = net.addPlace("p6");

		Transition tA = net.addTransition("A");
		Transition tB = net.addTransition("B");
		Transition tC = net.addTransition("C");
		Transition tD = net.addTransition("D");
		Transition tE = net.addTransition("E");
		Transition tF = net.addTransition("F");
		Transition tG = net.addTransition("G");
		Transition tH = net.addTransition("H");

		net.addArc(p0, tA);
		net.addArc(p0, tB);
		net.addArc(tA, p1);
		net.addArc(tB, p1);

		net.addArc(p1, tC);
		net.addArc(tC, p2);

		net.addArc(p2, tD);
		net.addArc(tD, p3);
		net.addArc(p3, tE);
		net.addArc(tE, p4);

		net.addArc(p2, tF);
		net.addArc(tF, p4);

		net.addArc(p4, tG);
		net.addArc(tG, p5);

		net.addArc(p5, tH);
		net.addArc(tH, p6);

		Marking initMarking = new Marking();
		initMarking.add(p0);

		Marking finalMarking = new Marking();
		finalMarking.add(p6);

		EncPNWSetFinalMarkings encodedPN = new EncPNWSetFinalMarkings(net, initMarking, new Marking[] { finalMarking },
				null);

		// create mapping from integer to encoded marking
		Map<Integer, Map<Integer, Integer>> mapInt2EncMarking = new HashMap<Integer, Map<Integer, Integer>>();

		RPSTConsultant consultant = new RPSTConsultant(new RPSTTreeWrapper(encodedPN));

		// Testing 1 : given initial marking from the source place, return CGH
		// create encoded marking
		Map<Integer, Integer> encMarking = new HashMap<Integer, Integer>();

		Integer encT0 = encodedPN.getEncOf(tA);
		for (Integer encPlace : encodedPN.getPredecessorsOf(encT0)) {
			encMarking.put(encPlace, 1);
		}
		;
		mapInt2EncMarking.put(1, encMarking);

		Map<Integer, Integer> expectedResult = new HashMap<Integer, Integer>();
		expectedResult.put(encodedPN.getEncOf(tC), 1);
		expectedResult.put(encodedPN.getEncOf(tG), 1);
		expectedResult.put(encodedPN.getEncOf(tH), 1);

		Map<Integer, Integer> estimation = consultant.getRequiredSuccessors(1, mapInt2EncMarking, encodedPN);

		assert(estimation.equals(expectedResult));
	}

	// Petrinet Structure:
	//        ----E2-----<-----------
	//	  -A- |     -D-o-E-         |
	//	 /   \|    /       \        |
	//	o     o-C-o         o-G-o-H-o-I-o
	//	 \   /     \       /
	//	  -B-       ---F--- 

	@Test
	public void test_xorSoundNetWLoop() throws Exception {
		// create a linear petri net
		Petrinet net = PetrinetFactory.newPetrinet("testing");
		Place p0 = net.addPlace("p0");
		Place p1 = net.addPlace("p1");
		Place p2 = net.addPlace("p2");
		Place p3 = net.addPlace("p3");
		Place p4 = net.addPlace("p4");
		Place p5 = net.addPlace("p5");
		Place p6 = net.addPlace("p6");
		Place p7 = net.addPlace("p7");

		Transition tA = net.addTransition("A");
		Transition tB = net.addTransition("B");
		Transition tC = net.addTransition("C");
		Transition tD = net.addTransition("D");
		Transition tE = net.addTransition("E");
		Transition tF = net.addTransition("F");
		Transition tG = net.addTransition("G");
		Transition tH = net.addTransition("H");
		Transition tI = net.addTransition("I");
		Transition tE2 = net.addTransition("E2");

		net.addArc(p0, tA);
		net.addArc(p0, tB);
		net.addArc(tA, p1);
		net.addArc(tB, p1);

		net.addArc(p1, tC);
		net.addArc(tC, p2);

		net.addArc(p2, tD);
		net.addArc(tD, p3);
		net.addArc(p3, tE);
		net.addArc(tE, p4);

		net.addArc(p2, tF);
		net.addArc(tF, p4);

		net.addArc(p4, tG);
		net.addArc(tG, p5);

		net.addArc(p5, tH);
		net.addArc(tH, p6);

		net.addArc(p6, tE2);
		net.addArc(tE2, p1);

		net.addArc(p6, tI);
		net.addArc(tI, p7);

		Marking initMarking = new Marking();
		initMarking.add(p0);

		Marking finalMarking = new Marking();
		finalMarking.add(p6);

		EncPNWSetFinalMarkings encodedPN = new EncPNWSetFinalMarkings(net, initMarking, new Marking[] { finalMarking },
				null);

		// create mapping from integer to encoded marking
		Map<Integer, Map<Integer, Integer>> mapInt2EncMarking = new HashMap<Integer, Map<Integer, Integer>>();

		RPSTConsultant consultant = new RPSTConsultant(new RPSTTreeWrapper(encodedPN));

		// Testing 1 : given initial marking from the source place, return CGHI
		// create encoded marking
		Map<Integer, Integer> encMarking = new HashMap<Integer, Integer>();

		Integer encT0 = encodedPN.getEncOf(tA);
		for (Integer encPlace : encodedPN.getPredecessorsOf(encT0)) {
			encMarking.put(encPlace, 1);
		}
		;
		mapInt2EncMarking.put(1, encMarking);

		Map<Integer, Integer> expectedResult = new HashMap<Integer, Integer>();
		expectedResult.put(encodedPN.getEncOf(tC), 1);
		expectedResult.put(encodedPN.getEncOf(tG), 1);
		expectedResult.put(encodedPN.getEncOf(tH), 1);
		expectedResult.put(encodedPN.getEncOf(tI), 1);

		Map<Integer, Integer> estimation = consultant.getRequiredSuccessors(1, mapInt2EncMarking, encodedPN);

		assert(estimation.equals(expectedResult));

		// Testing 2 : given initial marking from the predecessor of E, return EGHI
		// create encoded marking
		Map<Integer, Integer> encMarking2 = new HashMap<Integer, Integer>();

		Integer encTE = encodedPN.getEncOf(tE);
		for (Integer encPlace : encodedPN.getPredecessorsOf(encTE)) {
			encMarking2.put(encPlace, 1);
		}
		;
		mapInt2EncMarking.put(2, encMarking2);

		Map<Integer, Integer> expectedResult2 = new HashMap<Integer, Integer>();
		expectedResult2.put(encodedPN.getEncOf(tE), 1);
		expectedResult2.put(encodedPN.getEncOf(tG), 1);
		expectedResult2.put(encodedPN.getEncOf(tH), 1);
		expectedResult2.put(encodedPN.getEncOf(tI), 1);

		Map<Integer, Integer> estimation2 = consultant.getRequiredSuccessors(2, mapInt2EncMarking, encodedPN);

		assert(estimation2.equals(expectedResult2));
	}

	// Petrinet Structure:
	//              o--A2--<-o
	//             /          \
	//        -<-G2-o--F2-<-o--D2-o--
	//	  -A- |     -D-o-E-         H
	//	 /   \|    /       \        |
	//	o     o-C-o         o---G---o---I-o
	//	 \   /     \       /
	//	  -B-       ---F--- 

	@Test
	public void test_andBackwardSoundNetWLoop() throws Exception {
		// create a linear petri net
		Petrinet net = PetrinetFactory.newPetrinet("testing");
		Place p0 = net.addPlace("p0");
		Place p1 = net.addPlace("p1");
		Place p2 = net.addPlace("p2");
		Place p3 = net.addPlace("p3");
		Place p4 = net.addPlace("p4");
		Place p5 = net.addPlace("p5");
		Place p6 = net.addPlace("p6");
		Place p7 = net.addPlace("p7");
		Place p8 = net.addPlace("p8");
		Place p9 = net.addPlace("p9");
		Place p10 = net.addPlace("p10");
		Place p11 = net.addPlace("p11");

		Transition tA = net.addTransition("A");
		Transition tA2 = net.addTransition("A2");
		Transition tB = net.addTransition("B");
		Transition tC = net.addTransition("C");
		Transition tD = net.addTransition("D");
		Transition tD2 = net.addTransition("D2");
		Transition tE = net.addTransition("E");
		Transition tF = net.addTransition("F");
		Transition tG = net.addTransition("G");
		Transition tF2 = net.addTransition("F2");
		Transition tG2 = net.addTransition("G2");
		Transition tH = net.addTransition("H");
		Transition tI = net.addTransition("I");

		net.addArc(p0, tA);
		net.addArc(p0, tB);
		net.addArc(tA, p1);
		net.addArc(tB, p1);

		net.addArc(p1, tC);
		net.addArc(tC, p2);

		net.addArc(p2, tD);
		net.addArc(tD, p3);
		net.addArc(p3, tE);
		net.addArc(tE, p4);

		net.addArc(p2, tF);
		net.addArc(tF, p4);

		net.addArc(p4, tG);
		net.addArc(tG, p5);

		net.addArc(p5, tH);
		net.addArc(p5, tI);
		net.addArc(tI, p6);
		net.addArc(tH, p7);

		net.addArc(p7, tD2);
		net.addArc(tD2, p8);
		net.addArc(tD2, p9);

		net.addArc(p8, tA2);
		net.addArc(p9, tF2);

		net.addArc(tA2, p10);
		net.addArc(tF2, p11);

		net.addArc(p10, tG2);
		net.addArc(p11, tG2);

		net.addArc(tG2, p1);

		Marking initMarking = new Marking();
		initMarking.add(p8);
		initMarking.add(p9);

		Marking finalMarking = new Marking();
		finalMarking.add(p6);

		EncPNWSetFinalMarkings encodedPN = new EncPNWSetFinalMarkings(net, initMarking, new Marking[] { finalMarking },
				null);

		// create mapping from integer to encoded marking
		Map<Integer, Map<Integer, Integer>> mapInt2EncMarking = new HashMap<Integer, Map<Integer, Integer>>();

		RPSTConsultant consultant = new RPSTConsultant(new RPSTTreeWrapper(encodedPN));

		// Testing 1 : given initial marking from the source place, return GCI
		// create encoded marking
		Map<Integer, Integer> encMarking = new HashMap<Integer, Integer>();

		Integer encT0 = encodedPN.getEncOf(tA);
		for (Integer encPlace : encodedPN.getPredecessorsOf(encT0)) {
			encMarking.put(encPlace, 1);
		}
		;
		mapInt2EncMarking.put(1, encMarking);

		Map<Integer, Integer> expectedResult = new HashMap<Integer, Integer>();
		expectedResult.put(encodedPN.getEncOf(tG), 1);
		expectedResult.put(encodedPN.getEncOf(tC), 1);
		expectedResult.put(encodedPN.getEncOf(tI), 1);

		Map<Integer, Integer> estimation = consultant.getRequiredSuccessors(1, mapInt2EncMarking, encodedPN);

		assert(estimation.equals(expectedResult));

		// Testing 2 : given initial marking from the predecessor of D2, return AFDGGCI
		// create encoded marking
		Map<Integer, Integer> encMarking2 = new HashMap<Integer, Integer>();

		Integer encTD2 = encodedPN.getEncOf(tD2);
		for (Integer encPlace : encodedPN.getPredecessorsOf(encTD2)) {
			encMarking2.put(encPlace, 1);
		}
		;
		mapInt2EncMarking.put(2, encMarking2);

		Map<Integer, Integer> expectedResult2 = new HashMap<Integer, Integer>();
		expectedResult2.put(encodedPN.getEncOf(tD2), 1);
		expectedResult2.put(encodedPN.getEncOf(tA2), 1);
		expectedResult2.put(encodedPN.getEncOf(tF2), 1);
		expectedResult2.put(encodedPN.getEncOf(tG2), 1);
		expectedResult2.put(encodedPN.getEncOf(tI), 1);
		expectedResult2.put(encodedPN.getEncOf(tC), 1);
		expectedResult2.put(encodedPN.getEncOf(tG), 1);

		Map<Integer, Integer> estimation2 = consultant.getRequiredSuccessors(2, mapInt2EncMarking, encodedPN);

		assert(estimation2.equals(expectedResult2));

		// Testing 3 : given initial marking from the predecessor of G2, return CGGI
		// create encoded marking
		Map<Integer, Integer> encMarking3 = new HashMap<Integer, Integer>();

		Integer encTG2 = encodedPN.getEncOf(tG2);
		for (Integer encPlace : encodedPN.getPredecessorsOf(encTG2)) {
			encMarking3.put(encPlace, 1);
		}
		;
		mapInt2EncMarking.put(3, encMarking3);

		Map<Integer, Integer> expectedResult3 = new HashMap<Integer, Integer>();
		expectedResult3.put(encodedPN.getEncOf(tG2), 1);
		expectedResult3.put(encodedPN.getEncOf(tC), 1);
		expectedResult3.put(encodedPN.getEncOf(tG), 1);
		expectedResult3.put(encodedPN.getEncOf(tI), 1);

		Map<Integer, Integer> estimation3 = consultant.getRequiredSuccessors(3, mapInt2EncMarking, encodedPN);

		assert(estimation3.equals(expectedResult3));

		// Testing 3 : given initial marking in the final marking return empty
		// create encoded marking
		Map<Integer, Integer> encMarking4 = new HashMap<Integer, Integer>();
		encMarking4.put(encodedPN.getEncFinalMarkings().iterator().next().keySet().iterator().next(), 1);

		mapInt2EncMarking.put(4, encMarking4);

		Map<Integer, Integer> expectedResult4 = new HashMap<Integer, Integer>();

		Map<Integer, Integer> estimation4 = consultant.getRequiredSuccessors(4, mapInt2EncMarking, encodedPN);

		assert(estimation4.equals(expectedResult4));

	}

}
