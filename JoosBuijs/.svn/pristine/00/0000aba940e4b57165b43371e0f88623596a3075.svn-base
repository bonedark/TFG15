//=============================================================================
// Copyright 2006-2010 Daniel W. Dyer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//=============================================================================
package org.processmining.plugins.joosbuijs.blockminer.genetic;

import java.util.Random;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.plugins.joosbuijs.blockminer.genetic.FunctionNode.FUNCTIONTYPE;
import org.uncommons.maths.random.Probability;

/**
 * The tree (leaf) nodes that represent an event class of the event log
 * 
 * @author jbuijs
 * @author Daniel Dyer (original)
 */
public class EventClassNode implements Node
{
	//The event class this node represents
	private XEventClass eventClass;
	
	/**
	 * Instantiate a new instance
	 * 
	 * @param eventClass The event class this node represents
	 */
	public EventClassNode(XEventClass eventClass)
	{
		this.eventClass = eventClass;
	}
	
	/**
	 * 
	 * @return The event class this node represents
	 */
	public XEventClass getEventClass()
	{
		return eventClass;
	}
	
    @Override
    public String toString()
    {
        return eventClass.toString();
    }
    
    public String toString(int level)
    {
		StringBuilder buffer = new StringBuilder("");
		for (int i = 0; i < level; i++)
			buffer.append("\t");
		//System.getProperty("line.separator")
		buffer.append(eventClass.toString());
		buffer.append(System.getProperty("line.separator"));
		return buffer.toString();
    }
    
    public String getLabel()
    {
        return eventClass.toString();
    }
	
    /**
     * Replaces the current node 
     */
    public Node replaceNode(int index, Node newNode)
    {
    	//FIXME check if correct
        if (index != 0)
        {
            throw new IndexOutOfBoundsException("Invalid node index: " + index);
        }
        return newNode;        
    }

    /**
     * {@inheritDoc}
     */
    public Node mutate(Random rng, Probability mutationProbability, TreeFactory treeFactory)
    {
    	//Check if correct
        if (mutationProbability.nextEvent(rng))
        {
        	//Don't add more than one new node
        	return treeFactory.generateRandomCandidate(rng,1);
        }
        else
        {
            // Node is unchanged.
            return this;
        }
    }
    
    /**
     * The arity of a non-function node is always zero.
     * @return 0
     */
    public int getArity()
    {
        return 0;
    }

    
    /**
     * Leaf nodes always have a depth of 1 since they have no child nodes.
     * @return 1 
     */
    public int getDepth()
    {
        return 1;
    }


    /**
     * Leaf nodes always have a width of 1 since they have no child nodes.
     * @return 1
     */
    public int getWidth()
    {
        return 1;
    }

    
    /**
     * {@inheritDoc}
     */
    public int countNodes()
    {
        return 1;
    }
    
    /**
     * {@inheritDoc}
     */
    public int countLeafs()
    {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    public Node getNode(int index)
    {
        if (index != 0)
        {
            throw new IndexOutOfBoundsException("Invalid node index: " + index);
        }
        return this;
    }


    /**
     * {@inheritDoc}
     */
    public Node getChild(int index)
    {
        throw new IndexOutOfBoundsException("Leaf nodes have no children.");
    }

	public FUNCTIONTYPE getFunction() {
		return null;
	}
}
