package org.processmining.plugins.replayer.tester;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.connections.flexiblemodel.FlexStartTaskNodeConnection;
import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexFactory;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.models.flexiblemodel.SetFlex;
import org.processmining.models.flexiblemodel.StartTaskNodesSet;

public class Test1 {
//	@UITopiaVariant(affiliation = UITopiaVariant.EHV,  author = "arya", email = "a.adriansyah@tue.nl")
	@Plugin(name = "Produce Flexible model 1", 
			parameterLabels = {}, 
			returnLabels = { "Flexible model 1" , "Start Task" }, 
			returnTypes = { Flex.class , StartTaskNodesSet.class }, 
			userAccessible = true, 
			help = "Produces a Flexible model and its start task")
	public static Object[] petriNetProduce(PluginContext context) {
	final Flex flexDiagram = FlexFactory.newFlex("Flexible model 1");
	FlexNode a = flexDiagram.addNode("A");
	FlexNode b2 = flexDiagram.addNode("B2");
	FlexNode b1 = flexDiagram.addNode("B1");
	FlexNode tau = flexDiagram.addNode("Tau");
	tau.setInvisible(true);
	FlexNode x = flexDiagram.addNode("X");
	FlexNode y = flexDiagram.addNode("Y");
	FlexNode z = flexDiagram.addNode("Z");
	
	flexDiagram.addArc(a, tau);
	flexDiagram.addArc(a, b2);
	
	flexDiagram.addArc(tau, x);
	flexDiagram.addArc(tau, y);
	flexDiagram.addArc(tau, z);
	flexDiagram.addArc(tau, b1);
	

//	SetFlex empty=new SetFlex();
//	a.addInputNodes(empty);
	
	// output of a is {b2,tau} AND-SPLIT
	SetFlex setTauC= new SetFlex();
	setTauC.add(tau);
	setTauC.add(b2);
	a.addOutputNodes(setTauC);
	
	//in put for b2 is a
	SetFlex setB= new SetFlex();
	setB.add(a);
	b2.addInputNodes(setB);
	
	//input for tau is a
	SetFlex setTau= new SetFlex();
	setTau.add(a);
	tau.addInputNodes(setTau);
	
	//output for tau is x,y,z and b1
	SetFlex setTauOut=new SetFlex();
	setTauOut.add(x);
	setTauOut.add(y);
	setTauOut.add(z);
	setTauOut.add(b1);
	tau.addOutputNodes(setTauOut);
	
	//in put for b1 is a
	SetFlex setX= new SetFlex();
	setX.add(tau);
	x.addInputNodes(setX);
	
	SetFlex setY= new SetFlex();
	setY.add(tau);
	y.addInputNodes(setY);
	
	SetFlex setZ= new SetFlex();
	setZ.add(tau);
	z.addInputNodes(setZ);
	
	SetFlex setB1= new SetFlex();
	setB1.add(tau);
	b1.addInputNodes(setB1);
	
	
	a.commitUpdates();
	b1.commitUpdates();
	x.commitUpdates();
	y.commitUpdates();
	z.commitUpdates();
	b2.commitUpdates();
	tau.commitUpdates();

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
