package oomdptb.behavior.vfa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oomdptb.oomdp.GroundedAction;
import oomdptb.oomdp.State;


/**
 * This class is used for general purpose linear VFA. It only needs to be provided a FeatureDatabase object that will be used to store
 * retrieve state features. For every feature returned by the feature database, this class will automatically create a weight associated with it.
 * The returned approximated value for any state is the linear combination of state features and weights.
 *  
 * @author James MacGlashan
 *
 */
public class LinearVFA implements ValueFunctionApproximation {

	
	protected FeatureDatabase						featureDatabase;
	protected Map<Integer, FunctionWeight>			weights;
	protected double								defaultWeight = 0.0;
	
	public LinearVFA(FeatureDatabase featureDatabase) {
		
		this.featureDatabase = featureDatabase;
		this.weights = new HashMap<Integer, FunctionWeight>();
		
	}
	
	public LinearVFA(FeatureDatabase featureDatabase, double defaultWeight) {
		
		this.featureDatabase = featureDatabase;
		this.defaultWeight = defaultWeight;
		this.weights = new HashMap<Integer, FunctionWeight>();
		
	}

	@Override
	public ApproximationResult getStateValue(State s) {
		
		List <StateFeature> features = featureDatabase.getStateFeatures(s);
		return this.getApproximationResultFrom(features);
	}

	@Override
	public List<ActionApproximationResult> getStateActionValues(State s, List<GroundedAction> gas) {
	
		List <ActionFeaturesQuery> featureSets = this.featureDatabase.getActionFeaturesSets(s, gas);
		List <ActionApproximationResult> results = new ArrayList<ActionApproximationResult>(featureSets.size());
		
		for(ActionFeaturesQuery afq : featureSets){
			
			ApproximationResult r = this.getApproximationResultFrom(afq.features);
			ActionApproximationResult aar = new ActionApproximationResult(afq.queryAction, r);
			results.add(aar);
			
		}
		
		return results;
	}

	@Override
	public WeightGradient getWeightGradient(ApproximationResult approximationResult) {
		
		WeightGradient gradient = new WeightGradient(approximationResult.stateFeatures.size());
		for(StateFeature sf : approximationResult.stateFeatures){
			gradient.put(sf.id, sf.value);
		}
		
		return gradient;
	}
	
	
	
	protected ApproximationResult getApproximationResultFrom(List <StateFeature> features){
		
		List <FunctionWeight> activedWeights = new ArrayList<FunctionWeight>(features.size());
		
		double predictedValue = 0.;
		for(StateFeature sf : features){
			FunctionWeight fw = this.weights.get(sf.id);
			if(fw == null){
				fw = new FunctionWeight(sf.id, defaultWeight);
				this.weights.put(fw.weightId, fw);
			}
			predictedValue += sf.value*fw.weightValue;
			activedWeights.add(fw);
		}
		
		ApproximationResult result = new ApproximationResult(predictedValue, features, activedWeights);
		
		return result;
		
	}

}