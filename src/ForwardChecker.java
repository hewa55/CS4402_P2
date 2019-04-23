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

        int var = selectVar(domains);
        int val = selectVal(domains.get(var));
        leftBranch(binaryCSP,domains,var,val);
        rightBranch(binaryCSP,domains,var,val);
    }

    private List<SortedSet<Integer>> cloneDomain(List<SortedSet<Integer>> domains){
        List<SortedSet<Integer>> copy = new ArrayList<>();
        for (int i = 0; i < domains.size(); i++) {
            copy.add(i, new TreeSet<>());
            Iterator<Integer> iter = domains.get(i).iterator();
            while (iter.hasNext()){
                copy.get(i).add(iter.next());
            }
        }
        return copy;
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
        assignedVars.remove(var);
    }

    private void leftBranch(BinaryCSP binaryCSP,List<SortedSet<Integer>> domains, int var, int val){
        // takes varList, var, val

        assign(var,val);
        List<SortedSet<Integer>> domainsClone = cloneDomain(domains);
        SortedSet<Integer> remainingVal = new TreeSet<>();
        remainingVal.add(val);
        domains.get(var).retainAll(remainingVal);
        if(reviseFutureArcs2(binaryCSP,domains,var)){
            checkForwardRecursion(binaryCSP,domains);
        }
        //domains = cloneDomain(domainsClone);
        for (int i = 0; i <domains.size() ; i++) {
            domains.set(i, domainsClone.get(i));
        }
        unassign(var);
    }

    private void rightBranch(BinaryCSP binaryCSP,List<SortedSet<Integer>> domains, int var, int val){
        // takes varList, var, val

        //  deleteValue(var,val) from the domain of var
        domains.get(var).remove(val);

        if(!domains.get(var).isEmpty()){
            List<SortedSet<Integer>> domainsClone = cloneDomain(domains);
            // revise the future arcs based on the fact that the domain of var is now smaller
            if(reviseFutureArcs2(binaryCSP,domains,var)){
                checkForwardRecursion(binaryCSP,domains);
            }
            domains = cloneDomain(domainsClone);
        }
        domains.get(var).add(val);

        //      if reviseFutureArcs(varList, var):
        //          checkForward(varList - var)
        //      undoPruning

        // restoreValue(var,val) -- WHY? reasoning: when the right branch fails, the whole thing fails
    }

    private List<SortedSet<Integer>> reviseFutureArcs(BinaryCSP binaryCSP, List<SortedSet<Integer>> domains, int assignedVar, int assignedVal){

        // foreach futureVar in VarList where futureVar != var
        for (int i = 0; i < domains.size() ; i++) {
            if(assignedVars.contains(i)||i == assignedVar){
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

    private boolean reviseFutureArcs2(BinaryCSP binaryCSP, List<SortedSet<Integer>> domains, int assignedVar){
        boolean consistent = true;
        for (int i = 0; i < domains.size(); i++) {
            if(i==assignedVar || assignedVars.contains(i)){
                continue;
            }
            consistent = revise2(binaryCSP.getConstraints(),domains,i,assignedVar);
            if(!consistent){
                return false;
            }
        }
        return true;
    }

    private boolean revise2(List<BinaryConstraint> binaryConstraints, List<SortedSet<Integer>> domains, int futureVar, int assignedVar){
        SortedSet<Integer> revisedDomain = new TreeSet<>();
        for (int j = 0; j < binaryConstraints.size() ; j++) {
            if(binaryConstraints.get(j).appliesToVars(assignedVar,futureVar)){
                revisedDomain.addAll(binaryConstraints.get(j).obtainMirrorDomain(assignedVar,domains.get(assignedVar)));
                break;
            }
        }
        revisedDomain.retainAll(domains.get(futureVar));
        if(revisedDomain.isEmpty()){
            return false;
        }
        domains.set(futureVar,revisedDomain);
        return true;
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
