import java.util.*;

public class MAC {
    private QueueCheck arcQueue = new QueueCheck();
    private int counter = 0;
    // serves as a list to check which nodes any node is connected to
    // this is created once in the beginning but not changed afterwards
    private List<Set<Integer>> lookUpList;
    private List<Assignment> assignments = new ArrayList<>();
    private Set<Integer> assignedVars = new HashSet<>();

    public List<Assignment> getAssignments() {
        return assignments;
    }



    public void checkMac(BinaryCSP binaryCSP){
        // generate the domains for each variable,
        // var 0 corresponds to List.get(0) etc
        List<SortedSet<Integer>> domains = generateDomains(binaryCSP);
        generateLookupList(binaryCSP);
        generateArcQueue();
        checkMacRecursion(binaryCSP,domains);
    }

    private void generateArcQueue(){
        for (int i = 0; i < lookUpList.size() ; i++) {
            for (Integer secondVar: lookUpList.get(i)) {
                arcQueue.add(new Arc(i, secondVar));
            }
        }

    }

    private void checkMacRecursion(BinaryCSP binaryCSP, List<SortedSet<Integer>> domains){
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
        assign(var,val);
        List<SortedSet<Integer>> domainsClone = cloneDomain(domains);
        // only one value stays in the set of the current assignment
        SortedSet<Integer> remainingVal = new TreeSet<>();
        remainingVal.add(val);
        // remove the other
        domains.get(var).retainAll(remainingVal);

        if(AC3(binaryCSP,domains)){
            checkMacRecursion(binaryCSP,domains);

        }
        for (int i = 0; i <domains.size() ; i++) {
            domains.set(i, domainsClone.get(i));
        }
        // remove assignment
        unassign(var);

        domains.get(var).remove(val);
        if(!domains.get(var).isEmpty()){
            domainsClone = cloneDomain(domains);
            // revise the future arcs based on the fact that the domain of var is now smaller
            if(AC3(binaryCSP,domains)){
                checkMacRecursion(binaryCSP,domains);
            }
            domains = cloneDomain(domainsClone);
        }
        domains.get(var).add(val);
    }

    private boolean AC3(BinaryCSP binaryCSP, List<SortedSet<Integer>> domains){
        generateArcQueue();
        QueueCheck workingQueue = this.arcQueue;
        while(!workingQueue.isEmpty()){
            Arc currentArc = workingQueue.remove();
            switch (revise(binaryCSP, currentArc,domains)){
                case 1:
                    for (Integer pair:lookUpList.get(currentArc.firstvar)) {
                        workingQueue.add(new Arc(pair,currentArc.firstvar));
                    } break;
                case 0:
                    break;
                case -1: return false;
            }
        }
        for (SortedSet<Integer> domain: domains) {
            if(domain.isEmpty()){
                return false;
            }

        }
        return true;
    }


    private int revise(BinaryCSP binaryCSP, Arc currentArc,List<SortedSet<Integer>> domains){
        //SortedSet<Integer> domain_i = domains.get(currentArc.firstvar);
        //SortedSet<Integer> domain_j = domains.get(currentArc.secondvar);

        boolean changed = true;
        SortedSet<Integer> valuesToKeep = new TreeSet<>();
        BinaryConstraint currentBinaryConstraint = new BinaryConstraint();
        for (BinaryConstraint binaryConstraint: binaryCSP.getConstraints()) {
            if(binaryConstraint.appliesToVars(currentArc.firstvar,currentArc.secondvar)) {
                currentBinaryConstraint = binaryConstraint;
            }
        }
        Iterator<Integer> domainIterator = domains.get(currentArc.firstvar).iterator();
        while (domainIterator.hasNext()) {
            Integer d_i = domainIterator.next();
            boolean supported = false;
            for (Integer d_j: domains.get(currentArc.secondvar)) {
                        // if combination allowed, there is support
                supported= supported||currentBinaryConstraint.combinationAllowed(currentArc.secondvar,d_j,currentArc.firstvar,d_i);
                //break;
            }
            if(supported){
                valuesToKeep.add(d_i);
                changed = false;
            }
        }
        domains.get(currentArc.firstvar).retainAll(valuesToKeep);
        if(domains.get(currentArc.firstvar).isEmpty()){
            // TODO exit early
            return -1;
        }
        if(changed){
            return 1;
        } else {
            return 0;
        }
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
    private void unassign(int var){
        // remove the value from the assignment
        assignments.remove(assignments.size()-1);
        assignedVars.remove(var);
    }


    private void assign(int var, int val){
        //assign the value
        assignments.add(new Assignment(var,val));
        assignedVars.add(var);
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

    private void generateLookupList(BinaryCSP binaryCSP){
        // simply creates an list filled wtih sets filled with zeros, to be able to access the points
        // of the list in any order
        this.lookUpList = generateEmptyList(binaryCSP.getNoVariables());

        List<BinaryConstraint> constraints = binaryCSP.getConstraints();
        for (int i = 0; i < constraints.size() ; i++) {
            this.lookUpList.get(constraints.get(i).getFirstVar()).add(constraints.get(i).getSecondVar());
            this.lookUpList.get(constraints.get(i).getSecondVar()).add(constraints.get(i).getFirstVar());

        }
        for (int i = 0; i < binaryCSP.getNoVariables() ; i++) {
            this.lookUpList.get(i).remove(-1);
        }
    }
    private List<Set<Integer>> generateEmptyList(int size){
        List<Set<Integer>> temp = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Set<Integer> tempSet = new HashSet<>();
            tempSet.add(-1);
            temp.add(tempSet);
        }
        return temp;
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




}



