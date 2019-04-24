import java.util.ArrayList;
import java.util.List;

public class QueueCheck {
    private List<Arc> list = new ArrayList<>();

    public void add(Arc arc){
        for (int i = 0; i < list.size(); i++) {
            if(arc.firstvar==list.get(i).firstvar && arc.secondvar == list.get(i).secondvar){
                return;
            }
        }
        list.add(arc);
    }
    public Arc remove(){
        Arc temp = this.list.get(0);
        this.list.remove(0);
        return temp;
    }

    public boolean isEmpty(){
        return list.size()==0;
    }
}
