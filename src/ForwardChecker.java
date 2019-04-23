import java.util.*;

public class ForwardChecker {
    private List<Assignment> assignments = new ArrayList<>();
    private Set<Integer> assignedVars = new HashSet<>();

    public List<Assignment> getAssignments() {
        return assignments;
    }



    public void checkForward(BinaryCSP binaryCSP){
        if(binaryCSP.getNoVariables()==0){
            //      print solution --> return
            // TODO print solution
            return;
        }

        List<SortedSet<Integer>> domains = generateDomains(binaryCSP);

        checkForwardRecursion(binaryCSP,domains);
    }

    // IDEA make create a map of a map. The first map points takes the variable names in the arc as key and returns the
    // the second map, in which the domain
    // TODO figure out how the arc situation can be improved, the current solution is shit.



    public void checkForwardRecursion(BinaryCSP binaryCSP, List<SortedSet<Integer>> domains){
        if(binaryCSP.getNoVariables()==assignedVars.size()){
            //      print solution --> return
            // TODO print solution
            for (Assignment asg:assignments) {
                System.out.println(asg.getVar() +"="+asg.getVal());
            }
            return;
        }
        for (Assignment asg:assignments) {
            System.out.println(asg.getVar() +"="+asg.getVal());
        }
        int var = selectVar(domains);
        int val = selectVal(domains.get(var));
        leftBranch(binaryCSP,domains,var,val);
        rightBranch(binaryCSP,domains,var,val);

    }

    private List<SortedSet<Integer>> generateDomains(BinaryCSP binaryCSP){
        List<SortedSet<Integer>> domains = new ArrayList<>();

        for (int i = 0; i < binaryCSP.getNoVariables(); i++) {
            SortedSet<Integer> set = new TreeSet<>();
            int lowerBound = binaryCSP.getLB(i);
            int upperBound = binaryCSP.getUB(i);
            for (int j = lowerBound; j <= upperBound; j++) {
                set.add(j);
            }
            domains.add(set);
        }
        return domains;
    }

    private void assign(int var, int val){
        assignments.add(new Assignment(var,val));
        assignedVars.add(var);
    }
    private void unassign(int var){
        assignments.remove(assignments.size()-1);
        assignedVars.add(var);
    }

    private void leftBranch(BinaryCSP binaryCSP,List<SortedSet<Integer>> domains, int var, int val){
        // takes varList, var, val

        assign(var,val);
        List<SortedSet<Integer>> revisedDomains =reviseFutureArcs(binaryCSP,domains,var, val);
        if(!revisedDomains.isEmpty()){
            checkForwardRecursion(binaryCSP,revisedDomains);
        }
        //if(reviseFutureArcs(varList, var)){
            //      checkForward(varList - var)
        //}

        // undoPruning -- not required as the pruning is only persisted if the revision of all future arcs is successful

        unassign(var);
    }

    private void rightBranch(BinaryCSP binaryCSP,List<SortedSet<Integer>> domains, int var, int val){
        // takes varList, var, val

        //  deleteValue(var,val) from the domain of var
        domains.get(var).remove(val);

        if(!domains.get(var).isEmpty()){
            // revise the future arcs based on the fact that the domain of var is now smaller
            List<SortedSet<Integer>> revisedDomains = new ArrayList<>();
            for (int i = 0; i <domains.size() ; i++) {
                revisedDomains.add(i,new TreeSet<>());
            }
            for (Integer possibleVal: domains.get(var)) {
                List<SortedSet<Integer>> tmpRevisedDomains =reviseFutureArcs(binaryCSP,domains,var, possibleVal);
                for (int i = 0; i < tmpRevisedDomains.size() ; i++) {
                    revisedDomains.get(i).addAll(tmpRevisedDomains.get(i));
                }
            }
            boolean notempty = true;
            for (SortedSet<Integer> revisedDomain: revisedDomains) {
                notempty = notempty && (!revisedDomain.isEmpty());
            }
            if(notempty){
                checkForwardRecursion(binaryCSP,revisedDomains);
            }
        }

        //      if reviseFutureArcs(varList, var):
        //          checkForward(varList - var)
        //      undoPruning

        // restoreValue(var,val) -- WHY? reasoning: when the right branch fails, the whole thing fails
    }

    private List<SortedSet<Integer>> reviseFutureArcs(BinaryCSP binaryCSP, List<SortedSet<Integer>> domains, int assignedVar, int assignedVal){

        // foreach futureVar in VarList where futureVar != var
        for (int i = 0; i < domains.size() ; i++) {
            if(assignedVars.contains(i)){
                continue;
            }
            SortedSet<Integer> domain = revise(domains.get(i),binaryCSP.getConstraints(),i, assignedVar,assignedVal);
            //      // Prunes Domain(D(futureVar))
            if(domain.size()==0){
                // domain of the future var has been pruned to 0
                // last assignment wasn't good
                return new ArrayList<>();
            }
            domains.set(i,domain);
        }
        // only return the domains if all domains still have size > 0
        return domains;
    }

    private SortedSet<Integer> revise(SortedSet<Integer> previousDomain, List<BinaryConstraint> binaryConstraints, int futureVar, int assignedVar, int assignedVal){
        // obtain the current assignment from the assignment list
        // go through the binary constraints and check all the allowed combinations of the domain with
        // the assignment
        //int assignedVar = assignments.get(assignments.size()-1).getVar();
        //int assignedVal = assignments.get(assignments.size()-1).getVal();
        SortedSet<Integer> revisedDomain = new TreeSet<>();
        for (int j = 0; j < binaryConstraints.size() ; j++) {
            // TODO should not have to iterate through all, should have a hashmap pointing to the right entry
            if(binaryConstraints.get(j).appliesToVars(assignedVar,futureVar)){
                Iterator<Integer> domainIterator = previousDomain.iterator();
                while(domainIterator.hasNext()){
                    Integer futureAssingment = domainIterator.next();
                    if(binaryConstraints.get(j).combinationAllowed(futureVar,futureAssingment,assignedVar,assignedVal)){
                        revisedDomain.add(futureAssingment);
                    }
                }
            }
        }
        return revisedDomain;
    }


    private int selectVar(List<SortedSet<Integer>> domains){
        // function selects the variable with the smallest domain

        // set size to largest possible value so domain sizes are almost guranteed to be below
        // Caveat: for incredibly large domains, this doesn't apply
        int size = Integer.MAX_VALUE;
        // initialised to some value --> will definitely change, because domains.size() != 0
        int var = 0;
        for (int i = 0; i < domains.size() ; i++) {
            if(domains.get(i).size()<size && (!assignedVars.contains(i))){
                // the domain is the smallest we have seen so far
                var = i;
                size = domains.get(i).size();
            }
        }
        // return the variable with the smallest domain
        return var;
    }

    private int selectVal(SortedSet<Integer> domain){
        // function selects the value with the smallest value
        // the set is sorted, so just select the first element
        return domain.first();
    }
}
