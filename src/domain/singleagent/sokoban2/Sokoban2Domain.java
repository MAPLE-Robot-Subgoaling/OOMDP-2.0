package domain.singleagent.sokoban2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.singleagent.EpisodeSequenceVisualizer;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.singleagent.learning.LearningAgent;
import burlap.behavior.singleagent.learning.tdmethods.QLearning;
import burlap.behavior.singleagent.options.LocalSubgoalRF;
import burlap.behavior.singleagent.options.LocalSubgoalTF;
import burlap.behavior.singleagent.options.Option;
import burlap.behavior.singleagent.options.SubgoalOption;
import burlap.behavior.singleagent.planning.OOMDPPlanner;
import burlap.behavior.singleagent.planning.StateConditionTest;
import burlap.behavior.singleagent.planning.StateConditionTestIterable;
import burlap.behavior.singleagent.planning.commonpolicies.GreedyDeterministicQPolicy;
import burlap.behavior.singleagent.planning.stochastic.valueiteration.ValueIteration;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.Attribute;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.ObjectClass;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.PropositionalFunction;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.RewardFunction;
import burlap.oomdp.singleagent.SADomain;
import burlap.oomdp.singleagent.common.SinglePFTF;
import burlap.oomdp.visualizer.Visualizer;

public class Sokoban2Domain implements DomainGenerator {
    public class AgentClass extends ObjectClass {
	public AgentClass(Domain domain, String name) {
	    this(domain, name, false);
	}

	public AgentClass(Domain domain, String name, boolean hidden) {
	    super(domain, name, hidden);
	}
    }

    public class BlockClass extends ObjectClass {
	public BlockClass(Domain domain, String name) {
	    this(domain, name, false);
	}

	public BlockClass(Domain domain, String name, boolean hidden) {
	    super(domain, name, hidden);
	}
    }

    public class RoomClass extends ObjectClass {
	public RoomClass(Domain domain, String name) {
	    this(domain, name, false);
	}

	public RoomClass(Domain domain, String name, boolean hidden) {
	    super(domain, name, hidden);
	}
    }

    public class DoorClass extends ObjectClass {
	public DoorClass(Domain domain, String name) {
	    this(domain, name, false);
	}

	public DoorClass(Domain domain, String name, boolean hidden) {
	    super(domain, name, hidden);
	}
    }

    public static final String ATTX = "x";
    public static final String ATTY = "y";
    public static final String ATTDIR = "direction"; // optionally added
    // attribute to include the
    // agent's direction
    public static final String ATTTOP = "top";
    public static final String ATTLEFT = "left";
    public static final String ATTBOTTOM = "bottom";
    public static final String ATTRIGHT = "right";
    public static final String ATTCOLOR = "color";
    public static final String ATTSHAPE = "shape";

    public static final String CLASSAGENT = "agent";
    public static final String CLASSBLOCK = "block";
    public static final String CLASSROOM = "room";
    public static final String CLASSDOOR = "door";

    public static final String ACTIONNORTH = "north";
    public static final String ACTIONSOUTH = "south";
    public static final String ACTIONEAST = "east";
    public static final String ACTIONWEST = "west";

    public static final String PFAGENTINROOM = "agentInRoom";
    public static final String PFBLOCKINROOM = "blockInRoom";
    public static final String PFAGENTINDOOR = "agentInDoor";
    public static final String PFBLOCKINDOOR = "blockInDoor";
    public static final String PFBLOCKONWALL = "blockOnWall";

    public static final String PFATGOAL = "atGoal";

    public static final String[] COLORS = new String[] { "blue", "green",
	    "magenta", "red", "yellow" };

    public static final String[] SHAPES = new String[] { "chair", "bag",
	    "backpack", "basket" };

    public static final String[] DIRECTIONS = new String[] { "north", "south",
	    "east", "west" };

    protected static final String PFRCOLORBASE = "roomIs";
    protected static final String PFBCOLORBASE = "blockIs";
    protected static final String PFBSHAPEBASE = "shape";

    protected int maxX = 24;
    protected int maxY = 24;
    protected boolean includeDirectionAttribute = false;;

    // for the learning algorithm
    public static final double LEARNINGRATE = 0.9999;
    public static final double DISCOUNTFACTOR = 0.999995;
    public static final double LAMBDA = 1.0;

    public static void main(String[] args) {
	Sokoban2Domain dgen = new Sokoban2Domain();
	// dgen.includeDirectionAttribute(true);
	Domain domain = dgen.generateDomain();
	State s = getCleanState(domain, 3, 2, 1);

	setRoom(s, 0, 4, 0, 0, 8, "red");
	setRoom(s, 1, 8, 0, 4, 4, "green");
	setRoom(s, 2, 8, 4, 4, 8, "blue");

	setDoor(s, 0, 4, 6, 4, 6);
	setDoor(s, 1, 4, 2, 4, 2);

	setAgent(s, 6, 6);
	setBlock(s, 0, 2, 6, "backpack", "red");

	DiscreteStateHashFactory hashFactory = new DiscreteStateHashFactory();
	hashFactory.setAttributesForClass(CLASSAGENT,
		domain.getObjectClass(CLASSAGENT).attributeList);
	RewardFunction rf = new Sokoban2RF(1000, -1000, false);
	// RewardFunction rf = new UniformCostRF();
	TerminalFunction tf = new SinglePFTF(domain.getPropFunction(PFATGOAL));
	LearningAgent Q = new QLearning(domain, rf, tf,
		Sokoban2Domain.DISCOUNTFACTOR, hashFactory, 0.0,
		Sokoban2Domain.LEARNINGRATE, 1000);
	Sokoban2Parser parser = new Sokoban2Parser(domain);
	EpisodeAnalysis analyzer;
	for (int i = 1; i <= 100; i++) {
	    analyzer = new EpisodeAnalysis();

	    System.out.print("Episode " + i + ": ");
	    analyzer = Q.runLearningEpisodeFrom(s);
	    System.out.println("\tSteps: " + analyzer.numTimeSteps());
	    analyzer.writeToFile(String.format("output/e%03d", i), parser);
	}

	Visualizer v = Sokoban2Visualizer.getVisualizer("img");
	new EpisodeSequenceVisualizer(v, domain, parser, "output");
    }

    public void setMaxX(int maxX) {
	this.maxX = maxX;
    }

    public void setMaxY(int maxY) {
	this.maxY = maxY;
    }

    public void includeDirectionAttribute(boolean includeDirectionAttribute) {
	this.includeDirectionAttribute = includeDirectionAttribute;
    }

    /**
     * note: modified to include items for learning algorithms
     */
    @SuppressWarnings("unused")
    @Override
    public Domain generateDomain() {

	SADomain domain = new SADomain();

	// declare the attributes associated with the domain
	Attribute xatt = new Attribute(domain, ATTX,
		Attribute.AttributeType.DISC);
	xatt.setDiscValuesForRange(0, maxX, 1);

	Attribute yatt = new Attribute(domain, ATTY,
		Attribute.AttributeType.DISC);
	yatt.setDiscValuesForRange(0, maxY, 1);

	Attribute topAtt = new Attribute(domain, ATTTOP,
		Attribute.AttributeType.DISC);
	topAtt.setDiscValuesForRange(0, maxY, 1);

	Attribute leftAtt = new Attribute(domain, ATTLEFT,
		Attribute.AttributeType.DISC);
	leftAtt.setDiscValuesForRange(0, maxX, 1);

	Attribute bottomAtt = new Attribute(domain, ATTBOTTOM,
		Attribute.AttributeType.DISC);
	bottomAtt.setDiscValuesForRange(0, maxY, 1);

	Attribute rightAtt = new Attribute(domain, ATTRIGHT,
		Attribute.AttributeType.DISC);
	rightAtt.setDiscValuesForRange(0, maxX, 1);

	Attribute colAtt = new Attribute(domain, ATTCOLOR,
		Attribute.AttributeType.DISC);
	colAtt.setDiscValues(COLORS);

	Attribute shapeAtt = new Attribute(domain, ATTSHAPE,
		Attribute.AttributeType.DISC);
	shapeAtt.setDiscValues(SHAPES);

	// add the objects to the domain
	/*
	 * domain.addAttribute(shapeAtt); domain.addAttribute(colAtt);
	 * domain.addAttribute(rightAtt); domain.addAttribute(bottomAtt);
	 * domain.addAttribute(leftAtt); domain.addAttribute(topAtt);
	 * domain.addAttribute(yatt); domain.addAttribute(xatt);
	 */

	if (this.includeDirectionAttribute) {
	    Attribute dirAtt = new Attribute(domain, ATTDIR,
		    Attribute.AttributeType.DISC);
	    dirAtt.setDiscValues(DIRECTIONS);
	    // domain.addAttribute(dirAtt); // add any existing attributes to
	    // the
	    // domain
	}

	// declare the objects associated with the domain
	AgentClass agent = new AgentClass(domain, CLASSAGENT);
	agent.addAttribute(xatt);
	agent.addAttribute(yatt);
	if (this.includeDirectionAttribute) {
	    agent.addAttribute(domain.getAttribute(ATTDIR));
	}

	BlockClass block = new BlockClass(domain, CLASSBLOCK);
	block.addAttribute(xatt);
	block.addAttribute(yatt);
	block.addAttribute(colAtt);
	block.addAttribute(shapeAtt);

	if (this.includeDirectionAttribute) {
	    block.addAttribute(domain.getAttribute(ATTDIR));
	}

	RoomClass room = new RoomClass(domain, CLASSROOM);
	this.addRectAtts(domain, room);
	room.addAttribute(colAtt);

	DoorClass door = new DoorClass(domain, CLASSDOOR);
	this.addRectAtts(domain, door);

	// add the objects to the domain
	/*
	 * domain.addObjectClass(block); domain.addObjectClass(door);
	 * domain.addObjectClass(room); domain.addObjectClass(agent);
	 */

	// declare the actions
	Action northAction = new MovementAction(ACTIONNORTH, domain, 0, 1);
	Action southAction = new MovementAction(ACTIONSOUTH, domain, 0, -1);
	Action eastAction = new MovementAction(ACTIONEAST, domain, 1, 0);
	Action westAction = new MovementAction(ACTIONWEST, domain, -1, 0);

	// add the actions to the domain
	/*
	 * domain.addAction(westAction); domain.addAction(eastAction);
	 * domain.addAction(southAction); domain.addAction(northAction);
	 */

	// declare + add the propositional functions
	PropositionalFunction air = new PFInRegion(PFAGENTINROOM, domain,
		new String[] { CLASSAGENT, CLASSROOM }, true);
	PropositionalFunction bir = new PFInRegion(PFBLOCKINROOM, domain,
		new String[] { CLASSBLOCK, CLASSROOM }, true);
	PropositionalFunction bow = new PFOnWall(PFBLOCKONWALL, domain,
		new String[] { CLASSBLOCK, CLASSROOM });

	// My PropFunction
	PropositionalFunction goal = new PFGoal(PFATGOAL, domain, new String[] {
		CLASSAGENT, CLASSBLOCK, CLASSROOM });

	/*
	 * domain.addPropositionalFunction(bir);
	 * domain.addPropositionalFunction(air);
	 * domain.addPropositionalFunction(bow);
	 * domain.addPropositionalFunction(goal);
	 */

	PropositionalFunction aid = new PFInRegion(PFAGENTINDOOR, domain,
		new String[] { CLASSAGENT, CLASSDOOR }, false);
	PropositionalFunction bid = new PFInRegion(PFBLOCKINDOOR, domain,
		new String[] { CLASSBLOCK, CLASSDOOR }, false);

	/*
	 * domain.addPropositionalFunction(aid);
	 * domain.addPropositionalFunction(bid);
	 */

	for (String col : COLORS) {
	    PropositionalFunction pfr = new PFIsColor(PFRoomColorName(col),
		    domain, new String[] { CLASSROOM }, col);
	    PropositionalFunction pfb = new PFIsColor(PFBlockColorName(col),
		    domain, new String[] { CLASSBLOCK }, col);
	    /*
	     * domain.addPropositionalFunction(pfb);
	     * domain.addPropositionalFunction(pfr);
	     */
	}

	for (String shape : SHAPES) {
	    PropositionalFunction pf = new PFIsShape(PFBlockShapeName(shape),
		    domain, new String[] { CLASSBLOCK }, shape);
	    // domain.addPropositionalFunction(pf);
	}

	return domain;
    }

    protected void addRectAtts(Domain domain, ObjectClass oc) {
	oc.addAttribute(domain.getAttribute(ATTTOP));
	oc.addAttribute(domain.getAttribute(ATTLEFT));
	oc.addAttribute(domain.getAttribute(ATTBOTTOM));
	oc.addAttribute(domain.getAttribute(ATTRIGHT));
    }

    public static String PFRoomColorName(String color) {
	String capped = firstLetterCapped(color);
	return PFRCOLORBASE + capped;
    }

    public static String PFBlockColorName(String color) {
	String capped = firstLetterCapped(color);
	return PFBCOLORBASE + capped;
    }

    public static String PFBlockShapeName(String shape) {
	String capped = firstLetterCapped(shape);
	return PFBSHAPEBASE + capped;
    }

    public static State getCleanState(Domain domain, int nRooms, int nDoors,
	    int nBlocks) {

	State s = new State();

	// create rooms
	createNInstances(domain, s, CLASSROOM, nRooms);

	// now create doors
	createNInstances(domain, s, CLASSDOOR, nDoors);

	// now create blocks
	createNInstances(domain, s, CLASSBLOCK, nBlocks);

	// create agent
	ObjectInstance o = new ObjectInstance(
		domain.getObjectClass(CLASSAGENT), CLASSAGENT + 0);
	s.addObject(o);

	Attribute dirAtt = o.getObjectClass().getAttribute(ATTDIR);
	if (dirAtt != null) {
	    o.setValue(ATTDIR, "south");
	}

	return s;

    }

    public static State getClassicState(Domain domain) {

	State s = getCleanState(domain, 3, 2, 1);

	setRoom(s, 0, 4, 0, 0, 8, "red");
	setRoom(s, 1, 8, 0, 4, 4, "green");
	setRoom(s, 2, 8, 4, 4, 8, "blue");

	setDoor(s, 0, 4, 6, 4, 6);
	setDoor(s, 1, 4, 2, 4, 2);

	setAgent(s, 6, 6);
	setBlock(s, 0, 2, 2, "basket", "red");

	return s;

    }

    public static void setAgent(State s, int x, int y) {
	ObjectInstance o = s.getFirstObjectOfClass(CLASSAGENT);
	o.setValue(ATTX, x);
	o.setValue(ATTY, y);
    }

    public static void setBlockPos(State s, int i, int x, int y) {
	ObjectInstance o = s.getObjectsOfTrueClass(CLASSBLOCK).get(i);
	o.setValue(ATTX, x);
	o.setValue(ATTY, y);
    }

    public static void setBlock(State s, int i, int x, int y, String shape,
	    String color) {
	ObjectInstance o = s.getObjectsOfTrueClass(CLASSBLOCK).get(i);
	setBlock(o, x, y, shape, color);
    }

    public static void setBlock(ObjectInstance o, int x, int y, String shape,
	    String color) {
	o.setValue(ATTX, x);
	o.setValue(ATTY, y);
	o.setValue(ATTSHAPE, shape);
	o.setValue(ATTCOLOR, color);
    }

    public static void setRoom(State s, int i, int top, int left, int bottom,
	    int right, String color) {
	ObjectInstance o = s.getObjectsOfTrueClass(CLASSROOM).get(i);
	setRegion(o, top, left, bottom, right);
	o.setValue(ATTCOLOR, color);
    }

    public static void setDoor(State s, int i, int top, int left, int bottom,
	    int right) {
	ObjectInstance o = s.getObjectsOfTrueClass(CLASSDOOR).get(i);
	setRegion(o, top, left, bottom, right);
    }

    public static void setRoom(ObjectInstance o, int top, int left, int bottom,
	    int right, String color) {
	setRegion(o, top, left, bottom, right);
	o.setValue(ATTCOLOR, color);
    }

    public static void setRegion(ObjectInstance o, int top, int left,
	    int bottom, int right) {
	o.setValue(ATTTOP, top);
	o.setValue(ATTLEFT, left);
	o.setValue(ATTBOTTOM, bottom);
	o.setValue(ATTRIGHT, right);
    }

    protected static void createNInstances(Domain domain, State s,
	    String className, int n) {
	for (int i = 0; i < n; i++) {
	    ObjectInstance o = new ObjectInstance(
		    domain.getObjectClass(className), className + i);
	    s.addObject(o);
	}
    }

    public static int maxRoomXExtent(State s) {

	int max = 0;
	List<ObjectInstance> rooms = s.getObjectsOfTrueClass(CLASSROOM);
	for (ObjectInstance r : rooms) {
	    int right = r.getDiscValForAttribute(ATTRIGHT);
	    if (right > max) {
		max = right;
	    }
	}

	return max;
    }

    public static int maxRoomYExtent(State s) {

	int max = 0;
	List<ObjectInstance> rooms = s.getObjectsOfTrueClass(CLASSROOM);
	for (ObjectInstance r : rooms) {
	    int top = r.getDiscValForAttribute(ATTTOP);
	    if (top > max) {
		max = top;
	    }
	}

	return max;
    }

    protected static String firstLetterCapped(String s) {
	String firstLetter = s.substring(0, 1);
	String remainder = s.substring(1);
	return firstLetter.toUpperCase() + remainder;
    }

    public static ObjectInstance roomContainingPoint(State s, int x, int y) {
	List<ObjectInstance> rooms = s.getObjectsOfTrueClass(CLASSROOM);
	return regionContainingPoint(rooms, x, y);
    }

    public static ObjectInstance doorContainingPoint(State s, int x, int y) {
	List<ObjectInstance> doors = s.getObjectsOfTrueClass(CLASSDOOR);
	return regionContainingPoint(doors, x, y);
    }

    protected static ObjectInstance regionContainingPoint(
	    List<ObjectInstance> objects, int x, int y) {
	for (ObjectInstance o : objects) {
	    if (regionContainsPoint(o, x, y)) {
		return o;
	    }

	}

	return null;
    }

    public static boolean regionContainsPoint(ObjectInstance o, int x, int y) {
	int top = o.getDiscValForAttribute(ATTTOP);
	int left = o.getDiscValForAttribute(ATTLEFT);
	int bottom = o.getDiscValForAttribute(ATTBOTTOM);
	int right = o.getDiscValForAttribute(ATTRIGHT);

	if (y >= bottom && y <= top && x >= left && x <= right) {
	    return true;
	}

	return false;
    }

    public static ObjectInstance blockAtPoint(State s, int x, int y) {

	List<ObjectInstance> blocks = s.getObjectsOfTrueClass(CLASSBLOCK);
	for (ObjectInstance b : blocks) {
	    int bx = b.getDiscValForAttribute(ATTX);
	    int by = b.getDiscValForAttribute(ATTY);

	    if (bx == x && by == y) {
		return b;
	    }
	}

	return null;

    }

    public static boolean wallAt(State s, ObjectInstance r, int x, int y) {

	int top = r.getDiscValForAttribute(ATTTOP);
	int left = r.getDiscValForAttribute(ATTLEFT);
	int bottom = r.getDiscValForAttribute(ATTBOTTOM);
	int right = r.getDiscValForAttribute(ATTRIGHT);

	// agent along wall of room check
	if (((x == left || x == right) && y >= bottom && y <= top)
		|| ((y == bottom || y == top) && x >= left && x <= right)) {

	    // then only way for this to be a valid pos is if a door contains
	    // this point
	    ObjectInstance door = doorContainingPoint(s, x, y);
	    if (door == null) {
		return true;
	    }

	}

	return false;
    }

    public static boolean wallCheck(State s, ObjectInstance room, int x, int y) {
	int top = room.getDiscValForAttribute(ATTTOP);
	int bottom = room.getDiscValForAttribute(ATTBOTTOM);
	int left = room.getDiscValForAttribute(ATTLEFT);
	int right = room.getDiscValForAttribute(ATTRIGHT);

	// agent along wall of room check
	if (((x == left || x == right) && y >= bottom && y <= top)
		|| ((y == bottom || y == top) && x >= left && x <= right)) {

	    // then only way for this to be a valid pos is if a door contains
	    // this point
	    ObjectInstance door = doorContainingPoint(s, x, y);
	    if (door == null) {
		return true;
	    }

	}

	return false;
    }

    public class MovementAction extends Action {

	protected int xdelta;
	protected int ydelta;

	public MovementAction(String name, Domain domain, int xdelta, int ydelta) {
	    super(name, domain, "");
	    this.xdelta = xdelta;
	    this.ydelta = ydelta;
	}

	@Override
	protected State performActionHelper(State s, String[] params) {

	    ObjectInstance agent = s.getFirstObjectOfClass(CLASSAGENT);
	    int ax = agent.getDiscValForAttribute(ATTX);
	    int ay = agent.getDiscValForAttribute(ATTY);

	    int nx = ax + xdelta;
	    int ny = ay + ydelta;

	    // ObjectInstance roomContaining = roomContainingPoint(s, ax, ay);

	    boolean permissibleMove = false;
	    // ObjectInstance pushedBlock = blockAtPoint(s, nx, ny);

	    List<ObjectInstance> blocks = new ArrayList<ObjectInstance>();
	    ObjectInstance nextBlock = null;
	    int lastX = nx, lastY = ny;
	    while ((nextBlock = blockAtPoint(s, lastX, lastY)) != null) {
		blocks.add(nextBlock);
		lastX += xdelta;
		lastY += ydelta;
	    }

	    ObjectInstance roomWithLastBlock = roomContainingPoint(s, lastX,
		    lastY);
	    if (!wallAt(s, roomWithLastBlock, lastX, lastY)) {
		for (int i = 0; i < blocks.size(); i++) {
		    ObjectInstance block = blocks.get(i);
		    int bx = block.getDiscValForAttribute(ATTX);
		    int by = block.getDiscValForAttribute(ATTY);
		    bx += xdelta;
		    by += ydelta;

		    block.setValue(ATTX, bx);
		    block.setValue(ATTY, by);
		}

		permissibleMove = true;
	    }

	    /*
	     * if (pushedBlock != null) { int bx =
	     * pushedBlock.getDiscValForAttribute(ATTX); int by =
	     * pushedBlock.getDiscValForAttribute(ATTY);
	     * 
	     * int nbx = bx + xdelta; int nby = by + ydelta;
	     * 
	     * if (!wallAt(s, roomContaining, nbx, nby) && blockAtPoint(s, nbx,
	     * nby) == null) { permissibleMove = true;
	     * 
	     * // move the block pushedBlock.setValue(ATTX, nbx);
	     * pushedBlock.setValue(ATTY, nby);
	     * 
	     * }
	     * 
	     * } else if (!wallAt(s, roomContaining, nx, ny)) { permissibleMove
	     * = true; }
	     */

	    if (permissibleMove) {
		agent.setValue(ATTX, nx);
		agent.setValue(ATTY, ny);
	    }

	    if (Sokoban2Domain.this.includeDirectionAttribute) {
		if (this.xdelta == 1) {
		    agent.setValue(ATTDIR, "east");
		} else if (this.xdelta == -1) {
		    agent.setValue(ATTDIR, "west");
		} else if (this.ydelta == 1) {
		    agent.setValue(ATTDIR, "north");
		} else if (this.ydelta == -1) {
		    agent.setValue(ATTDIR, "south");
		}
	    }

	    return s;
	}

    }

    public class PFGoal extends PropositionalFunction {
	public PFGoal(String name, Domain domain, String[] parameterClasses) {
	    super(name, domain, parameterClasses);
	}

	@Override
	public boolean isTrue(State st, String[] params) {
	    List<ObjectInstance> blocks = st.getObjectsOfTrueClass(CLASSBLOCK);
	    List<ObjectInstance> rooms = st.getObjectsOfTrueClass(CLASSROOM);

	    for (int i = 0; i < blocks.size(); i++) {
		int bx = blocks.get(i).getDiscValForAttribute(ATTX);
		int by = blocks.get(i).getDiscValForAttribute(ATTY);
		ObjectInstance room = roomContainingPoint(st, bx, by);
		String bc = blocks.get(i).getStringValForAttribute(ATTCOLOR);
		String rc = room.getStringValForAttribute(ATTCOLOR);
		if (bc.equals(rc)) {
		    continue;
		}

		int wallsTouched = 0;
		if (wallCheck(st, room, bx + 1, by))
		    wallsTouched++;
		if (wallCheck(st, room, bx - 1, by))
		    wallsTouched++;
		if (wallCheck(st, room, bx, by + 1))
		    wallsTouched++;
		if (wallCheck(st, room, bx, by - 1))
		    wallsTouched++;

		if (wallsTouched >= 2
			&& doorContainingPoint(st, bx, by) == null) {
		    // return true;
		}
	    }

	    int blockCount = 0;
	    for (int i = 0; i < blocks.size(); i++) {
		int bx = blocks.get(i).getDiscValForAttribute(ATTX);
		int by = blocks.get(i).getDiscValForAttribute(ATTY);
		String bc = blocks.get(i).getStringValForAttribute(ATTCOLOR);

		for (int j = 0; j < rooms.size(); j++) {
		    String rc = rooms.get(j).getStringValForAttribute(ATTCOLOR);
		    ObjectInstance door = doorContainingPoint(st, bx, by);
		    if (regionContainsPoint(rooms.get(j), bx, by)
			    && rc.equals(bc) && door == null) {
			blockCount++;
		    }
		}
	    }

	    if (blockCount == blocks.size()) {
		return true;
	    }

	    return false;
	}
    }

    public class PFInRegion extends PropositionalFunction {

	protected boolean falseIfInDoor;

	public PFInRegion(String name, Domain domain, String[] params,
		boolean falseIfInDoor) {
	    super(name, domain, params);
	    this.falseIfInDoor = falseIfInDoor;
	}

	@Override
	public boolean isTrue(State s, String[] params) {

	    ObjectInstance o = s.getObject(params[0]);
	    int x = o.getDiscValForAttribute(ATTX);
	    int y = o.getDiscValForAttribute(ATTY);

	    if (this.falseIfInDoor) {
		if (doorContainingPoint(s, x, y) != null) {
		    return false;
		}
	    }

	    ObjectInstance region = s.getObject(params[1]);
	    return regionContainsPoint(region, x, y);

	}

    }

    public class PFOnWall extends PropositionalFunction {

	public PFOnWall(String name, Domain domain, String[] params) {
	    super(name, domain, params);
	}

	@Override
	public boolean isTrue(State s, String[] params) {
	    ObjectInstance o = s.getObject(params[0]);
	    ObjectInstance r = s.getObject(params[1]);
	    int x = o.getDiscValForAttribute(ATTX);
	    int y = o.getDiscValForAttribute(ATTY);

	    return wallAt(s, r, x, y);
	}
    }

    public class PFIsColor extends PropositionalFunction {

	protected String colorName;

	public PFIsColor(String name, Domain domain, String[] params,
		String color) {
	    super(name, domain, params);
	    this.colorName = color;
	}

	@Override
	public boolean isTrue(State s, String[] params) {

	    ObjectInstance o = s.getObject(params[0]);
	    String col = o.getStringValForAttribute(ATTCOLOR);

	    return this.colorName.equals(col);

	}

    }

    public class PFIsShape extends PropositionalFunction {

	protected String shapeName;

	public PFIsShape(String name, Domain domain, String[] params,
		String shape) {
	    super(name, domain, params);
	    this.shapeName = shape;
	}

	@Override
	public boolean isTrue(State s, String[] params) {
	    ObjectInstance o = s.getObject(params[0]);
	    String shape = o.getStringValForAttribute(ATTSHAPE);

	    return this.shapeName.equals(shape);
	}

    }

    public class InRoomOfColorTF implements TerminalFunction {

	protected PropositionalFunction colorPF;
	protected PropositionalFunction PF;

	public InRoomOfColorTF(Domain domain, String colorName, String PF) {
	    this.colorPF = domain.getPropFunction(Sokoban2Domain
		    .PFRoomColorName(colorName));
	    this.PF = domain.getPropFunction(PF);
	}

	@Override
	public boolean isTerminal(State s) {
	    // find the room the agent is in
	    List<GroundedProp> inRoomGPs = s.getAllGroundedPropsFor(this.PF);

	    for (GroundedProp gp : inRoomGPs) {
		if (gp.isTrue(s)) {
		    // then this gp holds the room parameter (param index 1)
		    // where the agent is
		    // check if that room object satisfies our color prop
		    return this.colorPF.isTrue(s, gp.params[1]);
		}
	    }

	    return false;
	}

    }

    public class TaskDoneTF implements TerminalFunction {

	protected PropositionalFunction colorPF;
	protected PropositionalFunction agentInRoomPF;
	protected PropositionalFunction blockInRoomPF;
	protected PropositionalFunction blockOnWallPF;

	public TaskDoneTF(Domain domain, String colorName) {
	    this.colorPF = domain.getPropFunction(Sokoban2Domain
		    .PFRoomColorName(colorName));
	    this.agentInRoomPF = domain
		    .getPropFunction(Sokoban2Domain.PFAGENTINROOM);
	    this.blockOnWallPF = domain
		    .getPropFunction(Sokoban2Domain.PFBLOCKONWALL);
	    this.blockInRoomPF = domain
		    .getPropFunction(Sokoban2Domain.PFBLOCKINROOM);
	}

	@Override
	public boolean isTerminal(State s) {
	    // TODO Auto-generated method stub

	    List<GroundedProp> blockOnWallGP = s
		    .getAllGroundedPropsFor(this.blockOnWallPF);
	    List<GroundedProp> agentInRoomGP = s
		    .getAllGroundedPropsFor(this.agentInRoomPF);
	    List<GroundedProp> blockInRoomGP = s
		    .getAllGroundedPropsFor(this.blockInRoomPF);

	    boolean agent = false;
	    boolean blockWall = false;
	    boolean blockRoom = false;

	    for (GroundedProp gp : agentInRoomGP) {
		if (gp.isTrue(s)) {
		    agent = this.colorPF.isTrue(s, gp.params[1]);
		}
		// System.out.println(agent);
	    }

	    for (GroundedProp gp : blockOnWallGP) {
		System.out.println(gp.pf.getName());
		if (gp.isTrue(s)) {
		    blockWall = true;
		    break;
		}
		System.out.println("\t" + blockWall);
	    }

	    for (GroundedProp gp : blockInRoomGP) {
		if (gp.isTrue(s))
		    blockRoom = this.colorPF.isTrue(s, gp.params[1]);
		// System.out.println("\t\t" + blockRoom);
	    }

	    if (blockWall) // if block is on the wall, then stop
		return true;
	    else if (!blockWall && agent && blockRoom) // if block is not on the
		// wall, and the block
		// and the agent are in
		// the room, stop
		return true;
	    else
		// continue running, not done.
		return false;
	}

    }

    // Can double as Initiation Condition and Subgoal Terminal Condition
    public class InRoomStateCheck implements StateConditionTestIterable {

	protected InRoomOfColorTF roomTF;
	protected Domain domain;
	protected String CLASS;

	public InRoomStateCheck(Domain domain, String Color, String CLASS) {
	    this.roomTF = new InRoomOfColorTF(domain, Color, CLASS);
	    this.CLASS = CLASS;
	    this.domain = domain;
	}

	// Gonna Try Calling a TF to see if it works
	// Technically it's boolean so it should be fine.
	@Override
	public boolean satisfies(State s) {
	    return roomTF.isTerminal(s);
	}

	@Override
	public Iterator<State> iterator() {

	    return new Iterator<State>() {

		@Override
		public boolean hasNext() {
		    State s = new State();

		    // Chec
		    s.addObject(new ObjectInstance(
			    domain.getObjectClass(CLASS), CLASS + 0));
		    return roomTF.isTerminal(s);
		}

		@Override
		public State next() {
		    return null;
		}

		@Override
		public void remove() {
		}
	    };
	}

	@Override
	public void setStateContext(State s) {
	    // TODO Auto-generated method stub
	}

    }

    public class AtRoomStateCheck implements StateConditionTest {
	protected InRoomOfColorTF roomTF;

	public AtRoomStateCheck(Domain domain, String Color, String CLASS) {
	    roomTF = new InRoomOfColorTF(domain, Color, CLASS);
	}

	// Gonna Try Calling a TF to see if it works
	// Technically it's boolean so it should be fine.
	@Override
	public boolean satisfies(State s) {
	    return roomTF.isTerminal(s);
	}

    }

    public Option getRoomOption(String name, Domain domain, String startRoom,
	    String goalRoom, String CLASS) {
	StateConditionTest start = new InRoomStateCheck(domain, startRoom,
		CLASS);
	StateConditionTest end = new InRoomStateCheck(domain, goalRoom, CLASS);

	DiscreteStateHashFactory hashFactory = new DiscreteStateHashFactory();
	hashFactory.setAttributesForClass(CLASS,
		domain.getObjectClass(CLASS).attributeList);

	OOMDPPlanner planner = new ValueIteration(domain, new LocalSubgoalRF(
		start, end), new LocalSubgoalTF(start, end), 0.99, hashFactory,
		0.001, 50);
	Policy p = new GreedyDeterministicQPolicy();

	return new SubgoalOption(name, p, start, end);
    }

}
