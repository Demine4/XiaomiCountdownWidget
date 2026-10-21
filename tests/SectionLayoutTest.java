import local.countdown.probe.SectionLayout;

public class SectionLayoutTest {
    private static int passed;
    private static void check(boolean ok,String name) { if(!ok) throw new AssertionError(name); passed++; }
    public static void main(String[] args) {
        SectionLayout standard=SectionLayout.plan(270,23,5,15,new int[]{44});
        check(standard.topRows==5 && standard.anniversaryRows==1,"five items plus complete anniversary");
        SectionLayout compact=SectionLayout.plan(165,23,5,15,new int[]{44});
        check(compact.topRows>0 && compact.anniversaryRows==1,"compact keeps anniversary visible");
        SectionLayout shortCard=SectionLayout.plan(110,23,5,15,new int[]{44});
        check(shortCard.topHidden && shortCard.anniversaryRows==1,"short card prioritizes full-width details");
        SectionLayout anniversaryOnly=SectionLayout.plan(270,23,0,15,new int[]{44,44,44});
        check(anniversaryOnly.topHidden && anniversaryOnly.anniversaryRows==3,"no empty top section");
        SectionLayout capped=SectionLayout.plan(900,23,20,3,new int[]{44,44,44,44});
        check(capped.topRows==3 && capped.anniversaryRows==3,"per-section maximum preserved");
        boolean bounded=true;
        int[] heights={44,60,44,72,44,44};
        for(int height=100;height<=700;height+=10) for(int row:new int[]{23,30,38}) {
            SectionLayout p=SectionLayout.plan(height,row,15,15,heights);
            int used=18+23+(p.topRows>0?31+p.topRows*(row+1):0);
            for(int i=0;i<p.anniversaryRows;i++) used+=heights[i];
            bounded&=used<=height && p.topRows<=15 && p.anniversaryRows<=heights.length;
        }
        check(bounded,"all planned rows fit across heights and font sizes");
        System.out.println("PASS: "+passed+" section layout checks (including 183 size/font combinations)");
    }
}
