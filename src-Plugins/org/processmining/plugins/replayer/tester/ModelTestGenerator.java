/**
 * 
 */
package org.processmining.plugins.replayer.tester;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.connections.flexiblemodel.FlexStartTaskNodeConnection;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexFactory;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.models.flexiblemodel.SetFlex;
import org.processmining.models.flexiblemodel.StartTaskNodesSet;


/**
 * @author Arya Adriansyah
 * @email a.adriansyah@tue.nl
 * @version Feb 17, 2011
 */

@Plugin(name = "Generate PN, Flexible model, BPMN, EPC #1", returnLabels = { "Petri net", "PN Start Task Nodes", 
		"Flexible model", "Flexible model Start Task Nodes", "BPMN", "BPMN Start Task Nodes", "EPC", "EPC Start Task Nodes" }, returnTypes = { Flex.class, StartTaskNodesSet.class, Flex.class,
		StartTaskNodesSet.class, Flex.class, StartTaskNodesSet.class, Flex.class, StartTaskNodesSet.class }, parameterLabels = {}, userAccessible = true)
public class ModelTestGenerator {
//	@PluginVariant(variantLabel = "Generate 4 similar models - 1", requiredParameterLabels = {})
//	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "A. Adriansyah", email = "a.adriansyah@tue.nl", uiLabel = UITopiaVariant.USEPLUGIN, pack="Replayer")
	public Object[] generate(PluginContext context) {
		Object[] res = new Object[8];
		int index = 0;

		Object[] pn = generatePNModel(context);
		res[index++] = pn[0];
		res[index++] = pn[1];

		Object[] cn = generateCausalNet(context);
		res[index++] = cn[0];
		res[index++] = cn[1];

		Object[] bpmn = generateBPMN(context);
		res[index++] = bpmn[0];
		res[index++] = bpmn[1];

		Object[] epc = generateEPCModel(context);
		res[index++] = epc[0];
		res[index++] = epc[1];

		return res;
	}

	private Object[] generateEPCModel(PluginContext context) {
		final Flex flexDiagram = FlexFactory.newFlex("EPC");
		FlexNode t1 = flexDiagram.addNode("A");
		FlexNode t2 = flexDiagram.addNode("B");
		FlexNode t3 = flexDiagram.addNode("C");
		FlexNode t4 = flexDiagram.addNode("D");
		
		FlexNode e1 = flexDiagram.addNode("e1");
		e1.setInvisible(true);
		FlexNode e2 = flexDiagram.addNode("e2");
		e2.setInvisible(true);
		FlexNode e3 = flexDiagram.addNode("e3");
		e3.setInvisible(true);

		FlexNode xorsplit = flexDiagram.addNode("XORsplit");
		xorsplit.setInvisible(true);
		FlexNode xorjoin = flexDiagram.addNode("XORjoin");
		xorjoin.setInvisible(true);

		FlexNode start = flexDiagram.addNode("start");
		start.setInvisible(true);
		
		FlexNode end = flexDiagram.addNode("end");
		end.setInvisible(true);

		// start
		SetFlex sett1 = new SetFlex();
		sett1.add(t1);
		start.addOutputNodes(sett1);
		flexDiagram.addArc(start, t1);
		
		// t1
		SetFlex setstart = new SetFlex();
		setstart.add(start);
		t1.addInputNodes(setstart);
		
		SetFlex setxorsplit = new SetFlex();
		setxorsplit.add(xorsplit);
		t1.addOutputNodes(setxorsplit);
		flexDiagram.addArc(t1, xorsplit);
		
		// xor split
		xorsplit.addInputNodes(sett1);
		
		SetFlex sete1 = new SetFlex();
		sete1.add(e1);
		xorsplit.addOutputNodes(sete1);
		flexDiagram.addArc(xorsplit, e1);
		
		SetFlex sete3 = new SetFlex();
		sete3.add(e3);
		xorsplit.addOutputNodes(sete3);
		flexDiagram.addArc(xorsplit, e3);
		
		// e1
		e1.addInputNodes(setxorsplit);
		
		SetFlex sett2 = new SetFlex();
		sett2.add(t2);
		e1.addOutputNodes(sett2);
		flexDiagram.addArc(e1, t2);
		
		// e3
		e3.addInputNodes(setxorsplit);
		
		SetFlex sett3 = new SetFlex();
		sett3.add(t3);
		e3.addOutputNodes(sett3);
		flexDiagram.addArc(e3, t3);
		
		// t2
		t2.addInputNodes(sete1);

		SetFlex setxorjoin = new SetFlex();
		setxorjoin.add(xorjoin);
		t2.addOutputNodes(setxorjoin);
		flexDiagram.addArc(t2, xorjoin);
		
		// t3
		t3.addInputNodes(sete3);
		t3.addOutputNodes(setxorjoin);
		flexDiagram.addArc(t3, xorjoin);
		
		// xor join
		xorjoin.addInputNodes(sett2);
		xorjoin.addInputNodes(sett3);
		
		SetFlex sete2 = new SetFlex();
		sete2.add(e2);
		xorjoin.addOutputNodes(sete2);
		flexDiagram.addArc(xorjoin, e2);
		
		// e2
		e2.addInputNodes(setxorjoin);
		
		SetFlex sett4 = new SetFlex();
		sett4.add(t4);
		e2.addOutputNodes(sett4);
		flexDiagram.addArc(e2, t4);

		// t4
		t4.addInputNodes(sete2);
		
		SetFlex setend = new SetFlex();
		setend.add(end);
		t4.addOutputNodes(setend);
		flexDiagram.addArc(t4, end);
		
		// end
		end.addInputNodes(sett4);
		
		// commit updates
		t1.commitUpdates();
		t2.commitUpdates();
		t3.commitUpdates();
		t4.commitUpdates();
		
		xorsplit.commitUpdates();
		xorjoin.commitUpdates();
		e1.commitUpdates();
		e2.commitUpdates();
		e3.commitUpdates();
		start.commitUpdates();
		end.commitUpdates();

		// add start node
		StartTaskNodesSet startTaskNodeSet = new StartTaskNodesSet();
		startTaskNodeSet.add(setstart);

		// create connection between Flexible model and start task node
		context.addConnection(new FlexStartTaskNodeConnection("Connection to start task node of " + flexDiagram.getLabel(), flexDiagram, startTaskNodeSet));

		// return objects
		return new Object[] { flexDiagram, startTaskNodeSet };
	}

	private Object[] generateBPMN(PluginContext context) {
		final Flex flexDiagram = FlexFactory.newFlex("BPMN");
		FlexNode t1 = flexDiagram.addNode("A");
		FlexNode t2 = flexDiagram.addNode("B");
		FlexNode t3 = flexDiagram.addNode("C");
		FlexNode t4 = flexDiagram.addNode("D");

		FlexNode xorsplit = flexDiagram.addNode("XORsplit");
		xorsplit.setInvisible(true);
		FlexNode xorjoin = flexDiagram.addNode("XORjoin");
		xorjoin.setInvisible(true);

		FlexNode start = flexDiagram.addNode("start");
		start.setInvisible(true);
		
		FlexNode end = flexDiagram.addNode("end");
		end.setInvisible(true);

		// start
		SetFlex sett1 = new SetFlex();
		sett1.add(t1);
		start.addOutputNodes(sett1);
		flexDiagram.addArc(start, t1);
		
		// t1
		SetFlex setstart = new SetFlex();
		setstart.add(start);
		t1.addInputNodes(setstart);
		
		SetFlex setxorsplit = new SetFlex();
		setxorsplit.add(xorsplit);
		t1.addOutputNodes(setxorsplit);
		flexDiagram.addArc(t1, xorsplit);
		
		// xor split
		xorsplit.addInputNodes(sett1);
		
		SetFlex sett2 = new SetFlex();
		sett2.add(t2);
		xorsplit.addOutputNodes(sett2);
		flexDiagram.addArc(xorsplit, t2);
		
		SetFlex sett3 = new SetFlex();
		sett3.add(t3);
		xorsplit.addOutputNodes(sett3);
		flexDiagram.addArc(xorsplit, t3);
		
		// t2
		t2.addInputNodes(setxorsplit);

		SetFlex setxorjoin = new SetFlex();
		setxorjoin.add(xorjoin);
		t2.addOutputNodes(setxorjoin);
		flexDiagram.addArc(t2, xorjoin);
		
		// t3
		t3.addInputNodes(setxorsplit);
		t3.addOutputNodes(setxorjoin);
		flexDiagram.addArc(t3, xorjoin);
		
		// xor join
		xorjoin.addInputNodes(sett2);
		xorjoin.addInputNodes(sett3);
		
		SetFlex sett4 = new SetFlex();
		sett4.add(t4);
		xorjoin.addOutputNodes(sett4);
		flexDiagram.addArc(xorjoin, t4);
		
		// t4
		t4.addInputNodes(setxorjoin);
		
		SetFlex setend = new SetFlex();
		setend.add(end);
		t4.addOutputNodes(setend);
		flexDiagram.addArc(t4, end);
		
		// end
		end.addInputNodes(sett4);
		
		// commit updates
		t1.commitUpdates();
		t2.commitUpdates();
		t3.commitUpdates();
		t4.commitUpdates();
		
		xorsplit.commitUpdates();
		xorjoin.commitUpdates();
		start.commitUpdates();
		end.commitUpdates();

		// add start node
		StartTaskNodesSet startTaskNodeSet = new StartTaskNodesSet();
		startTaskNodeSet.add(setstart);

		// create connection between Flexible model and start task node
		context.addConnection(new FlexStartTaskNodeConnection("Connection to start task node of " + flexDiagram.getLabel(), flexDiagram, startTaskNodeSet));

		// return objects
		return new Object[] { flexDiagram, startTaskNodeSet };
	}

	private Object[] generateCausalNet(PluginContext context) {
		final Flex flexDiagram = FlexFactory.newFlex("Flexible model");
		FlexNode t1 = flexDiagram.addNode("A");
		FlexNode t2 = flexDiagram.addNode("B");
		FlexNode t3 = flexDiagram.addNode("C");
		FlexNode t4 = flexDiagram.addNode("D");

		// t1
		SetFlex sett2 = new SetFlex();
		sett2.add(t2);
		t1.addOutputNodes(sett2);
		flexDiagram.addArc(t1, t2);
		
		SetFlex sett3 = new SetFlex();
		sett3.add(t3);
		t1.addOutputNodes(sett3);
		flexDiagram.addArc(t1, t3);
		
		// t2
		SetFlex sett1 = new SetFlex();
		sett1.add(t1);
		t2.addInputNodes(sett1);

		SetFlex sett4 = new SetFlex();
		sett4.add(t4);
		t2.addOutputNodes(sett4);
		flexDiagram.addArc(t2, t4);

		// t3
		t3.addInputNodes(sett1);
		t3.addOutputNodes(sett4);
		flexDiagram.addArc(t3, t4);
		
		// t4
		t4.addInputNodes(sett2);
		t4.addInputNodes(sett3);
		
		// commit updates
		t1.commitUpdates();
		t2.commitUpdates();
		t3.commitUpdates();
		t4.commitUpdates();

		// add start node
		StartTaskNodesSet startTaskNodeSet = new StartTaskNodesSet();
		startTaskNodeSet.add(sett1);

		// create connection between Flexible model and start task node
		context.addConnection(new FlexStartTaskNodeConnection("Connection to start task node of " + flexDiagram.getLabel(), flexDiagram, startTaskNodeSet));

		// return objects
		return new Object[] { flexDiagram, startTaskNodeSet };
	}

	private Object[] generatePNModel(PluginContext context) {
		final Flex flexDiagram = FlexFactory.newFlex("Petri Net");
		FlexNode t1 = flexDiagram.addNode("A");
		FlexNode t2 = flexDiagram.addNode("B");
		FlexNode t3 = flexDiagram.addNode("C");
		FlexNode t4 = flexDiagram.addNode("D");
		
		FlexNode doB = flexDiagram.addNode("doB");
		doB.setInvisible(true);
		
		FlexNode doC = flexDiagram.addNode("doC");
		doC.setInvisible(true);

		FlexNode start = flexDiagram.addNode("start");
		start.setInvisible(true);
		
		FlexNode p1 = flexDiagram.addNode("p1");
		p1.setInvisible(true);
		
		FlexNode p2 = flexDiagram.addNode("p2");
		p2.setInvisible(true);
		
		FlexNode p3 = flexDiagram.addNode("p3");
		p3.setInvisible(true);
		
		FlexNode p4 = flexDiagram.addNode("p4");
		p4.setInvisible(true);
		
		FlexNode end = flexDiagram.addNode("end");
		end.setInvisible(true);

		// start
		SetFlex sett1 = new SetFlex();
		sett1.add(t1);
		start.addOutputNodes(sett1);
		flexDiagram.addArc(start, t1);

		// t1
		SetFlex setStart = new SetFlex();
		setStart.add(start);
		t1.addInputNodes(setStart);

		SetFlex setp1 = new SetFlex();
		setp1.add(p1);
		t1.addOutputNodes(setp1);
		flexDiagram.addArc(t1, p1);

		// p1
		p1.addInputNodes(sett1);

		SetFlex setdoB = new SetFlex();
		setdoB.add(doB);
		p1.addOutputNodes(setdoB);
		flexDiagram.addArc(p1, doB);

		SetFlex setdoC = new SetFlex();
		setdoC.add(doC);
		p1.addOutputNodes(setdoC);
		flexDiagram.addArc(p1, doC);

		// doB
		doB.addInputNodes(setp1);

		SetFlex setp2 = new SetFlex();
		setp2.add(p2);
		doB.addOutputNodes(setp2);
		flexDiagram.addArc(doB, p2);

		// doC
		doC.addInputNodes(setp1);

		SetFlex setp3 = new SetFlex();
		setp3.add(p3);
		doC.addOutputNodes(setp3);
		flexDiagram.addArc(doC, p3);
		
		// p2
		p2.addInputNodes(setdoB);
		
		SetFlex sett2 = new SetFlex();
		sett2.add(t2);
		p2.addOutputNodes(sett2);
		flexDiagram.addArc(p2, t2);
		
		// p3
		p3.addInputNodes(setdoC);
		
		SetFlex sett3 = new SetFlex();
		sett3.add(t3);
		p3.addOutputNodes(sett3);
		flexDiagram.addArc(p3, t3);
		
		// t2
		t2.addInputNodes(setp2);
		
		SetFlex setp4 = new SetFlex();
		setp4.add(p4);
		t2.addOutputNodes(setp4);
		flexDiagram.addArc(t2, p4);

		// t3
		t3.addInputNodes(setp3);
		
		t3.addOutputNodes(setp4);
		flexDiagram.addArc(t3, p4);
		
		// p4
		p4.addInputNodes(sett2);
		p4.addInputNodes(sett3);
		
		SetFlex sett4 = new SetFlex();
		sett4.add(t4);
		p4.addOutputNodes(sett4);
		flexDiagram.addArc(p4, t4);
		
		// t4
		t4.addInputNodes(setp4);
		
		SetFlex setend = new SetFlex();
		setend.add(end);
		t4.addOutputNodes(setend);
		flexDiagram.addArc(t4, end);
		
		// end
		end.addInputNodes(sett4);
		
		// commit updates
		t1.commitUpdates();
		t2.commitUpdates();
		t3.commitUpdates();
		t4.commitUpdates();
		start.commitUpdates();
		p1.commitUpdates();
		p2.commitUpdates();
		p3.commitUpdates();
		p4.commitUpdates();
		end.commitUpdates();
		doB.commitUpdates();
		doC.commitUpdates();

		// add start node
		StartTaskNodesSet startTaskNodeSet = new StartTaskNodesSet();
		startTaskNodeSet.add(setStart);

		// create connection between Flexible model and start task node
		context.addConnection(new FlexStartTaskNodeConnection("Connection to start task node of " + flexDiagram.getLabel(), flexDiagram, startTaskNodeSet));

		// return objects
		return new Object[] { flexDiagram, startTaskNodeSet };
	}
}
