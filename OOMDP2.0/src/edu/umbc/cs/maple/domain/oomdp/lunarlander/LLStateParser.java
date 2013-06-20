package edu.umbc.cs.maple.domain.oomdp.lunarlander;

import java.util.List;

import edu.umbc.cs.maple.oomdp.ObjectInstance;
import edu.umbc.cs.maple.oomdp.State;
import edu.umbc.cs.maple.oomdp.StateParser;

public class LLStateParser implements StateParser {

	LunarLanderDomain lld_;
	
	public LLStateParser(){
		lld_ = new LunarLanderDomain();
		lld_.generateDomain();
	}
	
	
	@Override
	public String stateToString(State s) {

		StringBuffer buf = new StringBuffer(256);
		
		ObjectInstance agent = s.getObjectsOfTrueClass(LunarLanderDomain.AGENTCLASS).get(0);
		ObjectInstance pad = s.getObjectsOfTrueClass(LunarLanderDomain.PADCLASS).get(0);
		List <ObjectInstance> obsts = s.getObjectsOfTrueClass(LunarLanderDomain.OBSTACLECLASS);
		
		//write agent
		buf.append(agent.getRealValForAttribute(LunarLanderDomain.AATTNAME)).append(" ");
		buf.append(agent.getRealValForAttribute(LunarLanderDomain.XATTNAME)).append(" ");
		buf.append(agent.getRealValForAttribute(LunarLanderDomain.YATTNAME)).append(" ");
		buf.append(agent.getRealValForAttribute(LunarLanderDomain.VXATTNAME)).append(" ");
		buf.append(agent.getRealValForAttribute(LunarLanderDomain.VYATTNAME)).append("\n");
		
		//write pad
		buf.append(pad.getRealValForAttribute(LunarLanderDomain.LATTNAME)).append(" ");
		buf.append(pad.getRealValForAttribute(LunarLanderDomain.RATTNAME)).append(" ");
		buf.append(pad.getRealValForAttribute(LunarLanderDomain.BATTNAME)).append(" ");
		buf.append(pad.getRealValForAttribute(LunarLanderDomain.TATTNAME));
		
		//write each obstacle
		for(ObjectInstance ob : obsts){
			buf.append("\n").append(ob.getRealValForAttribute(LunarLanderDomain.LATTNAME)).append(" ");
			buf.append(ob.getRealValForAttribute(LunarLanderDomain.RATTNAME)).append(" ");
			buf.append(ob.getRealValForAttribute(LunarLanderDomain.BATTNAME)).append(" ");
			buf.append(ob.getRealValForAttribute(LunarLanderDomain.TATTNAME));
		}
		
		
		return buf.toString();
	}

	@Override
	public State stringToState(String str) {

		str = str.trim();
		
		String [] lineComps = str.split("\n");
		String [] aComps = lineComps[0].split(" ");
		String [] pComps = lineComps[1].split(" ");
		
		State s = lld_.getCleanState(lineComps.length-2);
		
		lld_.setAgent(s, Double.parseDouble(aComps[0]), Double.parseDouble(aComps[1]), Double.parseDouble(aComps[2]), Double.parseDouble(aComps[3]), Double.parseDouble(aComps[4]));
		lld_.setPad(s, Double.parseDouble(pComps[0]), Double.parseDouble(pComps[1]), Double.parseDouble(pComps[2]), Double.parseDouble(pComps[3]));
		
		for(int i = 2; i < lineComps.length; i++){
			String [] oComps = lineComps[i].split(" ");
			lld_.setObstacle(s, i-2, Double.parseDouble(oComps[0]), Double.parseDouble(oComps[1]), Double.parseDouble(oComps[2]), Double.parseDouble(oComps[3]));
		}
		
		return s;

	}

}
