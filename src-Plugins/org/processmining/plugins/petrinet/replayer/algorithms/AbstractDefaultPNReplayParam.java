/**
 * 
 */
package org.processmining.plugins.petrinet.replayer.algorithms;


/**
 * @author aadrians
 * Oct 21, 2011
 *
 */
public abstract class AbstractDefaultPNReplayParam implements IPNReplayParameter{

	protected boolean createConn = false;
	protected boolean guiMode = false;
	
	/**
	 * Return true if connections need to be made after replay is finished
	 */
	public boolean isCreatingConn() {
		return createConn;
	}

	/**
	 * Return true if GUI is used
	 */
	public boolean isGUIMode() {
		return guiMode;
	}
	
	/**
	 * value is true if later the algorithm is expected to give GUI notification
	 */
	public void setGUIMode(boolean value){
		this.guiMode = value;
	}
	
	/**
	 * value is true if the replay result of the algorithm is expected to be visualized
	 */
	public void setCreateConn(boolean value){
		this.createConn = value;
	}

}
