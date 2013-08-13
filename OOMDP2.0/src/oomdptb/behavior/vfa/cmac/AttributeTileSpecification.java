package oomdptb.behavior.vfa.cmac;

import oomdptb.oomdp.Attribute;

public class AttributeTileSpecification {

	public String			className;
	public Attribute		attribute;
	public double			windowSize;
	public double			bucketBoundary;
	
	
	public AttributeTileSpecification(String className, Attribute attribute, double windowSize){
		this.className = className;
		this.attribute = attribute;
		this.windowSize = windowSize;
	}
	
	public AttributeTileSpecification(String className, Attribute attribute, double windowSize, double bucketBoundary) {
		this.className = className;
		this.attribute = attribute;
		this.windowSize = windowSize;
		this.bucketBoundary = bucketBoundary;
	}

}
