package oomdptb.oomdp.visualizer;

import java.awt.Graphics2D;

import oomdptb.oomdp.Domain;
import oomdptb.oomdp.ObjectInstance;

public abstract class ObjectPainter {

	protected Domain 		domain;
	
	
	public ObjectPainter(Domain domain){
		this.domain = domain;
	}
	
	public void setDomain(Domain domain){
		this.domain = domain;
	}
	
	
	
	/**
	 * @param g2 graphics context to which the object should be painted
	 * @param ob the instantiated object to be painted
	 * @param cWidth width of the canvas size
	 * @param cHeight height of the canvas size
	 */
	public abstract void paintObject(Graphics2D g2, ObjectInstance ob, float cWidth, float cHeight);
	
	
}