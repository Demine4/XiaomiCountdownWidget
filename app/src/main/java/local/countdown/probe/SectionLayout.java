package local.countdown.probe;

/** Allocate real content height between two compact columns and a full-width anniversary section. */
public final class SectionLayout {
    public final int topRows, anniversaryRows;
    public final boolean topHidden;
    private SectionLayout(int top, int anniv) { topRows = top; anniversaryRows = anniv; topHidden = top == 0; }
    public static SectionLayout plan(int height, int rowHeight, int topCount, int maxRows, int[] anniversaries) {
        int cap = Math.max(1, Math.min(15, maxRows));
        int limit = Math.min(cap, anniversaries.length);
        int available = Math.max(0, height - 18 - 23);
        if (limit == 0) return new SectionLayout(Math.max(1, Math.min(cap, Math.min(topCount, (height - 41) / (rowHeight+1)))), 0);
        int annivTotal = 0;
        for (int i=0; i<limit; i++) annivTotal += anniversaries[i];
        int desiredTop = Math.min(cap, topCount);
        int topRows = 0;
        if (desiredTop > 0 && available - 31 >= rowHeight + 1 + anniversaries[0]) {
            int budget = available - 31;
            if (desiredTop * (rowHeight + 1) + annivTotal <= budget) topRows = desiredTop;
            else topRows = Math.min(desiredTop, Math.max(1, Math.min(budget - anniversaries[0], budget / 2) / (rowHeight+1)));
            available = budget - topRows * (rowHeight+1);
        }
        int shown = 0, used = 0;
        while (shown < limit && used + anniversaries[shown] <= available) used += anniversaries[shown++];
        return new SectionLayout(topRows, shown);
    }
}
