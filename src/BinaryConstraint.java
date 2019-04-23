import java.util.* ;

public final class BinaryConstraint {
  private int firstVar, secondVar ;
  private ArrayList<BinaryTuple> tuples ;

  public BinaryConstraint(int fv, int sv, ArrayList<BinaryTuple> t) {
    firstVar = fv ;
    secondVar = sv ;
    tuples = t ;
  }


  public String toString() {
    StringBuffer result = new StringBuffer() ;
    result.append("c("+firstVar+", "+secondVar+")\n") ;
    for (BinaryTuple bt : tuples)
      result.append(bt+"\n") ;
      return result.toString() ;
    }
  
  // SUGGESTION: You will want to add methods here to reason about the constraint
    public boolean combinationAllowed(int futureVar, int futureVarAssignment, int var, int varAssignment){

        int firstVal;
        int secondVal;
      if(firstVar ==futureVar){
            firstVal =  futureVarAssignment;
            secondVal = varAssignment;
        } else {
            firstVal =  varAssignment;
            secondVal = futureVarAssignment;
        }
        for (BinaryTuple tuple: tuples) {
            if(tuple.matches(firstVal,secondVal)){
                return true;
            }
        }
        return false;
    }

    public Set<Integer> obtainMirrorDomain(int domainVar, Set<Integer> domain){

        Set<Integer> returnSet = new TreeSet<>();
        if(firstVar ==domainVar){
            for (BinaryTuple tuple: tuples) {
                if(domain.contains(tuple.getVal1())){
                    returnSet.add(tuple.getVal2());
                }
            }
        } else {
            for (BinaryTuple tuple: tuples) {
                if(domain.contains(tuple.getVal2())){
                    returnSet.add(tuple.getVal1());
                }
            }
        }

        return returnSet;
    }

    public boolean appliesToVars(int var, int futureVar){
      return (firstVar == var && secondVar==futureVar || firstVar==futureVar&&secondVar==var);
    }
}
