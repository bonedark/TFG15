package org.processmining.plugins.replayer.tester;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.connections.flexiblemodel.FlexStartTaskNodeConnection;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexFactory;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.models.flexiblemodel.SetFlex;
import org.processmining.models.flexiblemodel.StartTaskNodesSet;

public class Test2 {
//	@UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "arya", email = "a.adriansyah@tue.nl")
	@Plugin(name = "Produce Flexible model 2", 
			parameterLabels = {}, 
			returnLabels = { "Flexible model 2" , "Start Task" }, 
			returnTypes = { Flex.class , StartTaskNodesSet.class }, 
			userAccessible = true, 
			help = "Produces a Flexible model and its start task")
	public static Object[] petriNetProduce(PluginContext context) {
	final Flex flexDiagram = FlexFactory.newFlex("Flexible model 1");
	FlexNode a = flexDiagram.addNode("A");
	FlexNode b2 = flexDiagram.addNode("B");
	FlexNode tau = flexDiagram.addNode("Tau");
	tau.setInvisible(true);
	FlexNode c = flexDiagram.addNode("C");
	
	flexDiagram.addArc(a, b2);
	flexDiagram.addArc(b2, tau);
	
	flexDiagram.addArc(tau, c);
	
	// output of a
	SetFlex setB= new SetFlex();
	setB.add(b2);
	a.addOutputNodes(setB);

	// input of B is A
	SetFlex setA = new SetFlex();
	setA.add(a);
	b2.addInputNodes(setA);

	// output of B is tau
	SetFlex setTau = new SetFlex();
	setTau.add(tau);
	b2.addOutputNodes(setTau);
	
	// input of tau
	tau.addInputNodes(setB);
	
	// output of tau
	SetFlex setC = new SetFlex();
	setC.add(c);
	tau.addOutputNodes(setC);
	
	
	// input of C
	c.addInputNodes(setTau);
	
	a.commitUpdates();
	b2.commitUpdates();
	tau.commitUpdates();
	c.commitUpdates();

	// add start node
	StartTaskNodesSet startTaskNode = new StartTaskNodesSet();
	SetFlex newSetFlex = new SetFlex();
	newSetFlex.add(a);
	startTaskNode.add(newSetFlex);

	// create connection between Flexible model and start task node
	context.addConnection(new FlexStartTaskNodeConnection(flexDiagram.getLabel(), flexDiagram, startTaskNode));

	// return objects
	return new Object[] { flexDiagram, startTaskNode };
	}
}
