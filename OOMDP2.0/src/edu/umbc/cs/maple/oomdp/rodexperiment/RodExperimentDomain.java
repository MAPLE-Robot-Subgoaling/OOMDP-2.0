package edu.umbc.cs.maple.oomdp.rodexperiment;

import edu.umbc.cs.maple.domain.oomdp.DomainGenerator;
import edu.umbc.cs.maple.oomdp.Action;
import edu.umbc.cs.maple.oomdp.Attribute;
import edu.umbc.cs.maple.oomdp.Domain;
import edu.umbc.cs.maple.oomdp.ObjectClass;
import edu.umbc.cs.maple.oomdp.PropositionalFunction;
import edu.umbc.cs.maple.oomdp.State;
import edu.umbc.cs.maple.oomdp.ObjectInstance;

public class RodExperimentDomain implements DomainGenerator {

	public static final String				XATTNAME = "xAtt"; //x attribute
	public static final String				YATTNAME = "yAtt"; //y attribute

	public static final String				AATTNAME = "angATT"; //Angle of the rod

	public static final String				LATTNAME = "lAtt"; //left boundary 
	public static final String				RATTNAME = "rAtt"; //right boundary
	public static final String				BATTNAME = "bAtt"; //bottom boundary
	public static final String				TATTNAME = "tAtt"; //top boundary

	//all the objects in the domain
	public static final String				AGENTCLASS = "agent";
	public static final String				OBSTACLECLASS = "obstacle";
	public static final String				GOALCLASS = "goal";

	public static final double				XMIN = 0.;
	public static final double				XMAX = 100.;
	public static final double				YMIN = 0.;
	public static final double				YMAX = 50.;
	public static final double				ANGLEMAX = Math.PI/2.;
	public static final double 				ANGLEINC = Math.PI/18;


	//All the actions available to the agent
	public static final String				ACTIONMOVEUP = "moveUp"; //moves one unit up
	public static final String				ACTIONMOVEDOWN = "moveDown"; //moves one unit down
	public static final String				ACTIONTURNLEFT = "upThrust"; //rotates 10 degrees
	public static final String				ACTIONTURNRIGHT = "downThrust"; //rotates -10 degrees

	//Propositional Functions
	public static final String				PFTOUCHGOAL = "touchedGoal";
	public static final String				PFTOUCHSURFACE = "touchingSurface"; //touching an obstacle

	public static Domain					RODDOMAIN = null;	

	public Domain generateDomain(){

		if( RODDOMAIN!= null ){
			return RODDOMAIN;
		}

		RODDOMAIN = new Domain();

		//create attributes
		Attribute xatt = new Attribute(RODDOMAIN, XATTNAME, Attribute.AttributeType.REAL);
		xatt.setLims(XMIN, XMAX);

		Attribute yatt = new Attribute(RODDOMAIN, YATTNAME, Attribute.AttributeType.REAL);
		yatt.setLims(YMIN, YMAX);

		Attribute aatt = new Attribute(RODDOMAIN, AATTNAME, Attribute.AttributeType.REAL);
		aatt.setLims(-ANGLEMAX, ANGLEMAX);

		Attribute latt = new Attribute(RODDOMAIN, LATTNAME, Attribute.AttributeType.REAL);
		latt.setLims(XMIN, XMAX);

		Attribute ratt = new Attribute(RODDOMAIN, RATTNAME, Attribute.AttributeType.REAL);
		ratt.setLims(XMIN, XMAX);

		Attribute batt = new Attribute(RODDOMAIN, BATTNAME, Attribute.AttributeType.REAL);
		batt.setLims(YMIN, YMAX);

		Attribute tatt = new Attribute(RODDOMAIN, TATTNAME, Attribute.AttributeType.REAL);
		tatt.setLims(YMIN, YMAX);

		//create classes
		ObjectClass agentclass = new ObjectClass(RODDOMAIN, AGENTCLASS);
		agentclass.addAttribute(xatt);
		agentclass.addAttribute(yatt);
		agentclass.addAttribute(aatt);

		ObjectClass obstclss = new ObjectClass(RODDOMAIN, OBSTACLECLASS);
		obstclss.addAttribute(latt);
		obstclss.addAttribute(ratt);
		obstclss.addAttribute(batt);
		obstclss.addAttribute(tatt);


		ObjectClass goalclass = new ObjectClass(RODDOMAIN, GOALCLASS);
		goalclass.addAttribute(latt);
		goalclass.addAttribute(ratt);
		goalclass.addAttribute(batt);
		goalclass.addAttribute(tatt);

		//Initialize actions
		Action moveUp = new ActionMoveUp(ACTIONMOVEUP, RODDOMAIN, "");
		Action moveDown = new ActionMoveDown(ACTIONMOVEDOWN, RODDOMAIN, "");
		Action turnLeft = new ActionTurnLeft(ACTIONTURNLEFT, RODDOMAIN, "");
		Action turnRight = new ActionTurnRight(ACTIONTURNRIGHT, RODDOMAIN, "");

		//add pfs
		PropositionalFunction touchGoal = new TouchGoalPF(PFTOUCHGOAL, RODDOMAIN, new String[]{AGENTCLASS, GOALCLASS});
		PropositionalFunction touchSurface = new TouchSurfacePF(PFTOUCHSURFACE, RODDOMAIN, new String[]{AGENTCLASS, OBSTACLECLASS});

		return RODDOMAIN;

	}

	public static void updateMotion(State st, double change) {

		ObjectInstance agent = st.getObjectsOfTrueClass(AGENTCLASS).get(0);
		double ang = agent.getRealValForAttribute(AATTNAME);
		double x = agent.getRealValForAttribute(XATTNAME);
		double y = agent.getRealValForAttribute(YATTNAME);

		double worldAngle = (Math.PI/2.) - ang;

		double tx = Math.cos(worldAngle)*change;
		double ty = Math.sin(worldAngle)*change;

		tx = tx + change;
		ty = ty + change;

		if (tx > XMAX || tx < XMIN || ty > YMAX || ty<YMIN){
			tx = x - change;
			ty = y - change;
		}

		//hits obstacles
		ObjectInstance obstacle = st.getObjectsOfTrueClass(OBSTACLECLASS).get(0);
		double l = obstacle.getRealValForAttribute(LATTNAME);
		double r = obstacle.getRealValForAttribute(RATTNAME);
		double b = obstacle.getRealValForAttribute(BATTNAME);
		double t = obstacle.getRealValForAttribute(TATTNAME);

		if(ty >= b && ty <= t && tx >=l && tx <= r){
			tx = x - change;
			ty = y - change;
		}

		agent.setValue(XATTNAME, tx);
		agent.setValue(YATTNAME, ty);
		agent.setValue(AATTNAME, ang);


	}

	public static void incAngle(State st, int dir) {
		ObjectInstance agent = st.getObjectsOfTrueClass(AGENTCLASS).get(0);
		double curA = agent.getRealValForAttribute(AATTNAME);

		double newa = curA + (dir * ANGLEINC);
		if(newa > ANGLEMAX){
			newa = ANGLEMAX;
		}
		else if(newa < -ANGLEMAX){
			newa = -ANGLEMAX;
		}

		agent.setValue(AATTNAME, newa);

	}

	public class ActionMoveUp extends Action{

		public ActionMoveUp(String name, Domain domain, String parameterClasses){
			super(name, domain, parameterClasses);
		}

		public ActionMoveUp(String name, Domain domain, String [] parameterClasses) {
			super(name, domain, parameterClasses);
		}

		@Override
		protected State performActionHelper(State st, String[] params) {
			RodExperimentDomain.updateMotion(st, 1.0);
			return st;
		}
	}

	public class ActionMoveDown extends Action{

		public ActionMoveDown(String name, Domain domain, String parameterClasses){
			super(name, domain, parameterClasses);
		}

		public ActionMoveDown(String name, Domain domain, String [] parameterClasses) {
			super(name, domain, parameterClasses);
		}

		@Override
		protected State performActionHelper(State st, String[] params) {
			RodExperimentDomain.updateMotion(st, -1.0);
			return st;
		}
	}

	public class ActionTurnRight extends Action{

		public ActionTurnRight(String name, Domain domain, String parameterClasses){
			super(name, domain, parameterClasses);
		}

		public ActionTurnRight(String name, Domain domain, String [] parameterClasses) {
			super(name, domain, parameterClasses);
		}

		@Override
		protected State performActionHelper(State st, String[] params) {
			RodExperimentDomain.incAngle(st, 1);
			RodExperimentDomain.updateMotion(st, 0.0);
			return st;
		}
	}

	public class ActionTurnLeft extends Action{

		public ActionTurnLeft(String name, Domain domain, String parameterClasses){
			super(name, domain, parameterClasses);
		}

		public ActionTurnLeft(String name, Domain domain, String [] parameterClasses) {
			super(name, domain, parameterClasses);
		}

		@Override
		protected State performActionHelper(State st, String[] params) {
			RodExperimentDomain.incAngle(st, -1);
			RodExperimentDomain.updateMotion(st, 0.0);
			return st;
		}
	}

	public class TouchGoalPF extends PropositionalFunction{

		public TouchGoalPF(String name, Domain domain, String parameterClasses) {
			super(name, domain, parameterClasses);
		}

		public TouchGoalPF(String name, Domain domain, String[] parameterClasses) {
			super(name, domain, parameterClasses);
		}

		@Override
		public boolean isTrue(State st, String[] params) {
			ObjectInstance agent = st.getObject(params[0]);
			ObjectInstance goal = st.getObject(params[1]);

			double l = goal.getRealValForAttribute(LATTNAME);
			double r = goal.getRealValForAttribute(RATTNAME);
			double b = goal.getRealValForAttribute(BATTNAME);
			double t = goal.getRealValForAttribute(TATTNAME);

			double x = agent.getRealValForAttribute(XATTNAME);
			double y = agent.getRealValForAttribute(YATTNAME);

			if(x>=l && x<r && y==t){
				return true;
			}

			return false;
		}

	}

	public class TouchSurfacePF extends PropositionalFunction{

		public TouchSurfacePF(String name, Domain domain, String parameterClasses) {
			super(name, domain, parameterClasses);
		}

		public TouchSurfacePF(String name, Domain domain, String [] parameterClasses) {
			super(name, domain, parameterClasses);
		}

		@Override
		public boolean isTrue(State st, String[] params) {


			ObjectInstance agent = st.getObject(params[0]);
			ObjectInstance o = st.getObject(params[1]);
			double x = agent.getRealValForAttribute(XATTNAME);
			double y = agent.getRealValForAttribute(YATTNAME);

			double l = o.getRealValForAttribute(LATTNAME);
			double r = o.getRealValForAttribute(RATTNAME);
			double b = o.getRealValForAttribute(BATTNAME);
			double t = o.getRealValForAttribute(TATTNAME);

			if(x >= l && x <= r && y >= b && y <= t){
				return true;
			}

			return false;
		}



	}

}
