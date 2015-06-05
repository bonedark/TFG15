package org.processmining.plugins.flex.replayer.data;

import java.util.List;

public class FlexExtendedEncodedState  {
	private short newlySelectedFlexNode;
	private List<Short> selectedInputSet;
	private List<Short> selectedOutputSet;
	private List<Short> unsatisfiedEventsInSelectedInputSet;
	private Boolean isMoveOnStartingNode = false;

	public FlexExtendedEncodedState(Short newlySelectedFlexNode, List<Short> selectedInputSet,
			List<Short> selectedOutputSet, List<Short> unsatisfiedEventsInSelectedInputSet) {
		this.newlySelectedFlexNode = newlySelectedFlexNode;
		this.selectedInputSet = selectedInputSet;
		this.selectedOutputSet = selectedOutputSet;
		this.unsatisfiedEventsInSelectedInputSet = unsatisfiedEventsInSelectedInputSet;
	}

	public short getNewlySelectedFlexNode() {
		return newlySelectedFlexNode;
	}

	public void setNewlySelectedFlexNode(short newlySelectedFlexNode) {
		this.newlySelectedFlexNode = newlySelectedFlexNode;
	}

	public List<Short> getSelectedInputSet() {
		return selectedInputSet;
	}

	public void setSelectedInputSet(List<Short> selectedInputSet) {
		this.selectedInputSet = selectedInputSet;
	}

	public List<Short> getSelectedOutputSet() {
		return selectedOutputSet;
	}

	public void setSelectedOutputSet(List<Short> selectedOutputSet) {
		this.selectedOutputSet = selectedOutputSet;
	}

	public List<Short> getUnsatisfiedEventsInSelectedInputSet() {
		return unsatisfiedEventsInSelectedInputSet;
	}

	public void setUnsatisfiedEventsInSelectedInputSet(
			List<Short> unsatisfiedEventsInSelectedInputSet) {
		this.unsatisfiedEventsInSelectedInputSet = unsatisfiedEventsInSelectedInputSet;
	}
	
	public Boolean getIsMoveOnStartingNode() {
		return isMoveOnStartingNode;
	}

	public void setIsMoveOnStartingNode(Boolean isMove) {
		this.isMoveOnStartingNode = isMove;
	}
}
