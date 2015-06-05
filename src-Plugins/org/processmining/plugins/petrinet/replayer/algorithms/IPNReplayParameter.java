/**
 * 
 */
package org.processmining.plugins.petrinet.replayer.algorithms;

/**
 * @author aadrians
 * Oct 21, 2011
 *
 */
public interface IPNReplayParameter {
	public boolean isCreatingConn();
	public boolean isGUIMode();
	
	public void setGUIMode(boolean value);
	public void setCreateConn(boolean value);
	
}
