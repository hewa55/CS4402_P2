import java.util.*;

public class ForwardChecker {
    private List<Assignment> assignments = new ArrayList<>();
    private Set<Integer> assignedVars = new HashSet<>();
    private  int counter = 0;


    public List<Assignment> getAssignments() {
        return assignments;
    }



    public void checkForward(BinaryCSP binaryCSP){
        // generate the domains for each variable,
        // var 0 corresponds to List.get(0) etc
        List<SortedSet<Integer>> domains = generateDomains(binaryCSP);

        checkForwardRecursion(binaryCSP,domains);
    }


    public void checkForwardRecursion(BinaryCSP binaryCSP, List<SortedSet<Integer>> domains){
        counter++;
        System.out.println(counter);
        if(binaryCSP.getNoVariables()==assignedVars.size()){
            for (Assignment asg:assignments) {
                System.out.println(asg.getVar() +"="+asg.getVal());
            }
            // exit system once one solution was found
            System.exit(0);
        }

        int var = selectVar(domains);
        int val = selectVal(domains.get(var));
        leftBranch(binaryCSP,domains,var,val);
        rightBranch(binaryCSP,domains,var,val);
    }

    private List<SortedSet<Integer>> cloneDomain(List<SortedSet<Integer>> domains){
        // create a deep copy for the domain for roll backs
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
        // generate the domains
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
        //assign the value
        assignments.add(new Assignment(var,val));
        assignedVars.add(var);
    }
    private void unassign(int var){
        // remove the value from the assignment
        assignments.remove(assignments.size()-1);
        assignedVars.remove(var);
    }

    private void leftBranch(BinaryCSP binaryCSP,List<SortedSet<Integer>> domains, int var, int val){
        // left branch of the 2 way tree

        assign(var,val);
        // clone domains for roll back
        List<SortedSet<Integer>> domainsClone = cloneDomain(domains);

        // only one value stays in the set of the current assignment
        SortedSet<Integer> remainingVal = new TreeSet<>();
        remainingVal.add(val);
        // remove the other
        domains.get(var).retainAll(remainingVal);

        // if there are no issues with empty domains, continue to the next level
        if(reviseFutureArcs2(binaryCSP,domains,var)){
            checkForwardRecursion(binaryCSP,domains);
        }
        //and put the content of the clone back into the domain if it didnt work out
        for (int i = 0; i <domains.size() ; i++) {
            domains.set(i, domainsClone.get(i));
        }
        // remove assignment
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

    }

    private boolean reviseFutureArcs2(BinaryCSP binaryCSP, List<SortedSet<Integer>> domains, int assignedVar){
        boolean consistent = true;
        // go through all possible vars
        for (int i = 0; i < domains.size(); i++) {
            // skip the one already assigned or the recently (failed) assignemnt
            if(i==assignedVar || assignedVars.contains(i)){
                continue;
            }

            // check consistency and return respective value
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
                //obtain the new domain for every value
                revisedDomain.addAll(binaryConstraints.get(j).obtainMirrorDomain(assignedVar,domains.get(assignedVar)));
                break;
            }
        }
        // only keep the intersection of the previous domain with the revised domain
        revisedDomain.retainAll(domains.get(futureVar));
        if(revisedDomain.isEmpty()){
            return false;
        }
        domains.set(futureVar,revisedDomain);
        return true;
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
