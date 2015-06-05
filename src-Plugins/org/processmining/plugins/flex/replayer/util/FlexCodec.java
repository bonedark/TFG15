/**
 * 
 */
package org.processmining.plugins.flex.replayer.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.processmining.models.flexiblemodel.Flex;
import org.processmining.models.flexiblemodel.FlexNode;
import org.processmining.models.flexiblemodel.SetFlex;

/**
 * @author Arya Adriansyah
 * @email a.adriansyah@tue.nl
 * @version Jul 11, 2010
 */
public class FlexCodec {
	public static final Short BLANK = Short.MIN_VALUE;
	public static final Short EMPTYSET = (short) (Short.MIN_VALUE + 1);

	// mapping of a node to index
	private Map<Short, FlexNode> mapShortNode;
	private Map<FlexNode, Short> mapNodeShort;

	// binding (set of possible output/input bindings)
	private Map<Short, Set<Short>> mapIOBindings;
	private Map<Set<Short>, Short> mapIOBindingsInv;

	// mapping from id to binding
	private Map<Short, FlexBinding> mapCodeToBinding;

	// mapping from possible binding of a node
	private Map<Short, Set<Short>> possibleNodeBindings;

	// additional info about nodes
	private Set<Short> bindingAnytimePossiblyFire;
	private Set<Short> invisNodes;

	// METHODS

	public Set<Short> getPossibleNodeBindings(Short node) {
		return possibleNodeBindings.get(node);
	}

	public FlexBinding getFlexBindingFor(Short codeBindingID) {
		return mapCodeToBinding.get(codeBindingID);
	}

	public Set<Short> getIOBindingsFor(Short ioBindingsID) {
		return mapIOBindings.get(ioBindingsID);
	}

	public FlexCodec(Flex flex) {
		Set<FlexNode> setNodes = flex.getNodes();
		mapShortNode = new HashMap<Short, FlexNode>(setNodes.size());
		mapNodeShort = new HashMap<FlexNode, Short>(setNodes.size());
		mapIOBindings = new HashMap<Short, Set<Short>>();
		mapIOBindingsInv = new HashMap<Set<Short>, Short>();
		mapCodeToBinding = new HashMap<Short, FlexBinding>();

		possibleNodeBindings = new HashMap<Short, Set<Short>>();
		bindingAnytimePossiblyFire = new HashSet<Short>(5);
		invisNodes = new HashSet<Short>();

		// create mapShortNode and mapNodeShort
		short nodeID = (short) (EMPTYSET + 1);
		for (FlexNode node : setNodes) {
			mapNodeShort.put(node, nodeID);
			mapShortNode.put(nodeID, node);

			if (node.isInvisible()) {
				invisNodes.add(nodeID);
			}
			nodeID++;
		}

		// init mapIOBindings
		final Set<Short> emptyShortSet = new HashSet<Short>();
		mapIOBindings.put(BLANK, null);
		mapIOBindings.put(EMPTYSET, emptyShortSet);
		mapIOBindingsInv.put(null, BLANK);
		mapIOBindingsInv.put(emptyShortSet, EMPTYSET);

		short ioBindingID = (short) (EMPTYSET + 1);
		short possibleBindingID = Short.MIN_VALUE;

		for (FlexNode node : setNodes) {
			Short currNodeID = mapNodeShort.get(node);
			Set<Short> possibleBindingForThisNode = new HashSet<Short>();

			Set<SetFlex> inputBindings = node.getInputNodes();
			assert (inputBindings != null); // input binding is not null
			// node that can fire anytime: empty SetFlex is a member of inputBindings
			// node that cannot fire: there is no member of inputBindings (size == 0)

			Set<SetFlex> outputBindings = node.getOutputNodes();
			assert (outputBindings != null);

			if ((inputBindings.size() > 0) && (outputBindings.size() > 0)) {
				for (SetFlex inputBinding : inputBindings) {
					Short encodedInputBinding = getInternalIOEncoding(inputBinding, ioBindingID);
					if (ioBindingID == encodedInputBinding) {
						ioBindingID++;
					}

					for (SetFlex outputBinding : outputBindings) {
						Short encodedOutputBinding = getInternalIOEncoding(outputBinding, ioBindingID);
						if (ioBindingID == encodedOutputBinding) {
							ioBindingID++;
						}

						FlexBinding binding = new FlexBinding(encodedInputBinding, currNodeID, encodedOutputBinding);
						mapCodeToBinding.put(possibleBindingID, binding);

						possibleBindingForThisNode.add(possibleBindingID);

						if (encodedInputBinding == EMPTYSET) {
							bindingAnytimePossiblyFire.add(possibleBindingID);
						}

						possibleBindingID++;
					}
				}
			} else if (inputBindings.size() > 0) {
				// output binding size = 0
				for (SetFlex inputBinding : inputBindings) {
					Short encodedInputBinding = getInternalIOEncoding(inputBinding, ioBindingID);
					if (ioBindingID == encodedInputBinding) {
						ioBindingID++;
					}

					FlexBinding binding = new FlexBinding(encodedInputBinding, currNodeID, BLANK);
					mapCodeToBinding.put(possibleBindingID, binding);

					possibleBindingForThisNode.add(possibleBindingID);

					if (encodedInputBinding == EMPTYSET) {
						bindingAnytimePossiblyFire.add(possibleBindingID);
					}

					possibleBindingID++;
				}
			} else if (outputBindings.size() > 0) {
				for (SetFlex outputBinding : outputBindings) {
					Short encodedOutputBinding = getInternalIOEncoding(outputBinding, ioBindingID);
					if (ioBindingID == encodedOutputBinding) {
						ioBindingID++;
					}

					FlexBinding binding = new FlexBinding(BLANK, currNodeID, encodedOutputBinding);
					mapCodeToBinding.put(possibleBindingID, binding);

					possibleBindingForThisNode.add(possibleBindingID);

					possibleBindingID++;
				}
			} else { // no input and output binding
				FlexBinding binding = new FlexBinding(BLANK, currNodeID, BLANK);
				mapCodeToBinding.put(possibleBindingID, binding);

				possibleBindingForThisNode.add(possibleBindingID);

				possibleBindingID++;
			}

			// update possible bindings for this node
			possibleNodeBindings.put(currNodeID, possibleBindingForThisNode);
		}

		testFlexCodec(flex);
	}

	public void printBinding(FlexBinding existingBinding) {
		short encInputBinding = existingBinding.getEncodedInputBinding();
		if (encInputBinding == FlexCodec.BLANK) {
			System.out.print("[BLANK] ");
		} else if (encInputBinding == FlexCodec.EMPTYSET) {
			System.out.print("[EMPTYSET] ");
		} else {
			for (Short node : getIOBindingsFor(encInputBinding)) {
				System.out.print(decode(node).getLabel());
				System.out.print(" ");
			}
		}
		System.out.print("|");
		System.out.print(decode(existingBinding.getEncodedNode()).getLabel());
		System.out.print("|");

		short encOutputBinding = existingBinding.getEncodedOutputBinding();
		if (encOutputBinding == FlexCodec.BLANK) {
			System.out.print("[BLANK] ");
		} else if (encOutputBinding == FlexCodec.EMPTYSET) {
			System.out.print("[EMPTYSET] ");
		} else {
			for (Short node : getIOBindingsFor(encOutputBinding)) {
				System.out.print(decode(node).getLabel());
				System.out.print(" ");
			}
		}
		System.out.println();
		System.out.println();
	}

	public void printCodec() {
		System.out.println("----------------------------- CODEC BINDING CONTENT ------------------------");
		for (Short key : mapCodeToBinding.keySet()) {
			FlexBinding existingBinding = mapCodeToBinding.get(key);
			System.out.println("Key : " + key);
			printBinding(existingBinding);
		}
		System.out.println("----------------------------- CODEC BINDING CONTENT ------------------------");
	}

	public void testFlexCodec(Flex flex) {
		// all flex node is mapped
		for (FlexNode node : flex.getNodes()) {
			assert (mapNodeShort.get(node) != null);
			assert (mapShortNode.get(mapNodeShort.get(node)).equals(node));
		}
	}

	public Short getEncIOBindingFor(Set<Short> originalSet) {
		return mapIOBindingsInv.get(originalSet);
	}

	private Short getInternalIOEncoding(SetFlex binding, Short biggestIDForBinding) {
		if (binding == null) {
			return BLANK;
		} else if (binding.size() == 0) {
			return EMPTYSET;
		} else {
			Set<Short> setShort = new HashSet<Short>(binding.size());
			for (FlexNode node : binding) {
				setShort.add(mapNodeShort.get(node));
			}

			if (mapIOBindingsInv.get(setShort) != null) {
				return mapIOBindingsInv.get(setShort);
			} else {
				// this is new io encoding
				mapIOBindings.put(biggestIDForBinding, setShort);
				mapIOBindingsInv.put(setShort, biggestIDForBinding);
			}
			return biggestIDForBinding;
		}
	}

	public Set<Short> getEncodedNodes() {
		return mapShortNode.keySet();
	}

	public Set<FlexNode> getFlexNodes() {
		return mapNodeShort.keySet();
	}

	public short encode(FlexNode node) {
		return mapNodeShort.get(node);
	}

	public FlexNode decode(short value) {
		return mapShortNode.get(value);
	}

	public String toString() {
		String res = "";
		res += "FlexNode --> Short \n";
		for (FlexNode ev : mapNodeShort.keySet()) {
			res += ev.toString() + " --> " + mapNodeShort.get(ev).toString() + "\n";
		}
		res += "-------------------------------------------";
		res += "Short --> FlexNode \n";
		for (Short sh : mapShortNode.keySet()) {
			res += sh + " --> " + mapShortNode.get(sh).toString() + "\n";
		}
		res += "-------------------------------------------";
		return res;
	}

	public Set<Short> getBindingAnytimePossiblyFire() {
		return this.bindingAnytimePossiblyFire;
	}

	public Set<Short> getInvisNodes() {
		return this.invisNodes;
	}

	public Short getEncodedFlexBindingFor(FlexBinding binding) {
		for (Short keySet : mapCodeToBinding.keySet()) {
			FlexBinding temp = mapCodeToBinding.get(keySet);
			if (binding.getEncodedInputBinding().compareTo(temp.getEncodedInputBinding()) == 0) {
				if (binding.getEncodedNode().compareTo(temp.getEncodedNode()) == 0) {
					if (binding.getEncodedOutputBinding().compareTo(temp.getEncodedOutputBinding()) == 0) {
						return keySet;
					}
				}
			}
		}
		return null;
	}

	public void printAllIO() {
		System.out.println("PRINT IO");
		for (Short encodedIO : mapIOBindings.keySet()) {
			System.out.println("Key : " + encodedIO);
			System.out.print("value : ");
			Set<Short> temp = null;
			if ((temp = mapIOBindings.get(encodedIO)) == null) {
				System.out.println(FlexCodec.BLANK + "BLANK");
			} else {
				if (temp.size() == 0) {
					System.out.println(FlexCodec.EMPTYSET + "EMPTYSET");
				} else {
					for (Short val : temp) {
						System.out.print(val + "(" + decode(val).getLabel() + ")");
					}
				}
			}
			System.out.println();
			System.out.println();
		}
	}
}
